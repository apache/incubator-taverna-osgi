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
package org.apache.taverna.plugin.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.taverna.configuration.app.ApplicationConfiguration;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;
import org.apache.taverna.plugin.PluginException;
import org.apache.taverna.plugin.PluginSite;
import org.apache.taverna.plugin.PluginSite.PluginSiteType;
import org.apache.taverna.plugin.PluginSiteManager;
import org.apache.taverna.plugin.xml.jaxb.PluginVersions;
import org.apache.taverna.plugin.xml.jaxb.Plugins;
import org.apache.taverna.profile.xml.jaxb.Updates;


/**
 * PluginSiteManager implementation.
 *
 * @author David Withers
 */
public class PluginSiteManagerImpl implements PluginSiteManager {

	private static final String PLUGIN_SITES_FILE = "plugin-sites.xml";
	private static final String DIGEST_ALGORITHM = "MD5";
	private static final String PLUGINS_FILE = "plugins.xml";

	private static final Logger logger = Logger.getLogger(PluginSiteManagerImpl.class);

	private ApplicationConfiguration applicationConfiguration;
	private DownloadManager downloadManager;

	private Unmarshaller unmarshaller;
	private Marshaller marshaller;

	private List<PluginSite> pluginSites;

	public PluginSiteManagerImpl() throws PluginException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Plugins.class, PluginSites.class);
			unmarshaller = jaxbContext.createUnmarshaller();
			marshaller = jaxbContext.createMarshaller();
		} catch (JAXBException e) {
			throw new PluginException("Error creating JAXBContext", e);
		}
	}

	@Override
	public List<PluginSite> getPluginSites() {
		if (pluginSites == null) {
			readPluginSitesFile();
			if (pluginSites == null) {
				pluginSites = new ArrayList<PluginSite>();
				pluginSites.addAll(getSystemPluginSites());
			}
		}
		return pluginSites;
	}

	@Override
	public PluginSite createPluginSite(URL pluginSiteURL) throws PluginException {
		try {
			File tempFile = File.createTempFile("plugins", null);
			tempFile.deleteOnExit();
			URL pluginFileURL = new URL(pluginSiteURL + "/" + PLUGINS_FILE);
			downloadManager.download(pluginFileURL, tempFile, DIGEST_ALGORITHM);
			return new PluginSiteImpl("", pluginSiteURL.toExternalForm());
		} catch (MalformedURLException e) {
			throw new PluginException(String.format("Invalid plugin site URL %1$s", pluginSiteURL), e);
		} catch (DownloadException e) {
			throw new PluginException(String.format("Error contacting plugin site at %1$s", pluginSiteURL), e);
		} catch (IOException e) {
			throw new PluginException(String.format("Error contacting plugin site at %1$s", pluginSiteURL), e);
		}
	}

	@Override
	public void addPluginSite(PluginSite pluginSite) throws PluginException {
		getPluginSites().add(pluginSite);
		writePluginSitesFile();
	}

	@Override
	public void removePluginSite(PluginSite pluginSite) throws PluginException {
		getPluginSites().remove(pluginSite);
		writePluginSitesFile();
	}

	@Override
	public List<PluginVersions> getPlugins(PluginSite pluginSite) throws PluginException {
		List<PluginVersions> plugins = new ArrayList<PluginVersions>();
		try {
			URL pluginSiteURL = new URL(pluginSite.getUrl() + "/" + PLUGINS_FILE);
			File pluginsFile = new File(getDataDirectory(), PLUGINS_FILE);
			downloadManager.download(pluginSiteURL, pluginsFile, DIGEST_ALGORITHM);
			Plugins pluginsXML = (Plugins) unmarshaller.unmarshal(pluginsFile);
			for (PluginVersions plugin : pluginsXML.getPlugin()) {
				plugin.setPluginSiteUrl(pluginSite.getUrl());
				plugins.add(plugin);
			}
		} catch (MalformedURLException e) {
			throw new PluginException(String.format("Plugin site %1$s has an invalid location",
					pluginSite.getName()), e);
		} catch (DownloadException e) {
			throw new PluginException(String.format("Error downloading from plugin site %1$s",
					pluginSite.getName()), e);
		} catch (JAXBException e) {
			throw new PluginException(String.format("Error getting plugins from plugin site %1$s",
					pluginSite.getName()), e);
		}
		return plugins;
	}

	private List<PluginSite> getSystemPluginSites() {
		List<PluginSite> systemPluginSites = new ArrayList<PluginSite>();
		Updates updates = applicationConfiguration.getApplicationProfile().getUpdates();
		systemPluginSites
				.add(new PluginSiteImpl("", updates.getPluginSite(), PluginSiteType.SYSTEM));
		return systemPluginSites;
	}

	private void writePluginSitesFile() {
		File pluginSitesFile = new File(getDataDirectory(), PLUGIN_SITES_FILE);
		try {
			marshaller.marshal(pluginSites, pluginSitesFile);
		} catch (JAXBException e) {
			logger.error("Error writing file " + pluginSitesFile, e);
		}
	}

	private void readPluginSitesFile() {
		File pluginSitesFile = new File(getDataDirectory(), PLUGIN_SITES_FILE);
		if (pluginSitesFile.exists()) {
			try {
				pluginSites = new ArrayList<PluginSite>();
				PluginSites pluginSitesStore = (PluginSites) unmarshaller
						.unmarshal(pluginSitesFile);
				for (PluginSiteImpl pluginSiteImpl : pluginSitesStore.getPluginSites()) {
					pluginSites.add(pluginSiteImpl);
				}
			} catch (JAXBException e) {
				logger.error("Error reading file " + pluginSitesFile, e);
			}
		}
	}

	private File getDataDirectory() {
		return new File(applicationConfiguration.getApplicationHomeDir(), "plugin-data");
	}

	public void setApplicationConfiguration(ApplicationConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}

	public void setDownloadManager(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

}
