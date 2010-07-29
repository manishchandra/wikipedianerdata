// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
package dao;
/**
 * Author: Jessica Anderson
 * Created: February 12, 2010
 */
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Page;
import model.Pair;

/** Data Access Object interface that maps between the project's model and the database schema.
 * 
 * @author Jessica Anderson
 *
 */
public interface IDao {
	
	/** Updates the page representation 
	 * @param page the page to be updated
	 * @param langAbbrev the language of the Page as an abbreviation
	 * 
	 */
	void updatePage(Page page, String langAbbrev);

	/** Inserts each page in a list to the database, in the specified language. 
	 * 
	 * @param tableName the table name to use (<code>PAGES_NL</code> or <code>PAGES_EN currently</code>)
	 * @param pages The list of pages to be processed
	 */
	void processBatch(String langAbbrev, List<Page> pages); 
	
	
	/** Inserts a set of words to the database.
	 * 
	 * @param words
	 */
	void processDictionary(Set<String> words);

	/** Finds a page based on id
	 * 
	 * @param langAbbrev two letter language abbrevation (ex. EN or NL)
	 * @param id the page id
	 * @return ResultSet The results of the search
	 */
	ResultSet retrievePage(String langAbbrev, long id);

	/**Finds a page based on title
	 * 
	 * @param langAbbrev two letter language abbrevation (ex. EN or NL)
	 * @param id the page id
	 * @return ResultSet The results of the search
	 */
	ResultSet retrievePage(String langAbbrev, String title);
	
	/** Finds all categories for a given page
	 * @param langAbbrev two letter language abbrevation (ex. EN or NL)
	 * @param id the page id 
	 * 
	 */
	ResultSet retrieveCategoriesPerPage(String langAbbrev, long id);
	
	/** Searches for a noun in a dictionary
	 * 
	 * @param s the noun to look up
	 * @return true if noun found, false otherwise
	 */
	boolean selectNoun(String noun);

	void updatePages(Map<String, Pair<Page, Page>> titlePageMap);

	
	

}