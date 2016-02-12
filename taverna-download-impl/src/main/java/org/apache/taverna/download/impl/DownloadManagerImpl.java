/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.taverna.download.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;

/**
 *
 */
public class DownloadManagerImpl implements DownloadManager {

	private static final int TIMEOUT = Integer.getInteger("taverna.download.timeout.seconds", 30) * 1000;
	
	private static final Logger logger = Logger.getLogger(DownloadManagerImpl.class);

	public void download(URL source, File destination) throws DownloadException {
		download(source, destination, null);
	}

	public void download(URL source, File destination, String digestAlgorithm) throws DownloadException {
		// TODO Use MessageDigest when Java 7 available
		if (digestAlgorithm != null && !digestAlgorithm.equals("MD5")) {
			throw new IllegalArgumentException("Only MD5 supported");
		}
		URL digestSource = null;
		if (digestAlgorithm != null) {
			try {
				digestSource = new URL(source.toString() + mapAlgorithmToFileExtension(digestAlgorithm));
			} catch (MalformedURLException e) {
				throw new DownloadException("Error creating digest URL", e);
			}
		}
		download(source, destination, digestAlgorithm, digestSource);
	}

	public void download(URL source, File destination, String digestAlgorithm, URL digestSource)
			throws DownloadException {
		// TODO Use MessageDigest when Java 7 available
		if (digestAlgorithm != null && !digestAlgorithm.equals("MD5")) {
			throw new IllegalArgumentException("Only MD5 supported");
		}
		// download the file
		File tempFile;
		try {
			tempFile = File.createTempFile("DownloadManager", "tmp");
			tempFile.deleteOnExit();
			logger.info(String.format("Downloading %1$s to %2$s", source, tempFile));
			FileUtils.copyURLToFile(source, tempFile, TIMEOUT, TIMEOUT);
		} catch (IOException e) {
			throw new DownloadException(String.format("Error downloading %1$s to %2$s.", source, destination), e);
		}
		if (digestSource != null) {
			// download the digest file
			File digestFile;
			try {
				digestFile = File.createTempFile("DownloadManager", "tmp");
				digestFile.deleteOnExit();
				logger.info(String.format("Downloading %1$s to %2$s", digestSource, digestFile));
				FileUtils.copyURLToFile(digestSource, digestFile, TIMEOUT, TIMEOUT);
			} catch (IOException e) {
				throw new DownloadException(String.format("Error checking digest for %1$s.", source), e);
			}
			// check the digest matches
			try {
				String digestString1 = DigestUtils.md5Hex(new FileInputStream(tempFile));
				String digestString2 = FileUtils.readFileToString(digestFile);
				if (!digestString1.equals(digestString2)) {
					throw new DownloadException(String.format(
							"Error downloading file: digsests not equal. (%1$s != %2$s)",
							digestString1, digestString2));
				}
			} catch (IOException e) {
				throw new DownloadException(String.format("Error checking digest for %1$s", destination),
						e);
			}
		}
		// copy file to destination
		try {
			logger.info(String.format("Copying %1$s to %2$s", tempFile, destination));
			FileUtils.copyFile(tempFile, destination);
		} catch (IOException e) {
			throw new DownloadException(String.format("Error downloading %1$s to %2$s.", source, destination), e);
		}

	}

	private String mapAlgorithmToFileExtension(String algorithm) {
		return "." + algorithm.toLowerCase().replaceAll("-", "");
	}

}
