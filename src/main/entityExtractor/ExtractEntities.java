// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 10, 2010
 * 
 */
package entityExtractor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import wikipediaparser.CategoryParser;

import dao.Dao;
import entityExtractor.EntityUtil;

import model.Language;
import model.Page;
import model.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.MapMaker;


/** Extracts (and tags) entities. 
 * 
 * @author Jessica Anderson
 *
 */
public class ExtractEntities {

	//private Map<String,String> cachedPageTypes;
	private ConcurrentMap<String, String> cachedPageTypes;
	
	private Page currentPage;
	private Set<String> nlEntities;
	private Set<Page> nlArticles;
	private Set<Page> enArticles;
	
	private Map<String,Pair<Page,Page>> titlePageMap;
	private EntityUtil util;
	
	private static int startId;
	private int numberOfThreads;
	private int extractionCounter;
	
	private static Logger logger = Logger.getLogger(ExtractEntities.class);
	
	public static void main(String args[]) {
		Map<String,String> categoryMap = Maps.newHashMap();
		CategoryParser parser = new CategoryParser("C:\\eclipse\\workspace\\Scriptie\\categoryClasses.txt");
		categoryMap = parser.parse();

		EntityUtil util = new EntityUtil(new Dao(), categoryMap);

		ConcurrentMap<String,String> cachedPTypes = new MapMaker()
			.concurrencyLevel(1)
			.softKeys()
			.softValues()
			.initialCapacity(1000000)
			.makeMap();
			//Maps.newHashMapWithExpectedSize(1000000);
		ExtractEntities entity = new ExtractEntities(util,1500,1,cachedPTypes);
		entity.executeFirstPass(0);
		
	}
	
	/** Constructor.
	 * 
	 * @param util utility class
	 */
	public ExtractEntities(EntityUtil util, int startIdIn, int numberOfThreads, ConcurrentMap<String,String> cachedPageTypes) {
		this.util = util;
		titlePageMap = Maps.newHashMap();
		nlArticles = Sets.newHashSet();
		enArticles = Sets.newHashSet();
		this.cachedPageTypes = cachedPageTypes;
		this.numberOfThreads = numberOfThreads;
		PropertyConfigurator.configure("log4j.properties");

		//logger.setLevel(Level.ERROR);
		startId = startIdIn;
	}
	
	/** Gets the next id to look up (sets extractionCounter)
	 * 
	 * @param threadNr the thread's number
	 */
	private void iterateId(int threadNr) {
		while(++extractionCounter % numberOfThreads != threadNr) {} //for sharing between threads	
	}
	
	/** Runs the entity extractor, orchestrating all necessary components
	 * 
	 */
	public void executeFirstPass(int threadNr) {
		extractionCounter = startId;

		iterateId(threadNr);
		
		
		while(extractionCounter <= 329020) { //while still more Dutch articles
			
			currentPage = util.extractPageById(Language.DUTCH.abbreviation(), extractionCounter);
			
			if(currentPage == null || currentPage.getBody() == null || currentPage.getTitle() == null) {
				logger.info("ExtractEntities: extracted id unavailable:"+extractionCounter);
				iterateId(threadNr);
				continue;
			}
			
			logger.info("ExtractEntities: extracted id available:"+extractionCounter);

			if(currentPage.isDisambiguation()) {
				logger.info("ExtractEntities: page with id "+extractionCounter+" is disambiguation page. Skipping!");
				iterateId(threadNr);
				continue;
			}
			
			if(util.isRedirect(currentPage))
			{
				logger.info("ExtractEntities: page with id "+extractionCounter+" is redirect. Skipping!");
				iterateId(threadNr);
				continue;
			}
			
			/*TODO:
			 * find all entities in currentPage, save stripped of [[|]]
			 *compare EN and NL entities if applicable, 
			 *look for matches, save these in enEntitiesFromLink 
			
			if(currentPage.englishLink != null && !currentPage.englishLink.equals("")) {
				Page page = util.extractPageByTitle("EN", currentPage.englishLink);
				if(page != null) {
					enEntities = util.matchEntities(page);	// TODO: We do nothing with these entities?
					util.removeCommonNouns(enEntities);		// TODO: We do nothing with these entities?
				}
			}
			*/
			
			
			nlEntities = util.matchEntities(currentPage); 
			
			for(String s : nlEntities){ //for all Dutch entities, find original linked Dutch Article
				if(s != null && !cachedPageTypes.containsKey(s.toLowerCase())) {
					nlArticles.add(util.findArticle(s)); 
				}
				else if(s != null) {
					logger.debug("Hit!");
				}
			}
			
			/*for all Dutch articles, try to find English equivalent*/
			for(Page p : nlArticles) { 
				if(p == null) {
					continue;
				}
				if(util.isRedirect(p)) {
					p = util.followRedirect(p, "NL");
					
					if(p == null) {
						continue;
					}
				}
				
				if(!p.isDisambiguation()) { // Don't worry about disambiguation pages.
					Page enPage = util.findEnglishEquivalent(p);
					if(enPage != null) {
						/* We found an English article by following an "en:" link directly from the dutch article
						 or by retrieving an English article with precisely the same name. */
						enArticles.add(enPage);
						titlePageMap.put(p.getTitle().toLowerCase(), new Pair<Page,Page>(p,enPage));
					}
					else {
						enPage = util.findAlternateEnglishEquivalent(p);
						if(enPage != null) {
							/* We found an English article by fetching an article name from the
							 '''bolded''' words at the start of the article.
							OR from following a #REDIRECT directive. */
							enArticles.add(enPage);
							titlePageMap.put(p.getTitle().toLowerCase(), new Pair<Page,Page>(p,enPage));
						}
						else {
							// No English articles.
							titlePageMap.put(p.getTitle().toLowerCase(), new Pair<Page,Page>(p,null));
						}
					}	
				}
				else {
					// Disambiguation page? Then we don't need the English equivalent, just use dutch cats
					titlePageMap.put(p.getTitle(), new Pair<Page,Page>(p,null));
				}
			}

			util.removeCommonNounPages(enArticles);
			// Remove common nouns from the list of entities
			
			//for each English article, check if entityType already exists
			//otherwise, try to find entity type and put it in nlArticles too.
			
			for(Map.Entry<String, Pair<Page,Page>> entry: titlePageMap.entrySet()) {
				if(entry.getValue().getSecond() == null) {
					if(entry.getValue().getFirst() != null && entry.getValue().getFirst().isDisambiguation()) {
						entry.getValue().getFirst().setEntityType(util.findDisambiguationType(entry.getValue().getFirst(), 0, cachedPageTypes));
					}
					else if(entry.getValue().getFirst() != null){
						entry.getValue().getFirst().setEntityType(util.findDutchType(entry.getValue().getFirst(), cachedPageTypes));
					}
				}
				else if(entry.getValue().getSecond() != null && 
						entry.getValue().getFirst() != null && 
						(entry.getValue().getSecond().getEntityType() == null ||
						 entry.getValue().getSecond().getEntityType().equals(""))) {
					
					String entityType = util.findType(entry.getValue().getSecond(), 0, true, cachedPageTypes);
					entry.getValue().getFirst().setEntityType(entityType);
					entry.getValue().getSecond().setEntityType(entityType);
				}
				
				if( entry.getValue().getSecond() != null &&
					entry.getValue().getSecond().getEntityType() != null) {
				//	synchronized(cachedPageTypes) {
						cachedPageTypes.put(entry.getValue().getSecond().getTitle().toLowerCase(), entry.getValue().getSecond().getEntityType());
						logger.debug("Added # " + cachedPageTypes.size());
				//	}
				}
				if( entry.getValue().getFirst() != null &&
					entry.getValue().getFirst().getEntityType() != null) {
				//	synchronized(cachedPageTypes) {
						cachedPageTypes.put(entry.getValue().getFirst().getTitle().toLowerCase(), entry.getValue().getFirst().getEntityType());
						logger.debug("Added # " + cachedPageTypes.size());
				//	}
				}
			}
			
			
			util.insertTypesIntoText(currentPage, titlePageMap, cachedPageTypes); 
			
			util.matchRecurringEntities(currentPage, titlePageMap, cachedPageTypes); 
			util.save(currentPage, titlePageMap); 
			
			titlePageMap.clear();
			nlEntities.clear();
			nlArticles.clear();
			enArticles.clear();
			
			iterateId(threadNr);
		}
	}
	
	/** Looks for missed entities by using n-grams
	 * 
	 */
	public void executeSecondPass() {
		
		extractionCounter = startId;

		while(extractionCounter <= 329020) { //while still more Dutch articles

			currentPage = util.extractPageById(Language.DUTCH.abbreviation(), extractionCounter);

			//first check for more entities using nGrams
			Set<String> twoGramEntities = util.findEntitiesUsingNGrams(currentPage, 2);
			util.processNGrams(twoGramEntities, currentPage);
			
			Set<String> threeGramEntities = util.findEntitiesUsingNGrams(currentPage, 3);
			util.processNGrams(threeGramEntities, currentPage);
			
			Set<String> fourGramEntities = util.findEntitiesUsingNGrams(currentPage, 4);
			util.processNGrams(fourGramEntities, currentPage);
			
		}
	}
	
	

}
