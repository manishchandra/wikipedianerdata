// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
package trainingDataGenerator;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;

import model.Language;
import model.Page;
import dao.Dao;

public class TrainingDataGenerator {

	static int NR_SLAVES = 1;
	static int nextData = 0;
	static int blockSize = 100;
	static int maxData = 1900000;
	static String output = "training_";
	static String cfoutput = "training_cf_";
	static String gazetteer = "gazetteer_";
	
	public TrainingDataGenerator() {
		nextData = 0;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TrainingDataGenerator gen = new TrainingDataGenerator();
		
		for(int i = 0; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-np")) {
				NR_SLAVES = Integer.parseInt(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-s")) {
				nextData = Integer.parseInt(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-n")) {
				blockSize = Integer.parseInt(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-m")) {
				maxData = Integer.parseInt(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-o")) {
				output = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-cfo")) {
				cfoutput = args[i+1];
			}
			if (args[i].equalsIgnoreCase("-g")) {
				gazetteer = args[i+1];
			}
		}
		TrainingDataGeneratorThread threads[] = new TrainingDataGeneratorThread[NR_SLAVES];
		
		for(int i = 0; i < NR_SLAVES; ++i) {
			threads[i] = new TrainingDataGeneratorThread( gen, i, NR_SLAVES, blockSize,output,cfoutput,gazetteer,maxData);
			threads[i].start();
		}

	}
	
	public synchronized int getTask() {
		nextData += blockSize;
		if(nextData > maxData) {
			return -1;
		}
		else {
			return nextData;
		}
	}

}


class TrainingDataGeneratorThread extends Thread {
		
	private int NR_SLAVES;
	private int threadNr;
	private int blockSize;
	private TrainingDataGenerator gen;
	private String contextOutput;
	private String contextFreeOutput;
	private String gazetteerOutput;
	private int maxData;
	int task;
	
	public TrainingDataGeneratorThread(
			TrainingDataGenerator gen, 
			int threadNr, 
			int numberOfThreads, 
			int blockSize,
			String contextOutput,
			String contextFreeOutput,
			String gazetteerOutput,
			int maxData) {
		this.gen = gen;
		this.threadNr = threadNr;
		this.NR_SLAVES = numberOfThreads;
		this.blockSize = blockSize;
		this.contextOutput = contextOutput;
		this.contextFreeOutput = contextFreeOutput;
		this.gazetteerOutput = gazetteerOutput;
		this.maxData = maxData;
	}
	
	/** Starts a new instance running
	 * 
	 */
	public void run() {

		ResultSet resultsPage = null;
		Page currentPage = null;
		Dao dao = new Dao();
		BufferedWriter contextOut = null;
		BufferedWriter contextFreeOut = null;
		BufferedWriter gazPerOut = null;
		BufferedWriter gazOrgOut = null;
		GenUtil util = new GenUtil();
		try {
			contextOut = new BufferedWriter(new FileWriter(contextOutput+threadNr));
			contextFreeOut = new BufferedWriter(new FileWriter(contextFreeOutput+threadNr));
			gazPerOut = new BufferedWriter(new FileWriter(gazetteerOutput+"PER_"+threadNr));
			gazOrgOut = new BufferedWriter(new FileWriter(gazetteerOutput+"ORG_"+threadNr));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		while((task = gen.getTask()) > 0) {
			for(long id = task; (id < task+blockSize) && (id < maxData); ++id) {
				resultsPage = dao.retrievePage(Language.DUTCH.abbreviation(), id);
				try {
					//first Page info
					if(resultsPage.first()){
						currentPage = new Page();
						currentPage.setId(resultsPage.getLong(1));
						currentPage.setTitle(resultsPage.getString(2));
						InputStream bodyAsStream = resultsPage.getBinaryStream(3);
						currentPage.setBody(util.convertStreamToString(bodyAsStream));
						currentPage.setEnglishLink(resultsPage.getString(4));
						currentPage.setEntityType(resultsPage.getString(5));
						currentPage.setDisambiguation(resultsPage.getBoolean(6));
						util.processPage(currentPage,contextOut,contextFreeOut,gazPerOut,gazOrgOut);
					}
					else {
						// No page with id ID? No problem - ga maar door.
					}
				}
				catch(Exception e) {
					
				}
			}
		}
		
		try {
			contextOut.close();
			contextFreeOut.close();
			gazPerOut.close();
			gazOrgOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
