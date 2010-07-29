// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 10, 2010
 */
package wikipediaparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.*;

import com.google.common.collect.Maps;

/** Takes a (perl) file and parses it into a map containing EntityType and name (of entity). 
 * 
 * File should consist of lines of the form: "fiction fans"=>"PERSON". ^ and & are allowed as
 * anchors. If not present, ".*" is placed before and after words. PERSON, FACILITY, GPE, and LOCATION are
 * valid, and any other entity types will either be not used or lumped together as MISC. 
 * 
 * @author Jessica Anderson
 */
public class CategoryParser {

	private String sourceFile;
	private static Logger logger = Logger.getLogger(CategoryParser.class);

	/** Constructor
	 * @param sourceFile a perl file containing category titles and and their respective entity type.
	 * 
	 */
	public CategoryParser(String sourceFile) {
		assert sourceFile != null;
		this.sourceFile = sourceFile;
	}

	/** Parses a perl file.
	 * 
	 * @return Map containing the category name and the entity type.
	 */
	public Map<String,String> parse() {
		Map<String,String> catMap = Maps.newHashMap();
		
		Pattern categoryPattern = Pattern.compile("\"(.+?)\"" +	// ex. "education institutions"
				"=>" +				// =>
				"\"(.+?)" + 		//ex. "ORGANIZATION
				"(\\+\\+.+?)?" + 	//ex. ++academic (optional)
				"\"");				//"
		
		InputStream inputStream = null;
		String sourceString = null;
		
		try {
			inputStream = new FileInputStream(sourceFile);
			sourceString = convertStreamToString(inputStream);
		} catch (FileNotFoundException f) {
			logger.error("Could not find file for parsing", f);
			logger.info("Unable to continue without file - exiting");
			System.exit(1);
		} catch(Exception e) {
			logger.error("Could not convert file into String", e);
			logger.info("Unable to continue without String - exiting");
			System.exit(1);
		}
		
		StringTokenizer tokenizer = new StringTokenizer(sourceString,",");
		Matcher categoryMatcher;
		
		while(tokenizer.hasMoreTokens()) {
			categoryMatcher = categoryPattern.matcher(tokenizer.nextToken().trim());
			if(categoryMatcher.matches() && categoryMatcher.groupCount() > 1) {
				catMap.put(categoryMatcher.group(1), categoryMatcher.group(2));
			}
		}
		return catMap;	
	}
	
	/* Converts an inputStream into a String
	 * @param is the inputStream to convert
	 * 
	 */
	private static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line + "\n");
	    }
	    is.close();
	    return sb.toString();
	  }
}
 