// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 12, 2010
 * 
 */
package dao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.*;

import java.sql.ResultSet;

import model.Page;
import model.Pair;



/** Data access object for the WikipediaParser schema instance and the project's model. 
 * 
 * @author Jessica Anderson
 */
public class Dao implements IDao {
	
	private Connection conn;
	
	private static Logger logger = Logger.getLogger(Dao.class);
	
	private String userName;
	private String password;
	private String url;
	
	public Dao() {
		conn = null;
        try
        {
            userName = "root";
            password = "Saw4rea51";
            url = "jdbc:mysql://localhost/wikipedia_clean";
            Class.forName ("com.mysql.jdbc.Driver").newInstance ();
            conn = DriverManager.getConnection (url, userName, password);
            System.out.println ("Database connection established"); //change to logger
            conn.setAutoCommit(false);
        } catch (SQLException e) {
        	logger.error("Cannot connect to database server", e);
        } catch (Exception e) {
        	logger.warn("Unidentified exception", e);
        }
		
	}
	
	/* Updates the body (main text) of a page. It does not update any other fields.
	 * 
	 * (non-Javadoc)
	 * @see dao.IDao#updatePage(model.Page, java.lang.String)
	 */
	public void updatePage(Page page, String langAbbrev) {
		assert page != null;
		assert langAbbrev != null;
		
		PreparedStatement updatePage = null;
		
		try {
			updatePage = conn.prepareStatement(
					"UPDATE PAGES_" + langAbbrev + " SET body = ? WHERE id = ? LIMIT 1");
			
		} catch(SQLException s) {
			logger.warn("Could not make prepared statements", s);
		}

		try {
			InputStream is = new ByteArrayInputStream(page.getBody().getBytes("UTF-8"));
			updatePage.setBinaryStream(1, is);//, page.getBody().length());
			updatePage.setLong(2, page.getId());
			updatePage.execute();
			conn.commit();
			
		} catch(SQLException s) {
			logger.warn("Could not insert data", s);
		} catch (Exception e) {
			logger.info("Unidentified exception", e);
		}
	}
	
	/** Updates all pages' entityType in the database.
	 * 
	 * @param articles A map containing the pages to be updated
	 * 
	 */
	public void updatePages(Map<String, Pair<Page, Page>> articles) {

		PreparedStatement updatePageNL = null;
		PreparedStatement updatePageEN = null;
		
		//first try to make prepared statements
		try {
			updatePageNL = conn.prepareStatement(
					"UPDATE PAGES_NL SET entity_type = ? WHERE id = ? LIMIT 1");
			updatePageEN = conn.prepareStatement(
					"UPDATE PAGES_EN SET entity_type = ? WHERE id = ? LIMIT 1");
			
		} catch(SQLException s) {
			logger.warn("Could not make prepared statements");
		}
		
		try {
			for(Pair<Page,Page> pagePair : articles.values()) {
				if(pagePair.getFirst() != null && pagePair.getFirst().getEntityType() != null && 
						!(pagePair.getFirst().getEntityType().equals(""))) {
					updatePageNL.setString(1, pagePair.getFirst().getEntityType());
					updatePageNL.setLong(2, pagePair.getFirst().getId());
					updatePageNL.execute();
				}
				if(pagePair.getSecond() != null && pagePair.getSecond().getEntityType() != null && 
						!(pagePair.getSecond().getEntityType().equals(""))) {
					updatePageEN.setString(1, pagePair.getSecond().getEntityType());
					updatePageEN.setLong(2, pagePair.getSecond().getId());
					updatePageEN.execute();
				}
			}
			conn.commit();
		} catch(SQLException e) {
			logger.warn("Could not insert data");
		} catch (Exception e) {
			logger.info("Give me a break");
			e.printStackTrace();
		}
	}
	
	/* 
	 * 
	 * Note: The page ids must be new and unique.
	 * 
	 * (non-Javadoc)
	 * @see dao.IDao#processBatch(java.lang.String, java.lang.String, java.util.List)
	 */
	public void processBatch(String langAbbrev, List<Page> pages) {
		
		assert pages != null;
		assert !(pages.isEmpty());
		
		PreparedStatement insertPages = null;
		PreparedStatement insertCategories = null;
		
		//first try to make prepared statements
		try {
			insertPages = conn.prepareStatement(
					"INSERT INTO PAGES_" + langAbbrev + " (id, title, body, en_link, dp) VALUES (?, ?, ?, ?, ?)");
			insertCategories = conn.prepareStatement(
					"INSERT INTO CATS_" + langAbbrev + " VALUES (?, ?)");
		} catch(SQLException s) {
			logger.warn("Could not make prepared statements");
		}
		
		//now try to process pages and their categories
		try {
			
			for(Page p : pages) {
				insertPages.setLong(1, p.getId());
				insertPages.setString(2, p.getTitle());
				InputStream is = null;
			
				is = new ByteArrayInputStream(p.getBody().getBytes("UTF-8"));
				insertPages.setBinaryStream(3, is);//, p.getBody().length());
				
				if(p.getEnglishLink() != null && p.getEnglishLink().length()>0) {
					insertPages.setString(4, p.getEnglishLink());
				} else {
					insertPages.setString(4, "");
				}
				insertPages.setBoolean(5, p.isDisambiguation());
				insertCategories.setLong(1, p.getId());
				insertPages.execute(); //record whether success or not
				
				//now try to add categories for the current page
				for(String s : p.getCategories()){
					insertCategories.setString(2, s);
					insertCategories.execute();	
				}
			}
			conn.commit();
			
		} catch (SQLException e) {
			logger.warn("Problem with database", e);
		} catch (UnsupportedEncodingException u) {
			logger.warn("Conversion to UTF-8 failed", u);
		}
	
	}
	
	/* (non-Javadoc)
	 * @see dao.IDao#processDictionary(java.util.Set)
	 */
	public void processDictionary(Set<String> words) {

		PreparedStatement insertWord = null;
		
		//first try to make prepared statements
		try {
			insertWord = conn.prepareStatement(
					"INSERT INTO DICTIONARY VALUES (?, ?)");
			
		} catch(SQLException s) {
			logger.warn("Could not make prepared statements");
		}
		

		try {
			int counter = 1;
			for(String s : words) {
				insertWord.setInt(1, counter);
				insertWord.setString(2, s);
				insertWord.execute();
				counter++;
			}
			
			conn.commit();
		} catch(SQLException e) {
			logger.warn("Could not insert data");
		}
		
	}
	
	/* (non-Javadoc)
	 * @see dao.IDao#retrievePageById(java.lang.String, long)
	 */
	public ResultSet retrievePage(String langAbbrev, long id) {
		assert langAbbrev != null;
		//assert !(langAbbrev.equalsIgnoreCase(""));
		assert id > 0;
		
		PreparedStatement extractPage = null;
		
		try {
			extractPage = conn.prepareStatement(
					"SELECT * FROM PAGES_" + langAbbrev + " WHERE id = ? LIMIT 1");
		} catch (SQLException e) {
			logger.warn("Could not create PreparedStatement", e);
			e.printStackTrace();
		}
		
		try {
			extractPage.setLong(1, id);
			ResultSet results = extractPage.executeQuery();
			return results;
		} catch (SQLException e) {
			logger.warn("Could not successfully execute query", e);
			e.printStackTrace();
		}
		return null;
	
	}
	
	/* (non-Javadoc)
	 * @see dao.IDao#retrievePageByTitle(java.lang.String, java.lang.String)
	 */
	public ResultSet retrievePage(String langAbbrev, String title) {
		assert langAbbrev != null;
		assert title != null;
		
		PreparedStatement extractPage = null;
		try {
			extractPage = conn.prepareStatement(
					//"SELECT * FROM PAGES_" + langAbbrev + " WHERE title LIKE ? COLLATE utf8_general_ci LIMIT 1");
					"SELECT * FROM PAGES_" + langAbbrev + " WHERE title LIKE ? COLLATE utf8_bin LIMIT 1");//COLLATE utf8_general_cs
		} catch (SQLException e) {
			logger.warn("Could not create PreparedStatement", e);
		}
		
		try {
			ResultSet results = null;
			for(int attempt = 0; attempt < 2 && (results == null || !results.first()); ++attempt) {
				title.trim();
				if(attempt > 0) {
					if(title.charAt(0) != title.toLowerCase().charAt(0)) {
						title = title.toLowerCase();
					}
					else {
						title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
					}
				}
				extractPage.setString(1, title);
				results = extractPage.executeQuery();
			}
			return results;
		} catch (SQLException e) {
			logger.warn("Could not successfully execute query", e);
		}
		return null;
	
	}
	
	/* (non-Javadoc)
	 * @see dao.IDao#retrieveCategoriesPerPage(java.lang.String, long)
	 */
	public ResultSet retrieveCategoriesPerPage(String langAbbrev, long id) {
		assert langAbbrev != null;
		assert id > 0;
		
		PreparedStatement extractCats = null;
		try {
			extractCats = conn.prepareStatement(
					"SELECT * FROM CATS_" + langAbbrev + " WHERE page_id = ?");
		} catch (SQLException e) {
			logger.warn("Could not create PreparedStatement", e);
			e.printStackTrace();
		}
		
		try {
			ResultSet results = null;
			if(id != 0) {
				extractCats.setLong(1, id);
				results = extractCats.executeQuery();
			}
			return results;
			
		} catch (SQLException e) {
			logger.warn("Could not successfully execute query", e);
			e.printStackTrace();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see dao.IDao#selectNoun(java.lang.String)
	 */
	public boolean selectNoun(String s) {
		assert s != null;
		
		PreparedStatement extractNoun = null;
		boolean hasResults = false;
		try {
			extractNoun = conn.prepareStatement(
					"SELECT * FROM DICTIONARY" + " WHERE name = ? COLLATE utf8_general_ci LIMIT 1");
		} catch (SQLException e) {
			logger.warn("Could not create PreparedStatement", e);
			e.printStackTrace();
		}
		
		try {
			
			extractNoun.setString(1, s);
			ResultSet results = extractNoun.executeQuery();
			
			if(results.first() && results.getString(2) != null) {
				hasResults = true;
			}
			
		} catch (SQLException e) {
			logger.warn("Could not successfully execute query", e);
			e.printStackTrace();
		}
		return hasResults;
	}
	
	

}
