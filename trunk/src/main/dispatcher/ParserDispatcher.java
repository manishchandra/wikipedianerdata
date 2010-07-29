// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: February 18, 2010
 */
package dispatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import model.Language;

/** Creates (sequentially) multiple java processes, specifically for the Wikipedia Parser. It executes
 * the process and keeps a timer, terminating the process if it takes longer than 50 minutes. 
 * 
 * @author Jessica Anderson
 *
 */
public class ParserDispatcher {

	private Process currentParseProcess;
	private ProcessBuilder builder;
	
	private static String directory;
	private static long commitInterval;
	private static Language language;
	private String filename;
	private static Logger logger = Logger.getLogger(ParserDispatcher.class);
	private FileWriter fstream;
	private BufferedWriter out;
	private FileWriter fstreamOutofTime;
	private BufferedWriter outofTime;
	private Date now;

	public static void main(String[] args) {

		PropertyConfigurator.configure("log4j.properties");
		directory = "C:\\eclipse\\workspace\\XML_EN";
		commitInterval = 100;
		language = Language.ENGLISH;
		
		for(int i = 0; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-d")) {
				directory = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-c")) {
				commitInterval = Long.parseLong(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-l")) {
				String lang = (args[i+1]);
				language = Language.valueOf(lang.toUpperCase());
			}
		}
		
		try {
			ParserDispatcher dispatcher = new ParserDispatcher();
			dispatcher.run();
		} catch(IOException i) {
			logger.error("A log file could not be created", i);
		}
	}

	public ParserDispatcher() throws IOException {

			fstream = new FileWriter("C:\\eclipse\\workspace\\Scriptie\\DispatcherOut.txt",true);
			out = new BufferedWriter(fstream);
			fstreamOutofTime = new FileWriter("C:\\eclipse\\workspace\\Scriptie\\DispatcherOutOfTime.txt",true);
			outofTime = new BufferedWriter(fstreamOutofTime);
			now = new Date();
	}
	
	/** Runs the dispatcher for a given set of files
	 * 
	 */
	public void run() throws IOException {
		final File dir = new File(directory);
		
		String[] children = dir.list();
		if (children == null) {
		   logger.info("No files to process - exiting");
		   return;
		}
		
		for (int i=0; i<children.length; i++) {
	        // Get filename of file or directory
	        filename = children[i];

	        if(filename.endsWith(".xml")) {
	        	builder = new ProcessBuilder("C:\\Program Files\\Java\\jre6\\bin\\javaw.exe", "-Xmx1024M", "-Dfile.encoding=Cp1252", "-classpath", "C:\\eclipse\\workspace\\Scriptie\\bin;C:\\Java\\apache-log4j-1.2.15\\log4j-1.2.15.jar;C:\\Java\\google-collect-1.0\\google-collect-1.0.jar;C:\\Java\\mysql-connector-java-5.1.12\\mysql-connector-java-5.1.12-bin.jar", "wikipediaparser.Parser",
	        			"-f", directory+filename,"-c", commitInterval+"", "-l",language.name());
	        	builder.redirectErrorStream(true);
	        	//builder = new ProcessBuilder("java", "Parser", "-f", directory+filename,"-c", commitInterval+"", "-l",language);
	        	DoIt();
	        }
	    }
		out.close();
		outofTime.close();
	}
	
	/** Starts a process, times in, and destroys if it takes too long. Also handles a general log file
	 * for all files completed, as well as an OutofTime log which says if a process was destroyed due 
	 * to taking too long to complete. 
	 * 
	 */
	private void DoIt() throws IOException {
		//First write new entry to log
		now.setTime(System.currentTimeMillis());
		out.write("Processing file: " + filename + " timestamp: "+now.toString());
	    
		try {
			// Start timer
			Timer destroyProcess = new Timer();
			BufferedReader reader;
			String line;
			currentParseProcess = builder.start();

			destroyProcess.schedule(new TimerTask(){
							public void run(){
								try{
								    now = new Date();
									now.setTime(System.currentTimeMillis());
									outofTime.write("File: "+filename+" ran out of time before finishing. Timestamp: "+now.toString());
								    
							    }catch (IOException e){
							    	logger.warn("Occured an exception while writing to OutofTime logfile", e);
							    }
							    logger.info("Process took too long, ending process");
								currentParseProcess.destroy();
							}
			},3000000);
			
			//allow output from running process, otherwise output blocks program from running.
			reader = new BufferedReader(new InputStreamReader(currentParseProcess.getInputStream()));
			while ((line = reader.readLine()) != null) {
			      System.out.println(line);
			}
			
			currentParseProcess.waitFor();
			destroyProcess.cancel();
			
			
		} catch (IOException e) {
			logger.warn("Exception occured while reading program's output", e);
		} catch (InterruptedException e) {
			logger.error("An interrupt occured", e);
		}
	}
}