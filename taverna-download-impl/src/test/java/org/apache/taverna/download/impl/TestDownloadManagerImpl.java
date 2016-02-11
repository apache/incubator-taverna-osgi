package org.apache.taverna.download.impl;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import static java.nio.charset.StandardCharsets.US_ASCII;

import org.apache.taverna.download.DownloadException;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestDownloadManagerImpl {
	
	/**
	 * This test should remain commented out 
	 * as it relies on a third-party 
	 * web service.
	 * 
	 * Verifies 
	 * 
	 */
	@Test(expected=DownloadException.class)
	public void handle300MultipleChoice() throws Exception {
		DownloadManagerImpl dl = new DownloadManagerImpl();
		Path pomFile = Files.createTempFile("test", ".pom");
		// NOTE: The below URL is a Taverna 2 POM - not related to 
		// taverna-plugin-impl
		URL wrongURL = new URL("http://192.185.115.65/~diana/DIANA_plugin_updated/test-plugins/gr/dianatools/diana.services-activity/1.0-SNAPSHOT/diana.services-activity-1.0-SNAPSHOT.pom");
		dl.download(wrongURL, pomFile.toFile(), "MD5");	
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
		Path exampleMd5 = example.resolve(example.getFileName() + ".md5");
//		stain@biggie:~$ echo -n "Hello world"|md5sum
//		3e25960a79dbc69b674cd4ec67a72c62  -
		Files.write(exampleMd5, "3e25960a79dbc69b674cd4ec67a72c62".getBytes(US_ASCII));
		
		DownloadManagerImpl dl = new DownloadManagerImpl();
		
		Path toFile = Files.createTempFile("downloaded", ".txt");
		
		dl.download(example.toUri().toURL(), toFile.toFile(), "MD5");
		String hello = Files.readAllLines(toFile, US_ASCII).get(0);
		
		assertEquals("Hello world", hello);
		
	}
	
}
