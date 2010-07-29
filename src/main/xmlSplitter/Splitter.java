// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 9, 2010
 */
package xmlSplitter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import org.apache.log4j.*;

/**Splits a wikipedia xml file into multiple small files for easier processing. Splits on the
 * <page> tag.
 * 
 * Note: The namespace info at the top of the wikipedia file has to be removed or the 
 * SAXParser will not work correctly! The top most bit of the file should be <mediawiki> 
 * followed by the first <page> tag. This should only be an issue for the first split file.
 * 
 *  Creates a log file which contains the splitting progress per file and which is used for resuming in case
 *  of problems or failure.
 * 
 * @author Jessica Anderson
 *  
 */
public class Splitter {

	private long pageCount;
	private long fileCount;
	
	private static int pagesPerBlock;
	
	private BufferedReader reader;
	private static String fileName;
	private static String baseName;
	private static String outputName;
	private static String newline;
	private static boolean needResume;
	private static Logger logger = Logger.getLogger(Splitter.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//constants for my base system
		fileName = "C:\\eclipse\\workspace\\enwiki-20100312-pages-articles.xml";
		baseName = "C:\\eclipse\\workspace\\XML_fail\\NEW_";
		outputName = "C:\\eclipse\\workspace\\SplitEnglishOut.txt";
		pagesPerBlock = 2;
		needResume = false;
		PropertyConfigurator.configure("log4j.properties");
		
		
		for(int i = 0; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-f")) {
				fileName = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-b")) {
				baseName = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-o")) {
				outputName = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-r")) {
				needResume = true;
			}
			if (args[i].equalsIgnoreCase("-n")) {
				pagesPerBlock = Integer.parseInt(args[i+1]);
			}
		}
		
		Splitter splitter;
		
		if(needResume) {
			logger.info("Splitter needs to be resumed - resuming");
			try {
				Resumer resumer = new Resumer(fileName,outputName,pagesPerBlock);
				splitter = new Splitter(resumer);
				
			} catch(FileNotFoundException f) {
				logger.warn("Could not find log file containing last split point, starting from the beginning", f);
				splitter = new Splitter();
			}
			
			splitter.split();
				
		}
			
		else {
			logger.info("Starting splitting");
			splitter = new Splitter();
			splitter.split();
		}
		
	}

	/**
	 * Default Constructor.
	 */
	public Splitter() {
		
		pageCount = 0; 
		fileCount = 0;
		
		newline = System.getProperty("line.separator");
		
		try {
			reader = new BufferedReader(
					new InputStreamReader(
							new DataInputStream(
									new FileInputStream(fileName))));
			
		} catch (FileNotFoundException e) {
			logger.error("Could not find the source xml file for parsing", e);
			logger.info("Can not continue without file - Exiting");
			System.exit(1);
		}
		
	}

	/** Constructor for resuming. Appropriately calls the Resumer to find current
	 * spot. 
	 * 
	 * @param resumer a Resumer instance
	 */
	public Splitter(Resumer resumer) {

		newline = System.getProperty("line.separator");
		
		try {
			reader = resumer.resume();
		} catch (FileNotFoundException e) {
			logger.error("Could not find the source xml file for parsing", e);
			logger.info("Can not continue without file - Exiting");
			System.exit(1);
		}
		fileCount = resumer.getFileCount();
		pageCount = resumer.getPageCount();	
		
	}
	
	/** Splits the original xml file into multiple files.
	 * 
	 * Note: Creates a log file locally to be used in case of resuming from crashing or
	 * other failures. 
	 * 
	 */
	public void split() {
		String currentLine = "";
		String currentFile = "";
		FileOutputStream fileOut = null;
		FileOutputStream pageRecordOut = null;
		
		final String mediaWikiOpen = "<mediawiki>" + newline;
		final String mediaWikiClose = "</mediawiki>" + newline;
		final String charSet = "UTF-8";
		
		
		try {
			logger.info("Creating new file");
			//create new file name and create file itself
			currentFile = baseName + fileCount + ".xml";					
			fileOut = new FileOutputStream(new File(currentFile));
			pageRecordOut = new FileOutputStream(new File(outputName));
			
			if(needResume) {
				fileOut.write(mediaWikiOpen.getBytes(charSet)); //need to add <mediawiki> opening tag
			}
			
			while ((currentLine = reader.readLine()) != null ) {
				currentLine.trim();
				currentLine += newline;
				fileOut.write(currentLine.getBytes(charSet));
				
				if (currentLine.contains("</page>")) {
					++pageCount;
					
					//are we at the wished page limit per file?
					if((pageCount % pagesPerBlock) == 0) {
						
						++fileCount;
						//time to split - create closing <mediawiki> tag and shut file
						if(fileOut != null){
							fileOut.write(mediaWikiClose.getBytes(charSet));
							fileOut.close();
						}
						
						//update log file with number of completed pages, including timestamp
						Date now = new Date();
						now.setTime(System.currentTimeMillis());
						
						String outputRecord = pageCount + " " + now.toString() + newline;
						byte [] wtfIsThis = outputRecord.getBytes(charSet);
						pageRecordOut.write(wtfIsThis);
						
						//create new file now
						currentFile = baseName + fileCount + ".xml";					
						fileOut = new FileOutputStream(new File(currentFile));

						fileOut.write(mediaWikiOpen.getBytes(charSet)); //closing tag
					}
				}
			}
			
			++fileCount;
			
			if(pageRecordOut != null) {
				pageRecordOut.close();
			}
			if(fileOut != null) { //handles special case of the very last file
				fileOut.write(mediaWikiClose.getBytes(charSet));
				fileOut.close();
			}
			
		} catch (IOException e) {
			logger.warn("Encountered an IO error while splitting file number " + fileCount, e);
			if(pageRecordOut != null) {
				try {
					pageRecordOut.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
	}
}
