package org.apache.taverna.download.impl;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.taverna.download.DownloadException;
import org.junit.Ignore;
import org.junit.Test;

public class TestDownloadManagerImpl {
	
	/**
	 * This test should remain @Ignored  
	 * as it relies on a third-party web site
	 * and should not break the build.
	 * 
	 * Verifies TAVERNA-893
	 * 
	 */
	@Ignore
	@Test(expected=DownloadException.class)
	public void handle300MultipleChoice() throws Exception {
		DownloadManagerImpl dl = new DownloadManagerImpl();
		Path pomFile = Files.createTempFile("test", ".pom");
		// NOTE: The below URL is a Taverna 2 POM - not related to 
		// taverna-plugin-impl
		URL wrongURL = new URL("http://192.185.115.65/~diana/DIANA_plugin_updated/test-plugins/gr/dianatools/diana.services-activity/1.0-SNAPSHOT/diana.services-activity-1.0-SNAPSHOT.pom");
		
		// With this we get an exception - but only because the 
		// 300 erorr page fails the MD5 check
		//dl.download(wrongURL, pomFile.toFile(), "MD5");
		// so we'll try without hashsum
		dl.download(wrongURL, pomFile.toFile());
	}
	
	
	/**
	 * Test downloading a file:/// to a File, checking md5 hashsum.
	 * 
	 * This might seem silly, but avoids spinning up a Jetty instance
	 * just for this unit test.
	 * 
	 */
	@Test
	public void downloadLocalFile() throws Exception {
		Path example = Files.createTempFile("example", ".txt");
		Files.write(example, "Hello world".getBytes(US_ASCII)); // No newline
		Path exampleMd5 = example.resolveSibling(example.getFileName() + ".md5");
//		stain@biggie:~$ echo -n "Hello world"|md5sum
//		3e25960a79dbc69b674cd4ec67a72c62  -
		Files.write(exampleMd5, "3e25960a79dbc69b674cd4ec67a72c62".getBytes(US_ASCII)); // no newline
		
		DownloadManagerImpl dl = new DownloadManagerImpl();
		
		Path toFile = Files.createTempFile("downloaded", ".txt");
		
		dl.download(example.toUri().toURL(), toFile.toFile(), "MD5");
		String hello = Files.readAllLines(toFile, US_ASCII).get(0);
		
		assertEquals("Hello world", hello);
		
	}
	
}
