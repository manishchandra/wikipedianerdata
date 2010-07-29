// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: April 20, 2010
 * 
 */
package entityExtractor;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import wikipediaparser.CategoryParser;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import dao.Dao;

/**
 * 
 * @author Jessica Anderson
 *
 */
public class EntityExtractionManager {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final int NR_THREADS = 8;

		ConcurrentMap<String,String> cachedPageTypes = new MapMaker()
		.concurrencyLevel(NR_THREADS)
		//.softKeys()
		//.softValues()
		.initialCapacity(1000)
		.makeMap();
		//Maps.newHashMapWithExpectedSize(5000000);
		
		Map<String,String> categoryMap = Maps.newHashMap();
		CategoryParser parser = new CategoryParser("C:\\eclipse\\workspace\\Scriptie\\categoryClasses.txt");
		categoryMap = parser.parse();

		EntityExtractionThread eet[] = new EntityExtractionThread[NR_THREADS];

		for(int i = 0; i < NR_THREADS; ++i) {
			eet[i] = new EntityExtractionThread(categoryMap, i, NR_THREADS, cachedPageTypes);
			eet[i].start();
		}		
	}
}

/** Creates a new instance (new thread), and it's own version of EntityUtil, Dao, and ExtractEntities. 
 * This keeps everything atomic and allows for faster database processing (multiple connections). 
 *
 */
class EntityExtractionThread extends Thread {
	
	Map<String,String> categoryMap;
	ConcurrentMap<String,String> cachedPageTypes;
	int threadNr;
	int numberOfThreads;
	
	public EntityExtractionThread(Map<String,String> categoryMap, int threadNr, int numberOfThreads, ConcurrentMap<String,String> cachedPageTypes) {
		this.categoryMap = categoryMap;
		this.threadNr = threadNr;
		this.numberOfThreads = numberOfThreads;
		this.cachedPageTypes = cachedPageTypes;
	}
	
	/** Starts a new instance running
	 * 
	 */
	public void run() {
		EntityUtil util = new EntityUtil(new Dao(), categoryMap);

		ExtractEntities entity = new ExtractEntities(util,3000,numberOfThreads,cachedPageTypes);
		entity.executeFirstPass(threadNr);
	}
	
}
