// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 23, 2010
 */
package wikipediaparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Language;

import org.apache.log4j.Logger;

/** Strips certain information of a (presumably wikipedia) text. For a full list of everything that is
 * stripped, please refer to source code.
 * 
 * @author Jessica Anderson
 *
 */
public class SyntaxStripper {
	
	private Pattern bottomText; //any sections below the main article which aren't interesting (refs, see also)
	private Pattern defaultSort;
	private Pattern category;
	private Pattern infobox;
	private Pattern articleIssues;
	private Pattern tables;
	private Pattern image;
	private Pattern ref;
	private Pattern refLong;
	private Pattern file;
	private Pattern seeAlso2;
	private Pattern comment;
	private Pattern cite;
	private Pattern pp_move_indef;
	private Pattern legend;
	private Pattern hashmark;
	private Pattern asterix;
	private Pattern bold;
	private Pattern mainDef;
	private Pattern mainRef;
	private Pattern afbeelding;
	private Pattern bestand;
	
	private Language language;
	
	private static Logger logger = Logger.getLogger(SyntaxStripper.class);

	/** Constructor: invoking this constructor carries a high cost as all Patterns are initialized 
	 * for the given language. This is a one-time cost for the life of the class, so use wisely!
	 * 
	 * @param langAbbrev
	 */
	public SyntaxStripper(Language language) {
		this.language = language;
		
		if(language == Language.ENGLISH) {
			initializeEnglishPatterns();
		}
		else
			initializeDutchPatterns();
	}
	
	/** Initializes English language regex patterns for manipulating and stripping wikipedia data. These
	 * are unfortunately language specific in most cases. This method is costly but only has to execute
	 * once for lifetime of the program usage of regexes. 
	 * 
	 */
	private void initializeEnglishPatterns() {
		logger.info("Initializing English patterns");
		
		bottomText= Pattern.compile("==\\s?" +
				"([Nn]otes|[Ff]urther [Rr]eading|[Ss]ee [Aa]lso|[Rr]eferences|[Ee]xternal [Ll]inks)" +
				"\\s?==" +
				".*",Pattern.DOTALL);		
		
		defaultSort = Pattern.compile("\\{\\{" +
				"DEFAULTSORT" +
				".*",Pattern.DOTALL);
		
		category = Pattern.compile("\\[\\[" +
				"Category:" +
				".*",Pattern.DOTALL);
		
		infobox = Pattern.compile("\\{\\{" +
				"\\s*[Ii]nfobox" + 
				".+?"+
			"\\}\\}",Pattern.DOTALL);
		
		articleIssues = Pattern.compile("\\{\\{[Aa]rticle [Ii]ssues" +
				".+?" +
				"\\}\\}",Pattern.DOTALL);

		tables = Pattern.compile("\\{\\|" +
				".+?" +
				"\\|\\}",Pattern.DOTALL);
		
		image = Pattern.compile("\\[\\[[Ii]mage:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);
		
		ref = Pattern.compile("<ref"+"[^>]*?/>",Pattern.DOTALL);
		
		refLong = Pattern.compile("<ref"+".*?"+"</ref>",Pattern.DOTALL);
		
		file = Pattern.compile("\\[\\[[Ff]ile:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);
	    
	    seeAlso2 = Pattern.compile("\\{\\{[Ss]ee [Aa]lso" +	// Start
				".+?"+
				"\\}\\}",Pattern.DOTALL);				// end
	    
	    comment = Pattern.compile("<!--" +
	    		".+?"+
	    		"-->",Pattern.DOTALL);
	    
	    cite = Pattern.compile("\\{\\{[Cc]ite " +
	    		".+?" +
	    		"\\}\\}",Pattern.DOTALL);
	    
	    pp_move_indef = Pattern.compile("\\{\\{[Pp]p-move-indef\\}\\}");
	    
	    legend = Pattern.compile("\\{\\{[Ll]egend"+
	    		".+?" +
	    		"\\}\\}",Pattern.DOTALL);
	    

	    hashmark = Pattern.compile("#+");
	    
	    asterix = Pattern.compile("\\*+");
	    
	    bold = Pattern.compile("'''");
	    
	    mainDef = Pattern.compile("\\{\\{[Mm]ain\\|" +
	    		".+?"+
	    		"\\}\\}",Pattern.DOTALL);
	    
	    mainRef = Pattern.compile(":\\s?''[Mm]ain [Aa]rticle\\s?:" +
	    		".+?" +
	    		"''");
	}
	
	/** Initializes Dutch language regex patterns for manipulating and stripping wikipedia  These
	 * are unfortunately language specific in most cases. This method is costly but only has to execute
	 * once for lifetime of the program usage of regexes. 
	 * 
	 */
	private void initializeDutchPatterns() {
		logger.info("Initializing Dutch patterns");
		
		bottomText= Pattern.compile("==\\s?" +
				"([Nn]oten|" +
				"[Zz]ie ook|" +
				"[Bb]ron(nen)|" +
				"[Ee]xterne link(s)?|" +
				"[Rr]eferentie(s)?|" +
				"[Hh]oeslink|" +
				"[Bb]ibliografie|" +
				"[Ll]iteratuur( en bronnen)?|" +
				"[Pp]ublicaties|" +
				"[Bb]ronvermelding)" +
				"\\s?==" +
				".*",Pattern.DOTALL);		
		
		defaultSort = Pattern.compile("\\{\\{"+
				"(DEFAULTSORT|"+
				"[Nn]avigatie|"+
				"[Bb]ron|" +
				"[Bb]eginnetje)"+
				".*",Pattern.DOTALL);

		category = Pattern.compile("\\[\\[" +
				"[Cc]ategorie:" +
				".*",Pattern.DOTALL);	

		infobox = Pattern.compile("\\{\\{" +
				"[Ii]nfobox" +
				".+?" +
			"\\}\\}",Pattern.DOTALL);

		tables = Pattern.compile("\\{\\|" +
				".+?" +
				"\\|\\}",Pattern.DOTALL);
		
		image = Pattern.compile("\\[\\[[Ii]mage:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);
		
		//this one can occur in addition to image
		afbeelding = Pattern.compile("\\[\\[[Aa]fbeelding:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);

		ref = Pattern.compile("<ref"+"[^>]*?/>",Pattern.DOTALL);
		
		refLong = Pattern.compile("<ref"+".*?"+"</ref>",Pattern.DOTALL);

		file = Pattern.compile("\\[\\[[Ff]ile:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);
	
		bestand = Pattern.compile("\\[\\[[Bb]estand:" +
				"[^\\[\\]]+?" +
				"\\]\\]",Pattern.DOTALL);
		
		//{{zieook //{{Zie ook //{{zie artikel //{{Zieartikel
	    seeAlso2 = Pattern.compile("\\{\\{[Zz]ie\\s?(ook|artikel)" +
				".+?" +
				"\\}\\}",Pattern.DOTALL);
	    
	    comment = Pattern.compile("<!--" +
	    		".+?" +
	    		"-->",Pattern.DOTALL);
	    
	    cite = Pattern.compile("\\{\\{[Cc]iteer " +
	    		".+?" +
	    		"\\}\\}",Pattern.DOTALL);
	    
	    legend = Pattern.compile("\\{\\{[Ll]egend" +
	    		".+?" +
	    		"\\}\\}",Pattern.DOTALL);
	
	    hashmark = Pattern.compile("#+");
	    
	    asterix = Pattern.compile("\\*+");
	    
	    bold = Pattern.compile("'''");
	    
	    mainDef = Pattern.compile("\\{\\{[Hh]oofdartikel\\|" +
	    		".+?" +
	    		"\\}\\}",Pattern.DOTALL);
	}
	
	/** Strips a wikipedia text of multiple un-needed constructs.
	 * 
	 * @param text the text to manipulate
	 * @param language EN or NL
	 * @return the manipulated text
	 */
	public String stripWikipediaSyntax(String text) {
		String result = text;
		logger.debug("Result is originally set to text - if stripping fails it will return original string");
		
		if(language == Language.ENGLISH) {
			result = stripWikipediaSyntaxEnglish(text);
		}
		else if(language == Language.DUTCH) {
			result = stripWikipediaSyntaxDutch(text);
		}
		
		return result;
	}

	/** Strips a Dutch wikipedia text of multiple un-needed constructs.
	 * 
	 * @param text the text to manipulate
	 * @return the manipulated text
	 */
	private String stripWikipediaSyntaxDutch(String text) {
		Matcher matcher = bottomText.matcher(text);
		text = matcher.replaceFirst("");
	        
		matcher = defaultSort.matcher(text);
		text = matcher.replaceFirst("");
		
		matcher = category.matcher(text);
		text = matcher.replaceFirst("");

		matcher = infobox.matcher(text);
		text = matcher.replaceFirst("");			
			
		matcher = tables.matcher(text);
		text = matcher.replaceAll("");
			
		matcher = image.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = afbeelding.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = ref.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = refLong.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = file.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = bestand.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = seeAlso2.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = comment.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = cite.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = legend.matcher(text);
		text = matcher.replaceAll("");
    
//		matcher = hashmark.matcher(text);
//		text = matcher.replaceAll("");
	    
		matcher = asterix.matcher(text);
		text = matcher.replaceAll("");
	    
		if(text.length() > 0) {
			matcher = bold.matcher(text);
			matcher.region(Math.min(100, text.length()-1), text.length()-1);
			text = matcher.replaceAll("");
		}

		return text;
	    
	}


	/** Strips a Dutch wikipedia text of multiple un-needed constructs.
	 * 
	 * @param text the text to manipulate
	 * @return the manipulated text
	 */
	private String stripWikipediaSyntaxEnglish(String text) {	
		
		Matcher matcher = bottomText.matcher(text);
		text = matcher.replaceFirst("");
		
		matcher = defaultSort.matcher(text);
		text = matcher.replaceFirst("");
		
		matcher = category.matcher(text);
		text = matcher.replaceFirst("");

		matcher = infobox.matcher(text);
		text = matcher.replaceFirst("");
			
		matcher = articleIssues.matcher(text);
		text = matcher.replaceFirst("");
			
		matcher = tables.matcher(text);
		text = matcher.replaceAll("");
			
		matcher = image.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = ref.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = refLong.matcher(text);
		text = matcher.replaceAll("");
		
		matcher = file.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = seeAlso2.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = comment.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = cite.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = pp_move_indef.matcher(text);
		text = matcher.replaceAll("");
	    
		matcher = legend.matcher(text);
		text = matcher.replaceAll("");
    
//		matcher = hashmark.matcher(text);
//		text = matcher.replaceAll("");
	    
		matcher = asterix.matcher(text);
		text = matcher.replaceAll("");
	    
		if(text.length() > 0) {
			matcher = bold.matcher(text);
			matcher.region(Math.min(100, text.length()-1), text.length()-1);
			text = matcher.replaceAll("");
		}

		matcher = mainDef.matcher(text);
		while(matcher.find()) {
			if(matcher.groupCount() > 0) {
				//text.replace(target, replacement)
			}
		}
	    
		matcher = mainRef.matcher(text);
		text = matcher.replaceAll("");
		
		return text;
		
	}

}