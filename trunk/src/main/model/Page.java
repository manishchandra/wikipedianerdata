// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 16, 2010
 * 
 */
package model;

import java.util.Set;
import com.google.common.collect.*;

/** A wikipedia page, including extra information about the page itself.
 * 
 * Note: This class uses setter injection in place of constructor because of the way information
 * is incrementally added as the SaxParser proceeds.
 * 
 * @author Jessica Anderson
 */
public class Page {
	private String title;
	private long id;
	private String body;
	private Set<String> categories;
	private String englishLink;
	private String entityType; //TODO: convert to Enum!
	private boolean isDisambiguation;
	
	public Page() {
		categories = Sets.newLinkedHashSet();
		isDisambiguation = false;
	}
	
	@Override
	public String toString() {
		String outString = "Page: " 
		+ title + "\nid = " 
		+ id + "\n"; 
		
		for(String s : categories) {
			outString += "Category: "+s+"\n";
		}
		return outString;
	}

	/** Hash code */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/** Equals method. Pages must be guaranteed to have a unique ID. 
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Page other = (Page) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public String getEnglishLink() {
		return englishLink;
	}

	public void setEnglishLink(String englishLink) {
		this.englishLink = englishLink;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public boolean isDisambiguation() {
		return isDisambiguation;
	}

	public void setDisambiguation(boolean isDisambiguation) {
		this.isDisambiguation = isDisambiguation;
	}
	


}
