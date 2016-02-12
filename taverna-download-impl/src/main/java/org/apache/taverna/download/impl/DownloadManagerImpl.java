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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.VersionInfo;
import org.apache.log4j.Logger;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;

/**
 *
 */
public class DownloadManagerImpl implements DownloadManager {

	CloseableHttpClient httpclient = HttpClients.createDefault();

	private static final int TIMEOUT = Integer.getInteger("taverna.download.timeout.seconds", 30) * 1000;

	private static final Logger logger = Logger.getLogger(DownloadManagerImpl.class);

	@Override
	public void download(URI source, Path destination) throws DownloadException {
		download(source, destination, null);
	}

	@Override
	public void download(URI source, Path destination, String digestAlgorithm) throws DownloadException {
		URI digestSource = null;
		if (digestAlgorithm != null) {
			// Note: Will break with ?download=file.xml kind of URLs
			digestSource = source.resolve(source.getPath() + mapAlgorithmToFileExtension(digestAlgorithm));
		}
		download(source, destination, digestAlgorithm, digestSource);
	}

	public String getUserAgent() {
		Package pack = getClass().getPackage();
		String httpClientVersion = VersionInfo.getUserAgent("Apache-HttpClient", "org.apache.http.client",
				Request.class);
		return "Apache-Taverna-OSGi" + "/" + pack.getImplementationVersion() + " (" + httpClientVersion + ")";
	}

	@Override
	public void download(URI source, Path destination, String digestAlgorithm, URI digestSource)
			throws DownloadException {

		MessageDigest md = null;
		if (digestAlgorithm != null) {
			try {
				md = MessageDigest.getInstance(digestAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalArgumentException("Unsupported digestAlgorithm: " + digestAlgorithm, e);
			}
		}

		// download the file
		Path tempFile;
		try {
			tempFile = Files.createTempFile(destination.getParent(), "." + destination.getFileName(), ".tmp");
		} catch (IOException e1) {
			// perhaps a permission problem?
			throw new DownloadException("Can't create temporary file in folder " + destination.getParent(), e1);
		}
		logger.info(String.format("Downloading %1$s to %2$s", source, tempFile));
		downloadToFile(source, tempFile);

		if (digestSource != null) {
			// download the digest file
			String expectedDigest;
			expectedDigest = downloadHash(digestSource).trim().toLowerCase(Locale.ROOT);
			// check if the digest matches
			try {
				try (InputStream s = Files.newInputStream(tempFile)) {
					DigestUtils.updateDigest(md, s);
					String actualDigest = Hex.encodeHexString(md.digest());
					if (!actualDigest.equals(expectedDigest)) {
						throw new DownloadException(
								String.format("Error downloading file: checksum mismatch (%1$s != %2$s)",
										actualDigest, expectedDigest));
					}
				}
			} catch (IOException e) {
				throw new DownloadException(String.format("Error checking digest for %1$s", destination), e);
			}
		}
		// All fine, move to destination
		try {
			logger.info(String.format("Copying %1$s to %2$s", tempFile, destination));
			Files.move(tempFile, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new DownloadException(String.format("Error downloading %1$s to %2$s.", source, destination), e);
		}

	}

	private String downloadHash(URI source) throws DownloadException {
		try {
			// We want to handle http/https with HTTPClient
			if (source.getScheme().equalsIgnoreCase("http") || source.getScheme().equalsIgnoreCase("https")) {
				logger.info("Downloading checksum " + source);
				return Request.Get(source).userAgent(getUserAgent()).connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute()
						.returnContent().asString(StandardCharsets.ISO_8859_1);
			} else {
				// Try as a supported Path, e.g. file: or relative path
				try {
					Path path = Paths.get(source);
					return Files.readAllLines(path, StandardCharsets.ISO_8859_1).get(0);				
				} catch (FileSystemNotFoundException e) {
					throw new DownloadException("Unsupported URL scheme: " + source.getScheme());
				}
 			}
		} catch (IOException e) {
			throw new DownloadException(String.format("Error downloading %1$s", source), e);
		}		
	}
	
	private void downloadToFile(URI source, Path destination) throws DownloadException {
		try {
			// We want to handle http/https with HTTPClient
			if (source.getScheme().equalsIgnoreCase("http") || source.getScheme().equalsIgnoreCase("https")) {
				Request.Get(source).userAgent(getUserAgent()).connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute()
						.saveContent(destination.toFile());
			} else {
				// Try as a supported Path, e.g. file: or relative path
				try {
					Path path = Paths.get(source);
					Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
				} catch (FileSystemNotFoundException e) {
					throw new DownloadException("Unsupported URL scheme: " + source.getScheme());
				}
			}
		} catch (IOException e) {
			throw new DownloadException(String.format("Error downloading %1$s to %2$s.", source, destination), e);
		}
	}

	private String mapAlgorithmToFileExtension(String algorithm) {
		return "." + algorithm.toLowerCase().replaceAll("-", "");
	}

}
