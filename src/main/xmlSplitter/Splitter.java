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
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Splits a wikipedia xml file into multiple small files for easier processing.
 * Splits on the <page> tag.
 * 
 * Note: The namespace info at the top of the wikipedia file has to be removed
 * or the SAXParser will not work correctly! The top most bit of the file should
 * be <mediawiki> followed by the first <page> tag. This should only be an issue
 * for the first split file.
 * 
 * Creates a log file which contains the splitting progress per file and which
 * is used for resuming in case of problems or failure.
 * 
 * @author Jessica Anderson
 * 
 */
public class Splitter {

    private static String logOutputName;
    private long pageCount;
    private long fileCount;

    private static int pagesPerBlock;

    private BufferedReader reader;
    private static String fileName;
    private static String baseOutputName;
    private static String newline;
    private static boolean needResume;
    private static Logger logger = Logger.getLogger(Splitter.class);

    /**
     * Splitter splitter;
     * 
     * if(needResume) { logger.info("Splitter needs to be resumed - resuming");
     * try { Resumer resumer = new Resumer(fileName,outputName,pagesPerBlock);
     * splitter = new Splitter(resumer);
     * 
     * } catch(FileNotFoundException f) { logger.warn(
     * "Could not find log file containing last split point, starting from the beginning"
     * , f); splitter = new Splitter(); }
     * 
     * splitter.split();
     * 
     * }
     * 
     * else { logger.info("Starting splitting"); splitter = new Splitter();
     * splitter.split(); }
     * 
     * }
     */
    /**
     * Default Constructor.
     */
    public Splitter() {

	readInProperties();
	if (needResume) {
	    try {
		Resumer resumer = new Resumer(fileName, logOutputName,
			pagesPerBlock);
		resume(resumer);
	    } catch (FileNotFoundException e) {
		logger.error("File could not be found", e);
	    }

	} else {
	    pageCount = 0;
	    fileCount = 0;

	    newline = System.getProperty("line.separator");

	    try {
		reader = new BufferedReader(new InputStreamReader(
			new DataInputStream(new FileInputStream(fileName))));

	    } catch (FileNotFoundException e) {
		logger.error("Could not find the source xml file for parsing",
			e);
		logger.info("Can not continue without file - Exiting");
		System.exit(1);
	    }

	}

    }

    /**
     * Constructor for resuming. Appropriately calls the Resumer to find current
     * spot.
     * 
     * @param resumer
     *            a Resumer instance
     */
    public void resume(Resumer resumer) {

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

    /**
     * Splits the original xml file into multiple files.
     * 
     * Note: Creates a log file locally to be used in case of resuming from
     * crashing or other failures.
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
	    // create new file name and create file itself
	    currentFile = baseOutputName + fileCount + ".xml";
	    fileOut = new FileOutputStream(new File(currentFile));
	    pageRecordOut = new FileOutputStream(new File(logOutputName));

	    if (needResume) {
		fileOut.write(mediaWikiOpen.getBytes(charSet)); // need to add
								// <mediawiki>
								// opening tag
	    }

	    while ((currentLine = reader.readLine()) != null) {
		currentLine.trim();
		currentLine += newline;
		fileOut.write(currentLine.getBytes(charSet));

		if (currentLine.contains("</page>")) {
		    ++pageCount;

		    // are we at the wished page limit per file?
		    if ((pageCount % pagesPerBlock) == 0) {

			++fileCount;
			// time to split - create closing <mediawiki> tag and
			// shut file
			if (fileOut != null) {
			    fileOut.write(mediaWikiClose.getBytes(charSet));
			    fileOut.close();
			}

			// update log file with number of completed pages,
			// including timestamp
			Date now = new Date();
			now.setTime(System.currentTimeMillis());

			String outputRecord = pageCount + " " + now.toString()
				+ newline;
			byte[] wtfIsThis = outputRecord.getBytes(charSet);
			pageRecordOut.write(wtfIsThis);

			// create new file now
			currentFile = baseOutputName + fileCount + ".xml";
			fileOut = new FileOutputStream(new File(currentFile));

			fileOut.write(mediaWikiOpen.getBytes(charSet)); // closing
									// tag
		    }
		}
	    }

	    ++fileCount;

	    if (pageRecordOut != null) {
		pageRecordOut.close();
	    }
	    if (fileOut != null) { // handles special case of the very last file
		fileOut.write(mediaWikiClose.getBytes(charSet));
		fileOut.close();
	    }

	} catch (IOException e) {
	    logger.warn("Encountered an IO error while splitting file number "
		    + fileCount, e);
	    if (pageRecordOut != null) {
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

    private void readInProperties() {
	Properties properties = new Properties();
	try {
	    properties.load(new FileInputStream("/Users/Jessica/workspace/wikipedianerdata/splitter.properties"));

	    fileName = properties.getProperty("fileName");
	    baseOutputName = properties.getProperty("baseOutputName");
	    logOutputName = properties.getProperty("logOutputName");
	    pagesPerBlock = Integer.parseInt(properties
		    .getProperty("pagesPerBlock"));

	    if (properties.getProperty("needResume").equals("true")) {
		needResume = true;
	    } else {
		needResume = false;
	    }

	} catch (IOException e) {
	    logger.error("Something went horribly wrong", e);
	}
    }
}
