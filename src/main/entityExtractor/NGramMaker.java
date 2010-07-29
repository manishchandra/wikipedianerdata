// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 18, 2010
 * 
 */
package entityExtractor;

//import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/** Creates n grams for a given size n and a text
 * 
 * @author Jessica Anderson
 *
 */
public class NGramMaker {

	private StringTokenizer tokenizer;
	
	public NGramMaker(String text) {
		tokenizer = new StringTokenizer(text);
	}
	
	/**Makes n grams
	 * 
	 * @param n the size of the gram
	 * @return A set of n grams
	 */
	public Set<String> makeNGrams(int n) {
		/*Deque<String> nGrams = new LinkedList<String>();
		Set<String> nGramsToReturn = Sets.newLinkedHashSet();
		while(tokenizer.hasMoreTokens()) {
			nGrams.push(tokenizer.nextToken());
			  if(nGrams.size() > n) {
			    nGrams.pop();
			  }
			  String finalGram = "";
			  for(String s : nGrams) {
				  finalGram.concat(s + " ");
				 
			  }
			  finalGram.trim();
			  nGramsToReturn.add(finalGram);
			}
		return nGramsToReturn;
		*/
		return null;
	}
}
