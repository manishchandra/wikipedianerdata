// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
package xmlSplitter;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.*;
import org.mockito.*;

public class ResumerTest {
	
	private Resumer resume;
	
	
	@Test(expected = FileNotFoundException.class)
	public void testConstructor() throws FileNotFoundException {
		resume = new Resumer("Scriptie/test/xmlSplitter/TestXMLWith4Pages.xml", "tst", 4);
	}
	
	/**
	 * Testing with valid resume file, valid xml file, and with exact matching
	 * number of pages as blocks, in order to test "basic" flow.
	 * @throws IOException 
	 */
	@Test
	public void testResumeNormal() throws IOException {
		resume = new Resumer("C:\\eclipse\\workspace\\TestResumer.xml", 
				"C:\\eclipse\\workspace\\SplitEnglishOut2.txt", 1);
		
		BufferedReader reader = resume.resume();
		
		assertEquals(1, resume.getPageCount());
		assertEquals(1, resume.getFileCount());
		assertEquals(1, resume.getPagesPerFileCount());
		
		String currentLine = reader.readLine();
		assertTrue(currentLine.contains("<page>"));
		
		currentLine = reader.readLine();
		assertTrue(currentLine.contains("<title> hey d00d 2</title>"));
	}
	
	/**
	 * Testing with valid resume file and valid xml file. Says that 3 pages were processed
	 * but block size 4, in order to test that the original block is then redone.
	 * @throws IOException 
	 */
	@Test
	public void testResumeWithUnmatchedPageBlock() throws IOException {
		resume = new Resumer("C:\\eclipse\\workspace\\TestResumer.xml", 
				"C:\\eclipse\\workspace\\SplitEnglishOutOdd.txt", 4);
		
		BufferedReader reader = resume.resume();
		
		assertEquals(3, resume.getPageCount());
		assertEquals(0, resume.getFileCount());
		assertEquals(4, resume.getPagesPerFileCount());
		
		String currentLine = reader.readLine();
		assertTrue(currentLine.contains("<page>"));
		
		currentLine = reader.readLine();
		assertTrue(currentLine.contains("<title> hey d00d 4 </title>"));
	}
	
	@Test
	public void testGetContents() throws FileNotFoundException {
		resume = new Resumer("C:\\eclipse\\workspace\\TestResumer.xml", 
				"C:\\eclipse\\workspace\\SplitEnglishOutOdd.txt", 4);
		
		assertEquals(3, resume.getPageCount());
	
	}

}
