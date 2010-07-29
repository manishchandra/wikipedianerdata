// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: March 8, 2010
 */
package wikipediaparser;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import com.google.common.collect.*;

import dao.Dao;
import dao.IDao;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Language;
import model.Page;

import org.apache.log4j.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**This class parses, with the help of a Handler, an XML document using the SAX Parser. It also provides
 * functionality for manipulating and saving the gleaned data. 
 * 
 * @author Jessica Anderson
 */
public class Parser {

	public static final String CHARSET = "UTF-8";
	
	private SAXParser saxParser;
	private SAXParserFactory factory;
	private Handler handler;
	
	private static String sourceXML;
	private static long commit_size;
	private List<Page> pages;
	private static Language language;
	private IDao dao;
	private SyntaxStripper stripper;
	
	public Pattern enLink;
	public Pattern disambiguationPattern;
	
	private static Logger logger = Logger.getLogger(Parser.class);
    
	


	/**
	 * Possible arguments are:
	 * -f sourceXML file
	 * -c commit size (after how many pages to commit to storage)
	 * -l language (english or dutch)
	 */
	public static void main(String[] args) {
		
		PropertyConfigurator.configure("log4j.properties");
		
		sourceXML = "Wikipedia-20100228142334.xml";
		commit_size = 100;
		language = Language.ENGLISH;
		
		for(int i = 0; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-f")) {
				sourceXML = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-c")) {
				commit_size = Long.parseLong(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-l")) {
				String lang = args[i+1];
				language = Language.valueOf(lang.toUpperCase());
			}
		}
		
		try {
		Parser p = new Parser();
		p.parseFile(); //primary function of the program
		} catch(SAXException e) {
			logger.fatal("Problem creating new SAXParser", e);
			logger.info("Cannot continue without parser, exiting");
			System.exit(1);
		} catch (ParserConfigurationException p) {
			logger.fatal("Could not configure parser", p);
			logger.info("Cannot continue without parser, exiting");
			System.exit(1);
		} catch (IOException i) {
			logger.fatal("Could not open or read file", i);
			logger.info("Cannot continue without source file, exiting");
			System.exit(1);
		}
	}

	/** Constructor
	 * @throws ParserConfigurationException 
	 * 
	 */
	public Parser() throws SAXException, ParserConfigurationException {
		factory = SAXParserFactory.newInstance();
		handler = new Handler(this);
		pages = Lists.newLinkedList();
		dao = new Dao();
		stripper = new SyntaxStripper(language);
		
		saxParser = factory.newSAXParser();
		
	}

	
	
	/**Run method for parsing the file
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public void parseFile() throws SAXException, IOException {
		InputSource inputSource = new InputSource();
		inputSource.setByteStream(new DataInputStream(new FileInputStream(sourceXML)));
		inputSource.setEncoding(CHARSET);
		saxParser.parse(inputSource, handler);
	}

	
	/** Non-SAX parsing for all <text></text> information per page. Initializes page fields
	 * for available data and strips unnecessary syntax out of the text body. Page is fully initialized
	 * and ready for use after running this method.
	 * 
	 * @param page the page whose text to parse
	 */
	protected void parseBody(Page page) {
		logger.debug("Parsing body");
		if(language == Language.ENGLISH) {
			enLink = Pattern.compile("\\[\\[en:(.+?)\\]\\]");
		    disambiguationPattern = Pattern.compile("\\{\\{disambig\\}\\}");
		}
		else {
			enLink = Pattern.compile("\\[\\[en:(" +
		    		".+?" +
		    		")\\]\\]");
		    disambiguationPattern = Pattern.compile("\\{\\{dp\\}\\}");
		}
		buildCategories(page);
		page.setDisambiguation(detectDisambiguation(page));
		page.setEnglishLink(extractInterwikiLink(page));
		page.setBody(stripper.stripWikipediaSyntax(page.getBody()));
	}
	
	/** Detects whether the given page is a disambiguation page or not.
	 * 
	 * @param page the page to check
	 * @return true if disambiguation page, false otherwise
	 */
	private boolean detectDisambiguation(Page page) {

		Matcher disMatcher = disambiguationPattern.matcher(page.getBody());
		
		if(disMatcher.find()) {
			return true;
		}
		else {
			return false;
		}	
	}

	/** Looks for an interwiki link in the current document.
	 * 
	 * @param page
	 * @return String containing the enLink or if does not exist, "" (empty String)
	 */
	protected String extractInterwikiLink(Page page) {
		
		Matcher enMatch = enLink.matcher(page.getBody());
		
		//english pages don't have english links
		if(language == Language.ENGLISH || !(enMatch.find())) {
			return "";
		}
		else
			return enMatch.group(1);
	}

	/** Retrieves and stores all categories for a page
	 * 
	 * @param page the page to use
	 */
	protected void buildCategories(Page page) {
		int startAt = 0;
		int nextCatLocation = 0;
		int nextCloseCat = 0;
		int nextPipe = 0;
		int nextBrackets = 0;
		int nextComment = 0;
		String category = "";
		String categoryIdentifier;
		
		if(language == Language.ENGLISH) {
			categoryIdentifier = "[[Category:";
		}
		else {
			categoryIdentifier = "[[Categorie:";
		}
		
		//TODO: Change to Patterns/Matchers
		while((nextCatLocation = page.getBody().indexOf(categoryIdentifier, startAt)) != -1) {
			//Pipes, brackets, and comments can occur so we check to see which comes first.
			nextBrackets = page.getBody().indexOf("]]",nextCatLocation);
			nextPipe = page.getBody().indexOf("|",nextCatLocation);
			nextComment = page.getBody().indexOf("<!--",nextCatLocation);
			
			//need to compare substring values to see who comes next
			if(nextBrackets == -1 && nextPipe == -1) {
				if(nextComment == -1) break;
				else nextCloseCat = nextComment;
			}
			else if(nextPipe == -1)
			{
				if(nextComment == -1) {
					nextCloseCat = nextBrackets;
				}
				else {
					nextCloseCat = nextBrackets < nextComment ? nextBrackets : nextComment;
				}
			}
			else if(nextBrackets == -1)
			{
				if(nextComment == -1) {
					nextCloseCat = nextPipe;
				}
				else {
					nextCloseCat = nextPipe < nextComment ? nextPipe : nextComment;
				}
			}
			else if(nextComment == -1) {
				nextCloseCat = nextBrackets < nextPipe ? nextBrackets : nextPipe;
			}
			else {
				int temp = Math.min(nextBrackets, nextPipe);				
				nextCloseCat = Math.min(temp, nextComment);
			}
			
			//
			category = page.getBody().substring(nextCatLocation+categoryIdentifier.length(),nextCloseCat);
			page.getCategories().add(category);
			startAt = nextCloseCat;
		}
	}

	
	/**Add a page to the pages list, which indicates it should be stored
	 * 
	 * @param p the page to add to the collection
	 * @return reference to the page object
	 */
	Page addPage(Page p) {
		assert(p != null); //check that p is not null!
		pages.add(p);
		return p;
	}

	/** Commits all pages in pages (list of pages) every so often (Depending on commit size). 
	 * Called by handler at the end of every page tag.
	 */
	protected void commitPage(boolean forceCommit) {
		
		if(forceCommit || pages.size() % commit_size == 0) {
			assert(language != null);
			
			dao.processBatch(language.abbreviation(), pages);
			pages.clear(); //clear out pages as they've been committed.
		}
			
	}
	
	public String getSourceDocument() {
		return sourceXML;
	}
	
	public void setSourceDocument(String sourceDocument) {
		sourceXML = sourceDocument;
	}
	
	public long getCommitSize() {
		return commit_size;
	}
	
	public void setCommitSize(long size) {
		commit_size = size;
	}
}
