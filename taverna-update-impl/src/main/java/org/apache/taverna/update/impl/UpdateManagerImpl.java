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
package org.apache.taverna.update.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.taverna.configuration.app.ApplicationConfiguration;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;
import org.apache.taverna.profile.xml.jaxb.ApplicationProfile;
import org.apache.taverna.profile.xml.jaxb.BundleInfo;
import org.apache.taverna.profile.xml.jaxb.UpdateSite;
import org.apache.taverna.profile.xml.jaxb.Updates;
import org.apache.taverna.update.UpdateException;
import org.apache.taverna.update.UpdateManager;
import org.apache.taverna.versions.xml.jaxb.Version;
import org.apache.taverna.versions.xml.jaxb.Versions;
import org.osgi.service.event.EventAdmin;

/**
 * Implementation of the Taverna Update Manager.
 *
 */
public class UpdateManagerImpl implements UpdateManager {

	private static final String DIGEST_ALGORITHM = "MD5";

	private EventAdmin eventAdmin;

	private int checkIntervalSeconds;

	private ApplicationConfiguration applicationConfiguration;

	private DownloadManager downloadManager;

	private long lastCheckTime;
	private boolean updateAvailable;
	private Unmarshaller unmarshaller;

	private Versions applicationVersions;
	private Version latestVersion;

	public UpdateManagerImpl() throws UpdateException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(UpdateSite.class, ApplicationProfile.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new UpdateException("Error creating JAXBContext", e);
		}
	}

	@Override
	public boolean checkForUpdates() throws UpdateException {
		ApplicationProfile applicationProfile = applicationConfiguration.getApplicationProfile();
		String version = applicationProfile.getVersion();
		Updates updates = applicationProfile.getUpdates();

		URI updatesURL;
		try {
			URI updateSiteURI = new URI(updates.getUpdateSite());
			updatesURL = updateSiteURI.resolve(updates.getUpdatesFile());
		} catch (URISyntaxException e) {
			throw new UpdateException(String.format("Update site URL (%s) is not a valid URL",
					updates.getUpdateSite()), e);
		}
		File updateDirectory = applicationConfiguration.getApplicationHomeDir().resolve("updates").toFile();
		updateDirectory.mkdirs();
		File updatesFile = new File(updateDirectory, updates.getUpdatesFile());
		try {
			downloadManager.download(updatesURL, updatesFile.toPath(), DIGEST_ALGORITHM);
		} catch (DownloadException e) {
			throw new UpdateException(String.format("Error downloading %1$s",
					updatesURL), e);
		}

		try {
			UpdateSite updateSite = (UpdateSite) unmarshaller
					.unmarshal(updatesFile);
			applicationVersions = updateSite.getVersions();
			latestVersion = applicationVersions.getLatestVersion();
			updateAvailable = isHigherVersion(latestVersion.getVersion(), version);
		} catch (JAXBException e) {
			throw new UpdateException(String.format("Error reading %s",
					updatesFile.getName()), e);
		}
		lastCheckTime = System.currentTimeMillis();
		return updateAvailable;
	}

	@Override
	public boolean update() throws UpdateException {
		if (updateAvailable) {
			ApplicationProfile applicationProfile = applicationConfiguration.getApplicationProfile();
			Updates updates = applicationProfile.getUpdates();
			URI profileURL;
			try {
				URI updateSiteURI = new URI(updates.getUpdateSite());
				profileURL = updateSiteURI.resolve(latestVersion.getFile());
			} catch (URISyntaxException e) {
				throw new UpdateException(String.format("Update site URL (%s) is not a valid URL",
						updates.getUpdateSite()), e);
			}

			File updateDirectory = applicationConfiguration.getApplicationHomeDir().resolve(
					"updates").toFile();
			updateDirectory.mkdirs();
			File latestProfileFile = new File(updateDirectory, "ApplicationProfile-"
					+ latestVersion.getVersion() + ".xml");
			try {
				downloadManager.download(profileURL, latestProfileFile.toPath(), DIGEST_ALGORITHM);
			} catch (DownloadException e) {
				throw new UpdateException(String.format("Error downloading %1$s",
						profileURL), e);
			}

			ApplicationProfile latestProfile;
			try {
				latestProfile = (ApplicationProfile) unmarshaller.unmarshal(latestProfileFile);
			} catch (JAXBException e) {
				throw new UpdateException(String.format("Error reading %s",
						latestProfileFile.getName()), e);
			}

			Set<BundleInfo> requiredBundles = getRequiredBundles(
					applicationConfiguration.getApplicationProfile(), latestProfile);
			downloadBundles(latestProfile, requiredBundles, applicationConfiguration.getStartupDir().resolve("lib").toFile());
			File applicationProfileFile = applicationConfiguration.getStartupDir().resolve("ApplicationProfile.xml").toFile();
			try {
				FileUtils.copyFile(latestProfileFile, applicationProfileFile);
			} catch (IOException e) {
				throw new UpdateException(String.format("Error copying %1$s to %2$s",
						latestProfileFile.getName(), applicationProfileFile.getName()), e);
			}
//			eventAdmin.postEvent(new Event("UpdateManagerEvent", new HashMap()));
			updateAvailable = false;
			return true;
		}
		return false;
	}

	/**
	 * @param requiredBundles
	 * @param file
	 * @throws UpdateException
	 */
	private void downloadBundles(ApplicationProfile profile, Set<BundleInfo> requiredBundles, File file) throws UpdateException {
		Updates updates = profile.getUpdates();
		String updateSite = updates.getUpdateSite();
		String libDirectory = updates.getLibDirectory();
		if (!libDirectory.endsWith("/")) {
			libDirectory = libDirectory + "/";
		}

		URI updateLibDirectory;
		try {
			updateLibDirectory = new URI(updateSite).resolve(libDirectory);
		} catch (URISyntaxException e) {
			throw new UpdateException(String.format("Update site URL (%s) is not a valid URL",
					updates.getUpdateSite()), e);
		}
		for (BundleInfo bundle : requiredBundles) {
			URI bundleURI = updateLibDirectory.resolve(bundle.getFileName());
			Path bundleDestination = new File(file, bundle.getFileName()).toPath();
			try {
				downloadManager.download(bundleURI, bundleDestination, DIGEST_ALGORITHM);
			} catch (DownloadException e) {
				throw new UpdateException(String.format("Error downloading %1$s to %2$s",
						bundleURI, bundleDestination), e);
			}
		}
	}

	/**
	 * Returns the new bundles required for the new application profile.
	 *
	 * @param currentProfile
	 * @param newProfile
	 * @return the new bundles required for the new application profile
	 */
	private Set<BundleInfo> getRequiredBundles(ApplicationProfile currentProfile,
			ApplicationProfile newProfile) {
		Set<BundleInfo> requiredBundles = new HashSet<BundleInfo>();
		Map<String, BundleInfo> currentBundles = new HashMap<String, BundleInfo>();
		for (BundleInfo bundle : currentProfile.getBundle()) {
			currentBundles.put(bundle.getSymbolicName(), bundle);
		}
		for (BundleInfo bundle : newProfile.getBundle()) {
			if (currentBundles.containsKey(bundle.getSymbolicName())) {
				BundleInfo currentBundle = currentBundles.get(bundle.getSymbolicName());
				if (!bundle.getVersion().equals(currentBundle.getVersion())) {
					requiredBundles.add(bundle);
				}
			} else {
				requiredBundles.add(bundle);
			}
		}
		return requiredBundles;
	}

	private boolean isHigherVersion(String version1, String version2) {
		org.osgi.framework.Version semanticVersion1 = org.osgi.framework.Version.parseVersion(version1);
		org.osgi.framework.Version semanticVersion2 = org.osgi.framework.Version.parseVersion(version2);
		return semanticVersion1.compareTo(semanticVersion2) > 0;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void setCheckIntervalSeconds(int checkIntervalSeconds) {
		this.checkIntervalSeconds = checkIntervalSeconds;
	}

	public void setApplicationConfiguration(ApplicationConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}

	public void setDownloadManager(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

}
