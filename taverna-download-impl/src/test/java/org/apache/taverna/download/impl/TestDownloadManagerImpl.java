package org.apache.taverna.download.impl;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.taverna.download.DownloadException;
import org.junit.Ignore;
import org.junit.Test;

public class TestDownloadManagerImpl {
	
	/**
	 * This test should remain @Ignored  
	 * as it relies on a web site
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
		URI wrongURL = URI.create("http://192.185.115.65/~diana/DIANA_plugin_updated/test-plugins/gr/dianatools/diana.services-activity/1.0-SNAPSHOT/diana.services-activity-1.0-SNAPSHOT.pom");
		
		// With this we get an exception - but only because the 
		// 300 erorr page fails the MD5 check
		//dl.download(wrongURL, pomFile.toFile(), "MD5");
		// so we'll try without hashsum
		dl.download(wrongURL, pomFile);
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
		Path example = Files.createTempFile("test", ".txt");
		Files.write(example, "Hello world".getBytes(US_ASCII)); // No newline
		Path exampleMd5 = example.resolveSibling(example.getFileName() + ".md5");
//		stain@biggie:~$ echo -n "Hello world"|md5sum
//		3e25960a79dbc69b674cd4ec67a72c62  -
		Files.write(exampleMd5, "3e25960a79dbc69b674cd4ec67a72c62".getBytes(US_ASCII)); // no newline
		
		DownloadManagerImpl dl = new DownloadManagerImpl();
		
		Path toFile = Files.createTempFile("downloaded", ".txt");
		
		dl.download(example.toUri(), toFile, "MD5");
		String hello = Files.readAllLines(toFile, US_ASCII).get(0);
		
		assertEquals("Hello world", hello);		
	}

	/**
	 * This test should remain @Ignored  
	 * as it relies on a web site
	 * and should not break the build.
	 * 
	 */
	@Ignore
	@Test
	public void downloadPomSha1() throws Exception {
		Path destination = Files.createTempFile("test", "pom");
		URI source = URI.create("https://repo.maven.apache.org/maven2/org/apache/apache/17/apache-17.pom");
		DownloadManagerImpl dl = new DownloadManagerImpl();
		dl.download(source, destination, "SHA-1");
	}
	
}
