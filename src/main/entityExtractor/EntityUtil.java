// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 14, 2010
 * 
 */
package entityExtractor;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.*;

import com.google.common.collect.Sets;
import model.Page;
import model.Pair;
import dao.Dao;
import dao.IDao;

/** Utility for manipulating entities
 * 
 * @author Jessica Anderson
 *
 */
public class EntityUtil {

	public static final String CHARSET = "UTF-8";
	
	private IDao dao;
	private static Logger logger = Logger.getLogger(EntityUtil.class);
	private static int maxDepthLimit = 3;
	private Pattern entityPattern;
	private Pattern simpleEntityPattern;
	private Pattern redirectPattern;
	private Matcher entityMatcher;
	private Matcher redirectMatcher;
	private int countPER;
	private int countORG;
	private int countLOC;
	private int countMISC;
	private int depthLimit;
	private Map<String,String> categoryMap;
	private static int typeThreshold = 3;
	
	/** Constructor
	 * 
	 * @param dao Dao instantiation
	 * @param categoryMap map of categories with their type
	 */
	public EntityUtil(Dao dao, Map<String,String> categoryMap) {
		this.dao = dao;
		entityPattern = Pattern.compile("(?i)\\[\\[([-/\\(\\)a-z0-9«¸È‚‰‡ÂÁÍÎËÔÓÏƒ≈…Ê∆ÙˆÚ˚˘ˇ÷‹¯·ÌÛ˙Ò—_\\s]+?)(\\|[-/\\(\\)a-z0-9«¸È‚‰‡ÂÁÍÎËÔÓÏƒ≈…Ê∆ÙˆÚ˚˘ˇ÷‹¯·ÌÛ˙Ò—_\\s]+?)?\\]\\]");
		simpleEntityPattern = Pattern.compile("(?i)\\[\\[([-/\\(\\)a-z0-9«¸È‚‰‡ÂÁÍÎËÔÓÏƒ≈…Ê∆ÙˆÚ˚˘ˇ÷‹¯·ÌÛ˙Ò—_\\s]+?)\\]\\]");
		redirectPattern = Pattern.compile("#(REDIRECT|DOORVERWIJZING)\\s?\\[\\[([-\\(\\)a-zA-Z0-9«¸È‚‰‡ÂÁÍÎËÔÓÏƒ≈…Ê∆ÙˆÚ˚˘ˇ÷‹¯·ÌÛ˙Ò—_\\s]+?)\\]\\]");
		depthLimit = 1;
		this.categoryMap = categoryMap;
		countPER = 0;
		countORG = 0;
		countLOC = 0;
		countMISC= 0;
	}
	

	/** Removes all common nouns from a set of words
	 * 
	 * @param entities the words to check
	 */
	public void removeCommonNouns(Set<String> entities) {
		assert(entities != null);
		
		for( String s : entities) {
			s.trim();
			if(dao.selectNoun(s)) {
				entities.remove(s);
			}
		}
	}
	
	/** Removes all pages that have a title that is a common noun
	 * 
	 * @param Pages the pages to check
	 */
	public void removeCommonNounPages(Set<Page> Pages) {
		//
		for( Page p : Pages) {
			if(dao.selectNoun(p.getTitle())) {
				Pages.remove(p);
			}
		}
	}

	/** Finds a page from data source based on page id
	 * 
	 * @param langAbbrev the page's language, abbreviated (EN or NL)
	 * @param id page id
	 * @return the page (with given id)
	 */
	public Page extractPageById(String langAbbrev, long id) {
		Page currentPage = new Page();
		ResultSet resultsPage = dao.retrievePage(langAbbrev, id);
		ResultSet resultsCats = dao.retrieveCategoriesPerPage(langAbbrev, id);
		assert(resultsPage != null);
		assert(resultsCats != null);
		try {
			//first Page info
			if(resultsPage.first()){
				currentPage.setId(resultsPage.getLong(1));
				currentPage.setTitle(resultsPage.getString(2));
				InputStream bodyAsStream = resultsPage.getBinaryStream(3);
				currentPage.setBody(convertStreamToString(bodyAsStream));
				currentPage.setEnglishLink(resultsPage.getString(4));
				currentPage.setEntityType(resultsPage.getString(5));
				currentPage.setDisambiguation(resultsPage.getBoolean(6));
				
				//next Cats info
				boolean moreRows = resultsCats.first(); //positions to the first line
				while(moreRows) {
					currentPage.getCategories().add(resultsCats.getString(2));
					moreRows = resultsCats.next();
				}
			}
			else {
				logger.warn("No results for id " +id+ " in language '"+langAbbrev+"'.");
			}
		} catch (SQLException e) {
			logger.warn("Could not successfully grab results", e);
			e.printStackTrace();
		} catch (Exception n) {
			logger.warn("Could not convert InputStream to String", n);
			n.printStackTrace();
		}
		
		return currentPage;
	}
	
	/** Finds a page from data source based on page title
	 * 
	 * @param langAbbrev the page's language, abbreviated (EN or NL)
	 * @param title page title
	 * @return the page (with given title)
	 */
	public Page extractPageByTitle(String langAbbrev, String title) {
		Page page = new Page();
		ResultSet resultsPage = dao.retrievePage(langAbbrev, title);
		assert(resultsPage != null);
		
		try {
			//first Page info
			if(resultsPage.first()){
				page.setId(resultsPage.getLong(1));
				page.setTitle(resultsPage.getString(2));
				InputStream bodyAsStream = resultsPage.getBinaryStream(3);
				page.setBody(convertStreamToString(bodyAsStream));
				page.setEnglishLink(resultsPage.getString(4));
				page.setEntityType(resultsPage.getString(5));
				page.setDisambiguation(resultsPage.getBoolean(6));
			}
			else {
				//logger.warn("No results for title " + title + " in language '"+langAbbrev+"'.");
			}
		} catch (SQLException e) {
			logger.warn("Could not successfully grab results", e);
			e.printStackTrace();
		} catch (Exception n) {
			logger.warn("Could not convert InputStream to String", n);
			n.printStackTrace();
		}
		
		assert(page.getId() > 0);
		ResultSet resultsCats = dao.retrieveCategoriesPerPage(langAbbrev, page.getId());
		//assert(resultsCats != null);
		if (resultsCats == null) {
			return null;
		}
		try {
			//next Cats info
			boolean moreRows = resultsCats.first(); //positions to the first line
			while(moreRows) {
				page.getCategories().add(resultsCats.getString(2));
				moreRows = resultsCats.next();
			}
			
		} catch(SQLException e) {
			logger.warn("Could not successfully grab results", e);
			e.printStackTrace();
		}
		if(page.getId() == 0) {
			return null;
		}
		else {
			return page;
		}
	}
	
	/** Finds (matches) entities in a given page text
	 * 
	 * @param p the page whose body to search
	 * @return a set of Strings, namely the entities that were found
	 */
	public Set<String> matchEntities(Page p) {
		//look for entities in page.body and save them in a set/list.
		assert(p != null);
		
		Set<String> entitySet = Sets.newLinkedHashSet();
		
		entityMatcher = entityPattern.matcher(p.getBody());	
		
		while(entityMatcher.find()) {
			entitySet.add(entityMatcher.group(1));			
		}
		
		return entitySet;	
	}
	
	/** Finds a Dutch article based on title
	 * 
	 * @param entity the name (title) of the entity
	 * @return Page the page with the given title
	 */
	public Page findArticle(String entity) {
		
		return extractPageByTitle("NL", entity);
	}
	
	/** Finds an English page equivalent to a given Dutch page
	 * NB: useswikipedia provided inter-wiki link.
	 * 
	 * @param page the Dutch page
	 * @return an English page
	 */
	public Page findEnglishEquivalent(Page page) {
		///two step approach: first look for EN link, otherwise search EN pages using title.
		
		assert(page != null);
		
		Page englishPage = null;
		if(page.getEnglishLink() != null && page.getEnglishLink().length() > 0) {
			englishPage = extractPageByTitle("EN", page.getEnglishLink());
			if(englishPage != null && isRedirect(englishPage)) {
				englishPage = followRedirect(englishPage,"EN");
			}
		}
		else {
			englishPage = extractPageByTitle("EN", page.getTitle());
			if(englishPage != null && isRedirect(englishPage)) {
				englishPage = followRedirect(englishPage,"EN");
			}
		}
		return englishPage;
	}
	
	/** Attempts to find an English page equivalent for a Dutch page
	 * NB: uses alternative titles for a Dutch page for searching
	 * 
	 * @param page the Dutch page
	 * @return an English page
	 */
	public Page findAlternateEnglishEquivalent(Page page) {
		
		redirectMatcher = redirectPattern.matcher(page.getBody());
		
		if(redirectMatcher.find()) {
			// Extract page by group 1
			Page nlRedirect = extractPageByTitle("NL",redirectMatcher.group(1));
			if(nlRedirect == null) {
				return null;
			}
			Page enByRedirect;
			enByRedirect = findEnglishEquivalent(nlRedirect);
			if(enByRedirect == null) {
				enByRedirect = findAlternateEnglishEquivalent(nlRedirect);
			}
			return enByRedirect;
		}
		
		Pattern boldPattern = Pattern.compile("'''(.+?)'''");
		
		Matcher boldMatcher = boldPattern.matcher(page.getBody());
		Page enPage = null;
		
		while(boldMatcher.find() && enPage == null) {
			enPage = extractPageByTitle("EN",boldMatcher.group(1));
		}
				
		return enPage;
	}
	
	
	public String findTypeDisambiguation(Page p,ConcurrentMap<String,String>cachedPageTypes) {
		// Find all entities
		Set<String> entities = matchEntities(p);
		Page article = null;
		int PERcount = 0;
		int LOCcount = 0;
		int ORGcount = 0;
		int MIScount = 0;
		String type = "";
		
		// findType for each entity
		for(String entity : entities) {
			article = findArticle(entity);
			if(article != null) {
				type = findType(article,0,false,cachedPageTypes);
			}
			// accumulate votes
			if(type != null) {
				if(type.equals("PER")) {
					++PERcount;
				}
				else if(type.equals("LOC")) {
					++LOCcount;
				}
				else if(type.equals("ORG")) {
					++ORGcount;
				}
				else {
					++MIScount;
				}
			}
		}
		
		// makes decision
		if( PERcount > LOCcount &&
			PERcount > ORGcount &&
			PERcount > MIScount) {
			return "PER";
		}
		else if(LOCcount > PERcount &&
				LOCcount > ORGcount &&
				LOCcount > MIScount) {
			return "LOC";
		}
		else if(ORGcount > LOCcount &&
				ORGcount > PERcount &&
				ORGcount > MIScount) {
			return "ORG";
		}
		else if(MIScount > PERcount &&
				MIScount > LOCcount &&
				MIScount > ORGcount) {
			return "MISC";
		}
		return null;
	}
	
	/** Finds the entity type
	 * 
	 * @param p the page to find the type of
	 * @param depth current depth
	 * @param setDepth true if can set maximum depth, false if not
	 * @param cachedPageTypes local cache
	 * @return entity Type
	 */
	public String findType(Page p, int depth, boolean setDepth, ConcurrentMap<String,String> cachedPageTypes) {
		//Iterate through categoryMap, checking for each category in given Page.
		//if there is a match, add count for given match type.
		// N.B.: We naively assume that category counts make sense. 
		// Conflicting category types are not handled well - the order of categories
		// listed on the Wikipedia page affects the outcome.

		if(cachedPageTypes.containsKey(p.getTitle().toLowerCase())) {
			logger.debug("Hit!");
			return cachedPageTypes.get(p.getTitle().toLowerCase());
		}
		
		if(setDepth) {
			depthLimit = 1;
		}

		if(depth > depthLimit) {
			return null;
		}
		if(depth == 0) {
			countLOC = 0;
			countORG = 0;
			countPER = 0;
			countMISC = 0;
		}
		
		Set<String> keys = categoryMap.keySet();
		String type = "";


		// Also: keys is empty!!! CategoryMap is empty!!!
		for(String cat : p.getCategories()) {
			if(cachedPageTypes.containsKey(cat.toLowerCase())){
				logger.debug("Hit!");
				type = cachedPageTypes.get(cat.toLowerCase());
				if(type != null && type.equalsIgnoreCase("PERSON")){
					++countPER;
				}
				else if(type != null && type.equalsIgnoreCase("ORGANIZATION")){
					++countORG;
				}
				else if(type != null && (type.equalsIgnoreCase("GPE") || 
						(type.equalsIgnoreCase("FACILITY")) || 
						(type.equalsIgnoreCase("LOCATION")))){
					++countLOC;
				}
				else {
					++countMISC;
				}
			}
			else {
				for(String key : keys) {
					String originalKey = key;
					key.trim();
					if(!key.contains("^")) {
						key = ".*" + key;
					}
					if(!key.contains("$"))
						key = key.concat(".*");
					
					if(cat.matches(key)){
						type = categoryMap.get(originalKey);
						if(type != null && type.equalsIgnoreCase("PERSON")){
							++countPER;
						}
						else if(type != null && type.equalsIgnoreCase("ORGANIZATION")){
							++countORG;
						}
						else if(type != null && (type.equalsIgnoreCase("GPE") || (type.equalsIgnoreCase("FACILITY")))){
							++countLOC;
						}
						else {
							++countMISC;
						}
						break;
					}
				}
			}

			if( countPER >= typeThreshold &&
					countPER > countORG &&
					countPER > countLOC &&
					countPER > countMISC) {
				return "PER";
			}
			else if(countORG >= typeThreshold &&
					countORG > countPER &&
					countORG > countLOC &&
					countORG > countMISC) {
				return "ORG";
			}
			else if(countLOC >= typeThreshold &&
					countLOC > countORG &&
					countLOC > countPER &&
					countLOC > countMISC) {
				return "LOC";
			}
			else if(countMISC >= typeThreshold &&
					countMISC > countORG &&
					countMISC > countPER &&
					countMISC > countLOC) {
				return "MISC";
			}
		}

		Set<Page> pages = findSuperCategories(p);
		String superType;

		for(Page pa : pages) {
			if(pa != null) {
				superType = findType(pa, depth+1,false,cachedPageTypes);
				if(superType != null) {
				//	synchronized (cachedPageTypes) {
						cachedPageTypes.put(pa.getTitle().toLowerCase(), superType);
						logger.debug("Added # "+cachedPageTypes.size());
				//	}
					return superType;
				}
			}
		}
		if(depth == 0 && depthLimit < maxDepthLimit) {
			depthLimit++;
			return findType(p,1,false,cachedPageTypes);
		}

		return null;
	}


	/** Finds the over-arching entity type for a disambiguation page
	 * 
	 * @param p the page to search
	 * @param cachedPageTypes local cache
	 * @return the entity type
	 */
	public String findDisambiguationType(Page page, int i, ConcurrentMap<String,String> cachedPageTypes) {
		// Straight-up vote for top type using all entities on the page.

		if(cachedPageTypes.containsKey(page.getTitle().toLowerCase())) {
			logger.debug("Hit!");
			return cachedPageTypes.get(page.getTitle().toLowerCase());
		}
		
		Set<String> entities = matchEntities(page);
		Page disamPage = null;
		String type = null;

		int disambCountPER = 0;
		int disambCountORG = 0;
		int disambCountLOC = 0;
		int disambCountMISC = 0;
		
		for(String entity: entities) {
			if(cachedPageTypes.containsKey(entity.toLowerCase())) {
				logger.debug("Hit!");
				type = cachedPageTypes.get(entity.toLowerCase());
				if(type != null) {
					if(type.equalsIgnoreCase("PER")){
						++disambCountPER;
					}
					else if(type.equalsIgnoreCase("ORG")){
						++disambCountORG;
					}
					else if(type.equalsIgnoreCase("LOC")){
						++disambCountLOC;
					}
					else {
						++disambCountMISC;
					}
				}
			}
			disamPage = findArticle(entity);
			if(disamPage != null) {
				type = findType(disamPage, 0,false,cachedPageTypes);
				if(type != null) {
				//	synchronized (cachedPageTypes) {
						cachedPageTypes.put(entity.toLowerCase(), type);
				//	}
					if(type.equalsIgnoreCase("PER")){
						++disambCountPER;
					}
					else if(type.equalsIgnoreCase("ORG")){
						++disambCountORG;
					}
					else if(type.equalsIgnoreCase("LOC")){
						++disambCountLOC;
					}
					else {
						++disambCountMISC;
					}
				}
			}
		}
		if( disambCountPER > disambCountORG &&
			disambCountPER > disambCountLOC &&
			disambCountPER > disambCountMISC) {
			return "PER";
		}
		if( disambCountORG > disambCountPER &&
			disambCountORG > disambCountLOC &&
			disambCountORG > disambCountMISC) {
			return "ORG";
		}
		if( disambCountLOC > disambCountPER &&
			disambCountLOC > disambCountORG &&
			disambCountLOC > disambCountMISC) {
			return "LOC";
		}
		if( disambCountMISC > disambCountPER &&
			disambCountMISC > disambCountORG &&
			disambCountMISC > disambCountLOC) {
			return "MISC";
		}
		return null;
	}
	
	/** Finds super categories for a given page 
	 * 
	 * @param p page to find super categories for
	 * @return 
	 */
	private Set<Page> findSuperCategories(Page p) {
		Set<Page> superCats = Sets.newHashSet();
		for(String category: p.getCategories()) {
			Page catPage = extractPageByTitle("EN", "Category:"+category.trim());
			if(catPage != null) {
				superCats.add(catPage);
			}
		}	
		return superCats;
	}
	
	/** inserts entities with their matching types into the original body of text
	 * 
	 * @param currentPage the page to insert into
	 * @param titlePageMap map of all entity pages and their corresponding types
	 * @param cachedPageTypes local cache
	 */
	public void insertTypesIntoText(Page currentPage, Map<String,Pair<Page,Page>> titlePageMap, Map<String,String> cachedPageTypes) {
		entityMatcher = simpleEntityPattern.matcher(currentPage.getBody());

		while(entityMatcher.find(0)) {
			String group1lc = entityMatcher.group(1).toLowerCase();
			if (titlePageMap.containsKey(group1lc)) {
				if(titlePageMap.get(group1lc).getSecond() != null &&
					titlePageMap.get(group1lc).getSecond().getEntityType() != null) {
					currentPage.setBody(entityMatcher.replaceFirst("@@@"+entityMatcher.group(1)+","+titlePageMap.get(group1lc).getSecond().getEntityType()+"@@@"));
				}
				else if(titlePageMap.get(group1lc).getFirst() != null &&
					titlePageMap.get(group1lc).getFirst().getEntityType() != null){
					currentPage.setBody(entityMatcher.replaceFirst("@@@"+entityMatcher.group(1)+","+titlePageMap.get(group1lc).getFirst().getEntityType()+"@@@"));
				}
				else {
					currentPage.setBody(entityMatcher.replaceFirst(entityMatcher.group(1)));
				}
			}
			else if(cachedPageTypes.containsKey(group1lc)) {
				logger.debug("Used in insertTypesIntoText");
				currentPage.setBody(entityMatcher.replaceFirst("@@@"+entityMatcher.group(1)+","+cachedPageTypes.get(group1lc)+"@@@"));
			}
			else {
				currentPage.setBody(entityMatcher.replaceFirst(entityMatcher.group(1)));
			}
			entityMatcher = simpleEntityPattern.matcher(currentPage.getBody());
		}
			
		entityMatcher = entityPattern.matcher(currentPage.getBody());
		while(entityMatcher.find(0)) {
			String group1 = entityMatcher.group(1);
			String group2 = entityMatcher.group(2).substring(1);
			String group1lc = group1.toLowerCase();
			//String group2lc = group2.toLowerCase();
			
			if((entityMatcher.groupCount() == 2) &&
					titlePageMap.containsKey(group1lc)) {
				//titlePageMap.containsKey(entityMatcher.group(1))) {
				if( titlePageMap.get(group1lc).getSecond() != null &&
					titlePageMap.get(group1lc).getSecond().getEntityType() != null) {
						String temp = entityMatcher.replaceFirst(		
							"@@@"+
							group2+","+
							titlePageMap.get(group1lc).getSecond().getEntityType()+
							"@@@");
						currentPage.setBody(temp);
				}
				else if(titlePageMap.get(group1lc).getFirst() != null &&
						titlePageMap.get(group1lc).getFirst().getEntityType() != null) {
					String temp = entityMatcher.replaceFirst(		
								"@@@"+
								group2+","+
								titlePageMap.get(group1lc).getFirst().getEntityType()+
								"@@@");
					currentPage.setBody(temp);
				}
				else if( cachedPageTypes.containsKey(group1lc) ) {
					if( cachedPageTypes.get(group1lc) != null ) {
						logger.debug("Used in insertTypesIntoText");
						String temp = entityMatcher.replaceFirst(		
								"@@@"+
								group2+","+
								cachedPageTypes.get(group1lc)+
								"@@@");
						currentPage.setBody(temp);
					}
				}
				else {
					currentPage.setBody(entityMatcher.replaceFirst(group2));
				}
			} 
			else {
				currentPage.setBody(entityMatcher.replaceFirst(group2));
			}
			entityMatcher = entityPattern.matcher(currentPage.getBody());
		}
	}
	
	/**Inserts a given entity and its type into a text
	 * 
	 * @param currentPage the page to insert into
	 * @param entityType entity type
	 * @param entity entity
	 * @return Page containing the inserted text
	 */
	public Page insertTypeIntoText(Page currentPage, String entityType, String entity) {
		//make matcher here with group so that can replace group with @@@entity, entityTypw
		//search using String!
		
		//Pattern customEntityPattern = Pattern.compile("\\[\\[("+entity+")(|[a-zA-Z_\\s]+?)?\\]\\]");
		//entityMatcher = customEntityPattern.matcher(currentPage.body);	
		//entityMatcher.replaceAll("@@@"+entity+","+entityType+"@@@");
		
		String temp = currentPage.getBody().replaceAll(
				//"\\[\\[("+entity+")(|[a-zA-Z_\\s]+?)?\\]\\]", 
				"(?i)\\s("+entity+")",
				" @@@"+entity+","+entityType+"@@@");
		currentPage.setBody(temp);
		
		return currentPage;
		
	}
	
	public void matchRecurringEntities(Page currentPage, Map<String,Pair<Page,Page>> titlePageMap, Map<String,String> cachedPageTypes) {
		//use nl map entry to search for nl page title in current page body, and if matches,
		//replace with @@@entity,entityType @@@ syntax
		 for(Map.Entry<String, Pair<Page,Page>> entry : titlePageMap.entrySet()) {
			 if( entry.getValue().getFirst().getEntityType() != null &&
				 !entry.getValue().getFirst().getEntityType().equalsIgnoreCase("")) {
				 insertTypeIntoText(currentPage,entry.getValue().getFirst().getEntityType(),entry.getKey());
			 }
		 }
		 // Throws java.util.ConcurrentModificationException
		 // Choice is to lock it with synchronize(cachedPageTypes) and take the performance hit
		 // or to have a less complete body-text and possibly do another pass at a later date.
	/*	 for(Map.Entry<String,String> entry : cachedPageTypes.entrySet()) {
			 if( entry.getValue() != null ) {
				 insertTypeIntoText(currentPage,entry.getValue(),entry.getKey());
			 }
		 }*/
	}
	
	/** Finds entities of the type PER (Person) and marks them in the page
	 * 
	 * @param currentPage page to finds perons in
	 */
	public void findUnmarkedPeople (Page currentPage) {
		//search for X.X. Caps = Person
		//search for Mr. X/Mrs. X = Person
		//call insertTypeIntoText to insert entity type at appropriate person
		// AGL: I think it's much more efficient to do the replace here, rather
		// than capturing the string, sending it to insertTypeIntoText, and performing
		// the search again for replacement.
		Pattern pplPatternInitials  = Pattern.compile("([A-Z]\\.(\\s)?[A-Z]\\.(\\s)?[A-Z][a-z]+)");
		Pattern pplPatternHonorific = Pattern.compile(
				"((([Dd]hr\\.)|([Mm]evr\\.)|([Mm]w\\.)|([Dd]e heer)|([Mm]evrouw))" +
				"((([A-Z][a-z]+)[\\s-])+))");

		entityMatcher = pplPatternInitials.matcher(currentPage.getBody());
		
		while(entityMatcher.find(0)) {
			entityMatcher.replaceFirst(		
				"@@@"+
				entityMatcher.group(1)+","+
				"PER"+
				"@@@");
		}
		entityMatcher = pplPatternHonorific.matcher(currentPage.getBody());
		
		while(entityMatcher.find(0)) {
			entityMatcher.replaceFirst(		
				"@@@"+
				entityMatcher.group(1)+","+
				"PER"+
				"@@@");
		}	
	}
	
	public void checkRunningLists() {
		//call to identify more entities from self-created lists
	}
	
	public void addToRunningLists() {
		//call at the end of processing a page to add the recently found entities (in titlePageMap)
	}
	
	/** Saves a group of modified pages
	 * 
	 * @param currentPage page to save
	 * @param titlePageMap pages 
	 */
	public void save(Page currentPage, Map<String,Pair<Page,Page>> titlePageMap) {
		dao.updatePage(currentPage, "NL");
		dao.updatePages(titlePageMap);
		
	}
	
	/** Saves (updates) all fields of a page
	 * 
	 * @param currentPage the page to update
	 */
	public void save(Page currentPage) {
		dao.updatePage(currentPage, "NL");
		
	}


	/** Finds entities based on n-grams
	 * 
	 * @param currentPage page to look in
	 * @param nGramSize the size of the n gram
	 * @return a set of entities found
	 */
	public Set<String> findEntitiesUsingNGrams(Page currentPage, int nGramSize) {
		NGramMaker maker = new NGramMaker(currentPage.getBody());
		return maker.makeNGrams(nGramSize);
	}
	
	/** Process n grams: find their English equivalent, find their type, and insert type into text
	 * 
	 * @param nGramEntities a set of entities
	 * @param currentPage the page to work on
	 */
	public void processNGrams(Set<String> nGramEntities, Page currentPage) {
		
		for(String s : nGramEntities) {
			Page nlPage = extractPageByTitle("NL", s);
			if(nlPage != null) {
				Page enPage = findEnglishEquivalent(nlPage);
				if(enPage != null) {
					String type = findType(enPage, 1,true,null);
					if(type != null) {
						insertTypeIntoText(currentPage, type, s);
						// used to save(currentPage) here
					}
				}
			}
		}
		save(currentPage);	// moved from inside loop/conditional.
	}
	
	/** Converts an input stream into a String.
	 * 
	 * @param is
	 * @return
	 * @throws Exception
	 */
	private static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is,CHARSET));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line + "\n");
	    }
	    is.close();
	    return sb.toString();
	  }



	/**Determines if a page is a redirect
	 * 
	 * @param currentPage page to use
	 * @return true if redirect, false otherwise
	 */
	public boolean isRedirect(Page currentPage) {
		
		redirectMatcher = redirectPattern.matcher(currentPage.getBody());
		
		if(redirectMatcher.find()) {
			
			return true;
		}
		
		return false;
	}


	/** Follows a redirect to a new page
	 * 
	 * @param currentPage the redirect to follow
	 * @param langAbbrev the language of the page, abbreviated (EN or NL)
	 * @return new page (followed from the redirect)
	 */
	public Page followRedirect(Page currentPage, String langAbbrev) {
		
		assert(currentPage != null);
		
		redirectMatcher = redirectPattern.matcher(currentPage.getBody());
		
		if(redirectMatcher.find()) {
			
			return extractPageByTitle(langAbbrev,redirectMatcher.group(2));
		}
		
		return null;
	}



	/** Find the entity type
	 * NB: uses dutch categories; useful if no EN link exists
	 * 
	 * @param page current page
	 * @param cachedPageTypes local cache
	 * @return entity type
	 */
	public String findDutchType(Page page, ConcurrentMap<String,String> cachedPageTypes) {

		// Lookup dutch pages for each category.
		// For each category page, find english equivalent.
		// For each english equivalent, find type
		// Vote on type.
		
		Set<Page> dutchCats = Sets.newHashSet();
		Set<String> dutchCatsCached = Sets.newHashSet();
		Set<Page> englishCats = Sets.newHashSet();
		Set<String> workingSet = page.getCategories();

		boolean insufficientCategories = true;
		int counter = 0;
		
		countPER = 0;
		countORG = 0;
		countLOC = 0;
		countMISC = 0;

		while(insufficientCategories && counter++ < 3) {
			// workingset starts as the top page's categories
			// Iterate through the set and add all the retrieved pages to dutchCats
			for(String cat : workingSet) {
				if(!cachedPageTypes.containsKey(cat.toLowerCase())){
					Page category = extractPageByTitle("NL",cat);
					if(category != null) {
						dutchCats.add(category);
					}
				} else {
					dutchCatsCached.add(cat);
				}
			}
			
			// Find the English equivalent to all categories in the dutchCats collection
			for(Page category : dutchCats) {
				Page enCategory = findEnglishEquivalent(category);
				if(enCategory != null) {
					englishCats.add(enCategory);
				}
			}


			if(englishCats.size() > 0 || dutchCatsCached.size() > 0) {
				insufficientCategories = false; // exit loop
			}
			else {
				// Clear the workingSet to refill at a higher level.
				workingSet.clear();
				
				// For each category page in the dutchCats collection, find the list of supercategories
				// For each of these, add its categories to the workingset.
				for(Page dutchCategory : dutchCats) {
					for(Page workingPage : findSuperCategories(dutchCategory)) {
						if(workingPage != null) {
							workingSet.addAll(workingPage.getCategories());
						}
					}
				}
			}
		}
		
		// Find the category type of each english category
		// Accumulate votes 
		for(Page enCategory : englishCats) {
			String type = findType(enCategory, 0,false,cachedPageTypes);
			if(type != null) {
			//	synchronized(cachedPageTypes) {
					cachedPageTypes.put(enCategory.getTitle().toLowerCase(), type);
			//	}
			}
			if(type != null && type.equalsIgnoreCase("PER")){
				++countPER;
			}
			else if(type != null && type.equalsIgnoreCase("ORG")){
				++countORG;
			}
			else if(type != null && type.equalsIgnoreCase("LOC")){
				++countLOC;
			}
			else if(type != null && type.equalsIgnoreCase("MISC")){
				++countMISC;
			}
		}
		for(String cachedCat : dutchCatsCached) {
			String type = cachedPageTypes.get(cachedCat);

			if(type != null && type.equalsIgnoreCase("PER")){
				++countPER;
			}
			else if(type != null && type.equalsIgnoreCase("ORG")){
				++countORG;
			}
			else if(type != null && type.equalsIgnoreCase("LOC")){
				++countLOC;
			}
			else if(type != null && type.equalsIgnoreCase("MISC")){
				++countMISC;
			}
		}
		
		// Compare tallies
		if( 	countPER > countORG &&
				countPER > countLOC &&
				countPER > countMISC) {
			return "PER";
		}
		else if(countORG > countPER &&
				countORG > countLOC &&
				countORG > countMISC) {
			return "ORG";
		}
		else if(countLOC > countORG &&
				countLOC > countPER &&
				countLOC > countMISC) {
			return "LOC";
		}
		else if(countMISC > countORG &&
				countMISC > countPER &&
				countMISC > countLOC) {
			return "MISC";
		}
		else // No clear winner D:
			return null;
	}
}
