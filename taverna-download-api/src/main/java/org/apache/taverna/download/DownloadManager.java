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
package org.apache.taverna.download;

import java.io.File;
import java.net.URL;

/**
 * Download Manager for handling file download and checking the integrity of the download.
 *
 * @author David Withers
 */
public interface DownloadManager {

	/**
	 * Downloads a file from a URL.
	 * <p>
	 * The destination file will be created if it does not exist. If it does exist it will be
	 * overwritten.
	 *
	 * @param source
	 *            the file to download
	 * @param destination
	 *            the file to write to
	 * @throws DownloadException
	 *             if
	 *             <ul>
	 *             <li>the source does not exist</li> <li>the source cannot be downloaded</li> <li>
	 *             the destination is not a file</li> <li>the destination cannot be written to</li>
	 *             </ul>
	 */
	public void download(URL source, File destination) throws DownloadException;

	/**
	 * Downloads a file from a URL and checks the integrity of the download by downloading and
	 * verifying the a checksum using the specified algorithm.
	 * <p>
	 * Every implementation is required to support the following standard algorithms:
	 * <ul>
	 * <li>MD5</li>
	 * <li>SHA-1</li>
	 * <li>SHA-256</li>
	 * </ul>
	 * <p>
	 * The checksum source will be calculated by appending the algorithm name to the source. e.g.
	 * for an MD5 algorithm and a source of http://www.example.com/test.xml the checksum will be
	 * downloaded from http://www.example.com/test.xml.md5
	 *
	 * @param source
	 *            the file to download
	 * @param destination
	 *            the file to write to
	 * @param digestAlgorithm
	 *            the digest algorithm to use
	 * @throws DownloadException
	 *             if
	 *             <ul>
	 *             <li>the source does not exist</li> <li>the digest source does not exist</li> <li>
	 *             the source cannot be downloaded</li> <li>the destination cannot be written to
	 *             </li> <li>the destination is not a file</li> <li>the checksums do no match</li>
	 *             </ul>
	 */
	public void download(URL source, File destination, String digestAlgorithm)
			throws DownloadException;

	/**
	 * Downloads a file from a URL and checks the integrity of the download by downloading and
	 * verifying the a checksum using the specified algorithm.
	 * <p>
	 * Every implementation is required to support the following standard algorithms:
	 * <ul>
	 * <li>MD5</li>
	 * <li>SHA-1</li>
	 * <li>SHA-256</li>
	 * </ul>
	 * <p>
	 *
	 * @param source
	 *            the file to download
	 * @param destination
	 *            the file to write to
	 * @param digestAlgorithm
	 *            the digest algorithm to use
	 * @param digestSource
	 *            the digest file to check
	 * @throws DownloadException
	 *             if
	 *                <ul>
	 *                <li>the source does not exist</li> <li>the digest source does not exist</li>
	 *                <li> the source cannot be downloaded</li> <li>the destination cannot be
	 *                written to</li> <li>the destination is not a file</li> <li>the digestSource
	 *                does not exist</li> <li>the checksums do no match</li>
	 *                </ul>
	 */
	public void download(URL source, File destination, String digestAlgorithm, URL digestSource)
			throws DownloadException;

}
