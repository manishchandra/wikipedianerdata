// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: March 8, 2010
 */
package wikipediaparser;

import model.Page;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Provides handlers for events coming from SAXParser. These allow for the parsing of an XML document.
 * 
 * This class provides specific handlers for finding <code>page</code> information 
 * in a wikipedia XML document. 
 * 
 * @author Jessica Anderson
 */
public class Handler extends DefaultHandler {

	private Parser parser;
	private Page page;
	private State state;
	private boolean parseStuff;
	
	public enum State {
		UNDEFINED, PAGE, TITLE, ID, TEXT;
	}
	
	/** Constructor */
	public Handler(Parser parser) {
		this.parser = parser;
		state = State.UNDEFINED;
	}
	
/**** Event Handlers ****/
	
	
	/** Looks for start tags in a wikipedia xml document: specifically <mediawiki>, <page>, <title>,
	 * <id>, <text>
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if(qName.equalsIgnoreCase("mediawiki")) {
			state = State.UNDEFINED;
			
		}
		else if(qName.equalsIgnoreCase("page")) {
			// Start a new one
			state = State.PAGE;
			page = new Page();
			parser.addPage(page);
			parseStuff = true;
			
		}
		else if(qName.equalsIgnoreCase("title")) {
			page.setTitle("");
			state = State.TITLE;
		}
		else if(qName.equalsIgnoreCase("id")) {
			state = State.ID;
			
		}
		else if(qName.equalsIgnoreCase("text")) {
			// Get ready to parse!!
			page.setBody("");
			state = State.TEXT;
		}
	}
	
	/**
	 * Takes action every time text/characters are found between a tag set.
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String str = String.copyValueOf(ch, start, length);
		
		switch(state) {
		case TITLE:
			String title = page.getTitle() + str;
			page.setTitle(title);
			break;
		case ID:
			if(parseStuff) {
			page.setId(Long.parseLong(str));
			parseStuff = false;
			}
			break;
		case TEXT:
			String body = page.getBody() + str;
			page.setBody(body);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Looks for end tags
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if(qName.equalsIgnoreCase("mediawiki")) {
			// Done with this file.
			parser.commitPage(true);
		}
		else if(qName.equalsIgnoreCase("page")) {
			// Close previous page
			parser.commitPage(false);
			state = State.UNDEFINED;
		}
		else if(qName.equalsIgnoreCase("title")) {
			state = State.UNDEFINED;
		}
		else if(qName.equalsIgnoreCase("id")) {
			state = State.UNDEFINED;
		}
		else if(qName.equalsIgnoreCase("text")) {
			// Parse body text
			state = State.UNDEFINED;
			parser.parseBody(page); //parse the text, maybe move later to better place
		}
	}
	
	/** Returns the current state
	 * 
	 * @return State - one of the enum values for state
	 */
	public State getState() {
		return state;
	}
	
}
