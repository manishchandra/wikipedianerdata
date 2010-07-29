// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 18, 2010
 * 
 */
package model;

/**Enum representing a language
 * 
 * @author Jessica Anderson
 *
 */
public enum Language {

	ENGLISH("EN"),
	DUTCH("NL");
	
	private String abbrev;
	
	Language(String abbreviation) {
		this.abbrev = abbreviation;
	}
	
	public String abbreviation() {
		return abbrev;
	}
}
