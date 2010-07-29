// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
package trainingDataGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Page;

public class GenUtil {

	public static final String CHARSET = "UTF-8";
	private Pattern entityPattern = null;
	private Matcher entityMatcher = null;
	private Pattern punctuationPattern = null;
	private Matcher punctuationMatcher = null;
	private int startIndex = 0;
	private int endIndex = 0;
	private String Wmin2;
	private String Wmin1;
	private String W;
	private String Wplus1;
	private String Wplus2;
	private String entry;
	int IdxMin2;
	int IdxMin1;
	int IdxW;
	int IdxPlus1;
	int IdxPlus2;
	
	int includeMins;
	int includePlus;
	
	public GenUtil() {
		entityPattern = Pattern.compile("@@@(.*?),(.*?)@@@");
		punctuationPattern = Pattern.compile(".,:|\\/~`;!@#$%^&*(){}[]-_=+'\"<>?");
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is,CHARSET));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line + "\n");
	    }
	    is.close();
	    return sb.toString();
	}
	
	public void processPage(Page page, 
			BufferedWriter out, 
			BufferedWriter cfout, 
			BufferedWriter goutPerson,
			BufferedWriter goutOrg) {
		boolean written;
		entityMatcher = entityPattern.matcher(page.getBody());
		
		while(entityMatcher.find()){
			startIndex = entityMatcher.start();
			endIndex = entityMatcher.end();
			if(endIndex >= page.getBody().length()) {
				break;
			}
			IdxW = startIndex + "@@@".length();
			IdxMin1 = page.getBody().lastIndexOf(' ', startIndex-1);
			IdxMin2 = page.getBody().lastIndexOf(' ', IdxMin1-1);
			IdxPlus1 = page.getBody().indexOf(' ', endIndex+1);
			IdxPlus2 = page.getBody().indexOf(' ', IdxPlus1+1);
			if((IdxPlus1 < 0) || (IdxPlus2 < 0)) {
				break;
			}
			Wmin2 = page.getBody().substring(IdxMin2+1, IdxMin1-1);
			Wmin1 = page.getBody().substring(IdxMin1+1,startIndex-1);
			//W = entityMatcher.group(1);
			Wplus1 = page.getBody().substring(endIndex+1, IdxPlus1-1);
			Wplus2 = page.getBody().substring(IdxPlus1+1,IdxPlus2-1);

			if(punctuationPattern.matcher(Wmin1).find()) {
				includeMins = 0;
			}
			else if(punctuationPattern.matcher(Wmin2).find()) {
				includeMins = 1;
			}

			if(punctuationPattern.matcher(Wplus1).find()) {
				includePlus = 0;
			}
			else if(punctuationPattern.matcher(Wplus2).find()) {
				includePlus = 1;
			}

			try {
				if(entityMatcher.group(2).equals("PER")) {
					goutPerson.write(entityMatcher.group(1)+"\n");
				}
				else if(entityMatcher.group(2).equals("ORG")) {
					goutOrg.write(entityMatcher.group(1)+"\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			written = false;
			if(includeMins == 0 && includePlus == 0) {
				// Zet in apart bestand
				try {
					cfout.write(entityMatcher.group(2)+"|"+
								entityMatcher.group(1)+"|"+
								"|||\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				written = true;
			}
			else if(includeMins == 1 && includePlus == 0) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						"|"+							// W-2
						Wmin1 + "|"+					// W-1
						"|"+							// W+1
						"\n";							// W+2
			}
			else if(includeMins == 2 && includePlus == 0) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						Wmin2 + "|"+					// W-2
						Wmin1 + "|"+					// W-1
						"|"+							// W+1
						"\n";							// W+2
			}
			else if(includeMins == 0 && includePlus == 1) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						"|"+							// W-2
						"|"+							// W-1
						Wplus1 + "|"+					// W+1
						"\n";							// W+2				
			}
			else if(includeMins == 1 && includePlus == 1) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						"|"+							// W-2
						Wmin1 + "|"+					// W-1
						Wplus1 + "|"+					// W+1
						"\n";							// W+2		
			}
			else if(includeMins == 2 && includePlus == 1) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						Wmin2 + "|"+					// W-2
						Wmin1 + "|"+					// W-1
						Wplus1 + "|"+					// W+1
						"\n";							// W+2		
			}
			else if(includeMins == 0 && includePlus == 2) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						"|"+							// W-2
						"|"+							// W-1
						Wplus1 + "|"+					// W+1
						Wplus2 + "\n";					// W+2	
			}
			else if(includeMins == 1 && includePlus == 2) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						"|"+							// W-2
						Wmin1 + "|"+					// W-1
						Wplus1 + "|"+					// W+1
						Wplus2 + "\n";					// W+2	
			}
			else if(includeMins == 2 && includePlus == 2) {
				entry = entityMatcher.group(2)+"|"+		// CLASS
						entityMatcher.group(1)+"|"+		// entity
						Wmin2 + "|"+					// W-2
						Wmin1 + "|"+					// W-1
						Wplus1 + "|"+					// W+1
						Wplus2 + "\n";					// W+2	
			}
			if(!written) {
				try {
					out.write(entry);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
