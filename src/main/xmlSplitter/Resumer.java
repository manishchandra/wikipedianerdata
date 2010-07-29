// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 10, 2010
 */
package xmlSplitter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import org.apache.log4j.*;

/**
 * Allows splitting to be resumed from mid-point in case of error or otherwise. If half a file
 * has been written , it starts from the beginning point of the last fully written file and
 * will rewrite the half-written file.
 * 
 * @author Jessica Anderson
 */
public class Resumer {

	private final String xmlFile; //original xml file
	private final String resumeRecordFile; //file used to keep track of last split point
	private long contents; //page count from file, for number of pages successfully split
	private long fileCount; //number of files created through splitting
	private final long pagesPerBlock; //how many pages to encode per file/block
	private Logger logger = Logger.getLogger(Resumer.class);
	
	/**Constructor
	 * 
	 * @param XMLFile source file
	 * @param ResumeRecordFile file containing last split point
	 * @param pagesPerBlock how many pages per file/block. Should be same as original splitter.
	 */
	public Resumer(String xmlFile, String resumeRecordFile, long pagesPerBlock) throws FileNotFoundException {
		assert xmlFile != null;
		assert resumeRecordFile != null;
		assert pagesPerBlock > 0;
		
		this.xmlFile= xmlFile.trim();
		this.resumeRecordFile = resumeRecordFile.trim();
		this.pagesPerBlock = pagesPerBlock;
		getContents();
		
		
	}
	
	/** Finds the last place that was split in the file to be split
	 * 
	 * @return Buffered Reader set to last split point. 
	 */
	public BufferedReader resume() throws FileNotFoundException {
		BufferedReader reader = null;
		String currentLine;
		long pageCount = 0;
		
		reader = new BufferedReader(
					new InputStreamReader(
							new DataInputStream(
									new FileInputStream(xmlFile))));
	
		try {
			while(pageCount < contents && (currentLine = reader.readLine()) != null ) {
				if (currentLine.contains("</page>")) {
					++pageCount;

					if( pageCount % pagesPerBlock == 0) {
						++fileCount;
					}
				}
			}
		} catch (IOException e) {
			logger.warn("IoException while reading line in from XML source file", e);
		}
			
		return reader;
		
	}
	

	/** Grab the page count from the ResumeRecordFile
	 * 
	 * @return the page count
	 */
	private void getContents() throws FileNotFoundException {
		File xml = new File(resumeRecordFile);
		Scanner scanner = null;
		scanner = new Scanner(xml);
		contents = scanner.nextLong();
		
		scanner.close();
	}
	
	/** Gets the page count */
	public long getPageCount() {
		return contents;
	}
	
	
	/** Gets the file count Note: Will be 0 unless resume() has executed*/
	public long getFileCount() {
		return fileCount;
	}
	
	/** Gets the pages per file count */
	public long getPagesPerFileCount() {
		return pagesPerBlock;
	}
	
	/** Gets the pages per file count */
	public String getResumeRecordFile() {
		return resumeRecordFile;
	}
	
	/** Gets the pages per file count */
	public String getxmlFile() {
		return xmlFile;
	}
	
}
