// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: March 18, 2010
 */
package wikipediaparser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.collect.Sets;

import dao.Dao;
import dao.IDao;

/* Parses a file of words (one word per line) and inserts into storage.
 * @author
 */
public class DictParser {

	public static final String CHARSET = "UTF-8";
	private Logger logger = Logger.getLogger(DictParser.class);
	private Set<String> entries;
	private String sourceFile = "eng_com.dic";
	private IDao dao;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");

		DictParser dp = new DictParser();
		dp.parseDictionary();
	}
	
	public DictParser() {
		entries = Sets.newLinkedHashSet();
		this.dao = new Dao();
	}
	
	/** Parses the file
	 * 
	 */
	public void parseDictionary() {
		BufferedReader reader = null;
		
		//open file for reading
		try {
			reader = new BufferedReader(
					new InputStreamReader(
							new DataInputStream(
									new FileInputStream(sourceFile)),CHARSET));
			
		} catch (FileNotFoundException e) {
			logger.error("Could not find the file for parsing", e);
			logger.info("Unable to continue without file - exiting");
			System.exit(1);
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding! ("+CHARSET+")",e);
			System.exit(1);
		}
		
		try {
			String line;
			
			while((line = reader.readLine()) != null) {
				entries.add(line.trim());
			}
		} catch (IOException e) {
			logger.warn("Error occured while processing entries", e);
		}
		
		dao.processDictionary(this.entries);
	}

}
