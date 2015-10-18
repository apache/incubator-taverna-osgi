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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.taverna.configuration.app.ApplicationConfiguration;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;
import org.apache.taverna.plugin.Plugin;
import org.apache.taverna.plugin.Plugin.State;
import org.apache.taverna.plugin.PluginException;
import org.apache.taverna.plugin.PluginManager;
import org.apache.taverna.plugin.PluginSite;
import org.apache.taverna.plugin.PluginSiteManager;
import org.apache.taverna.plugin.xml.jaxb.PluginInfo;
import org.apache.taverna.plugin.xml.jaxb.PluginVersions;
import org.apache.taverna.profile.xml.jaxb.BundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * PluginManager implementation.
 *
 * @author David Withers
 */
public class PluginManagerImpl implements PluginManager {

	private static final String DIGEST_ALGORITHM = "MD5";
	private static final String PLUGIN_FILE_NAME = "META-INF/taverna/plugin.xml";

	private static final Logger logger = Logger.getLogger(PluginManagerImpl.class);

	private EventAdmin eventAdmin;
	private ApplicationConfiguration applicationConfiguration;
	private BundleContext bundleContext;
	private DownloadManager downloadManager;
	private PluginSiteManager pluginSiteManager;

	private Map<String, Plugin> installedPlugins = new TreeMap<String, Plugin>();
	private Map<String, PluginVersions> availablePlugins = new TreeMap<String, PluginVersions>();
	private Map<String, PluginVersions> pluginUpdates = new TreeMap<String, PluginVersions>();

	private boolean updateAvailablePlugins = true;

	private Map<File, PluginDirectoryWatcher> pluginDirectoryWatchers = new HashMap<File, PluginDirectoryWatcher>();

	private Set<Bundle> installedBundles = new HashSet<Bundle>();

	private Unmarshaller unmarshaller;

	public PluginManagerImpl() throws PluginException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(PluginInfo.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new PluginException("Error creating JAXBContext", e);
		}
	}

	@Override
	public void checkForUpdates() throws PluginException {
		boolean updatesFound = false;
		synchronized (pluginUpdates) {
			pluginUpdates.clear();
			for (PluginSite pluginSite : pluginSiteManager.getPluginSites()) {
				List<PluginVersions> plugins = pluginSiteManager.getPlugins(pluginSite);
				for (PluginVersions plugin : plugins) {
					if (installedPlugins.containsKey(plugin.getId())) {
						Plugin installedPlugin = installedPlugins.get(plugin.getId());
						if (installedPlugin.getFile().canWrite()) {
							Version latestVersion = Version.parseVersion(plugin.getLatestVersion()
									.getVersion());
							if (latestVersion.compareTo(installedPlugin.getVersion()) > 0) {
								pluginUpdates.put(plugin.getId(), plugin);
								updatesFound = true;
							}
						}
					}
				}
			}
		}
		if (updatesFound) {
			postEvent(PluginManager.UPDATES_AVAILABLE);
		}
	}

	@Override
	public List<PluginVersions> getPluginUpdates() throws PluginException {
		synchronized (pluginUpdates) {
			return new ArrayList<PluginVersions>(pluginUpdates.values());
		}
	}

	@Override
	public void loadPlugins() throws PluginException {
		loadPlugins(applicationConfiguration.getSystemPluginDir());
		loadPlugins(applicationConfiguration.getUserPluginDir());
	}

	@Override
	public List<PluginVersions> getAvailablePlugins() throws PluginException {
		if (updateAvailablePlugins) {
			synchronized (availablePlugins) {
				availablePlugins = new HashMap<String, PluginVersions>();
				for (PluginSite pluginSite : pluginSiteManager.getPluginSites()) {
					List<PluginVersions> plugins = pluginSiteManager.getPlugins(pluginSite);
					for (PluginVersions plugin : plugins) {
						if (!installedPlugins.containsKey(plugin.getId())) {
							availablePlugins.put(plugin.getId(), plugin);
						}
					}
				}
			}
			updateAvailablePlugins = false;
		}
		return new ArrayList<PluginVersions>(availablePlugins.values());
	}

	@Override
	public List<Plugin> getInstalledPlugins() throws PluginException {
		return new ArrayList<Plugin>(installedPlugins.values());
	}

	@Override
	public Plugin installPlugin(File pluginFile) throws PluginException {
		// check if already installed
		synchronized (installedPlugins) {
			for (Plugin plugin : installedPlugins.values()) {
				if (plugin.getFile().equals(pluginFile)) {
					return plugin;
				}
			}
			// check plugin file
			if (pluginFile.exists()) {
				new PluginException(String.format("Plugin file %1$s does not exist", pluginFile));
			}
			if (pluginFile.isFile()) {
				new PluginException(String.format("Plugin file %1$s is not a file", pluginFile));
			}
			if (!pluginFile.canRead()) {
				new PluginException(String.format("Plugin file %1$s is not readable", pluginFile));
			}
			// install plugin from plugin file
			logger.info(String.format("Installing plugin from '%s'", pluginFile));
			JarFile jarFile;
			try {
				jarFile = new JarFile(pluginFile);
			} catch (IOException e) {
				throw new PluginException(String.format("Error reading plugin file %1$s",
						pluginFile), e);
			}
			Plugin plugin = installPlugin(jarFile);
			installedPlugins.put(plugin.getId(), plugin);
			availablePlugins.remove(plugin.getId());
			postEvent(PluginManager.PLUGIN_INSTALLED);
			return plugin;
		}
	}

	@Override
	public Plugin installPlugin(String pluginSiteURL, String pluginFileName) throws PluginException {
		File pluginFile = getPluginFile(pluginSiteURL, pluginFileName);
		return installPlugin(pluginFile);
	}

	@Override
	public Plugin updatePlugin(PluginVersions pluginVersions) throws PluginException {
		String pluginId = pluginVersions.getId();
		String pluginSiteUrl = pluginVersions.getPluginSiteUrl();
		String pluginFile = pluginVersions.getLatestVersion().getFile();
		Plugin plugin = installedPlugins.get(pluginId);
		plugin.stop();
		Plugin newPlugin;
		try {
			newPlugin = installPlugin(pluginSiteUrl, pluginFile);
		} catch (PluginException e) {
			plugin.start();
			throw new PluginException("Failed to update plugin " + pluginId, e);
		}
		synchronized (pluginUpdates) {
			pluginUpdates.remove(pluginId);
		}
		uninstallPlugin(plugin);
		return newPlugin;
	}

	void uninstallPlugin(File pluginFile) {
		synchronized (installedPlugins) {
			for (Plugin plugin : installedPlugins.values()) {
				if (plugin.getFile().equals(pluginFile)) {
					uninstallPlugin(plugin);
					break;
				}
			}
		}
	}

	void uninstallPlugin(Plugin plugin) {
		synchronized (installedPlugins) {
			if (installedPlugins.containsKey(plugin.getId())) {
				for (Bundle bundle : plugin.getBundles()) {
					if (installedBundles.contains(bundle)) {
						// check if bundle is used by other plugins
						boolean bundleInUse = false;
						for (Plugin installedPlugin : installedPlugins.values()) {
							if (!installedPlugin.equals(plugin)) {
								if (installedPlugin.getBundles().contains(bundle)) {
									bundleInUse = true;
									break;
								}
							}
						}
						if (!bundleInUse) {
							try {
								logger.info("Uninstalling bundle " + bundle.getSymbolicName());
								bundle.uninstall();
								installedBundles.remove(bundle);
								System.out.println("Remove " + bundle.getSymbolicName());
							} catch (BundleException e) {
								logger.warn(String.format(
										"Error uninstalling bundle %1$s for plugin %2$s",
										bundle.getSymbolicName(), plugin.getName()), e);
							}
						}
					}
				}
				installedPlugins.remove(plugin.getId());
				pluginUpdates.remove(plugin.getId());
				updateAvailablePlugins = true;
				postEvent(PluginManager.PLUGIN_UNINSTALLED);
			}
		}
	}

	public void loadPlugins(File pluginDir) throws PluginException {
		if (checkPluginDirectory(pluginDir, false)) {
			for (File pluginFile : pluginDir.listFiles()) {
				if (pluginFile.isFile() && pluginFile.canRead() && !pluginFile.isHidden()) {
					try {
						installPlugin(pluginFile).start();
					} catch (PluginException e) {
						logger.warn(String.format("Error loading plugin from '%s'", pluginFile), e);
					}
				}
			}
		}
		startWatchingPluginDirectory(pluginDir);
	}

	private Plugin installPlugin(JarFile jarFile) throws PluginException {
		PluginInfo pluginInfo = getPluginInfo(jarFile);

		PluginImpl plugin = new PluginImpl(this, new File(jarFile.getName()), pluginInfo);

		// check bundles exist in jar
		for (BundleInfo bundleInfo : pluginInfo.getBundle()) {
			// find the bundle in the plugin jar
			JarEntry entry = jarFile.getJarEntry(bundleInfo.getFileName());
			if (entry == null) {
				throw new PluginException(String.format(
						"Plugin file '%1$s' does not contain bundle file '%2$s'.",
						jarFile.getName(), bundleInfo.getFileName()));
			}
		}

		// install plugin bundles
		Set<Bundle> pluginBundles = plugin.getBundles();
		for (BundleInfo bundleInfo : pluginInfo.getBundle()) {
			Bundle installedBundle = getInstalledBundle(bundleInfo);
			if (installedBundle == null) {
				// install the bundle from the jar
				JarEntry entry = jarFile.getJarEntry(bundleInfo.getFileName());
				String bundleURL = jarEntryToURL(jarFile, entry);
				try {
					Bundle bundle = bundleContext.installBundle(bundleURL);
					pluginBundles.add(bundle);
					installedBundles.add(bundle);
					System.out.println("Add " + bundle.getSymbolicName());
				} catch (BundleException e) {
					// clean up by removing bundles already installed
					for (Bundle bundle : pluginBundles) {
						try {
							bundle.uninstall();
							installedBundles.remove(bundle);
						} catch (BundleException ex) {
							logger.warn("Error unistalling bundle", ex);
						}
					}
					throw new PluginException(String.format("Error installing bundle file %1$s",
							bundleURL), e);
				}
			} else {
				pluginBundles.add(installedBundle);
			}
		}
		plugin.setState(State.INSTALLED);
		return plugin;
	}

	private Bundle getInstalledBundle(BundleInfo bundleInfo) {
		for (Bundle installedBundle : bundleContext.getBundles()) {
			if (installedBundle.getSymbolicName().equals(bundleInfo.getSymbolicName())) {
				org.osgi.framework.Version installedVersion = installedBundle.getVersion();
				if (installedVersion
						.equals(new org.osgi.framework.Version(bundleInfo.getVersion()))) {
					return installedBundle;
				}
			}
		}
		return null;
	}

	public PluginInfo getPluginInfo(JarFile jarFile) throws PluginException {
		// TODO check manifest for non standard plugin info file
		JarEntry pluginEntry = jarFile.getJarEntry(PLUGIN_FILE_NAME);
		if (pluginEntry == null) {
			throw new PluginException(String.format(
					"Plugin file '%1$s' does not contain a %2$s file.", jarFile.getName(),
					PLUGIN_FILE_NAME));
		}
		try {
			InputStream inputStream = jarFile.getInputStream(pluginEntry);
			return (PluginInfo) unmarshaller.unmarshal(inputStream);
		} catch (JAXBException e) {
			throw new PluginException(String.format("Error reading plugin file %1$s from %2$s",
					pluginEntry, jarFile.getName()), e);
		} catch (IOException e) {
			throw new PluginException(String.format("Error reading plugin file %1$s from %2$s",
					pluginEntry, jarFile.getName()), e);
		}
	}

	private File getPluginFile(String pluginSiteURL, String pluginFileName) throws PluginException {
		File pluginFile = new File(getPluginDirectory(), pluginFileName);
		String pluginFileURL = pluginSiteURL + "/" + pluginFileName;
		try {
			downloadManager.download(new URL(pluginFileURL), pluginFile, DIGEST_ALGORITHM);
		} catch (DownloadException e) {
			throw new PluginException("Error downloading plugin file " + pluginFile, e);
		} catch (MalformedURLException e) {
			throw new PluginException("Invalid plugin file URL " + pluginFileURL, e);
		}
		return pluginFile;
	}

	public void startWatchingPluginDirectory(File pluginDir) throws PluginException {
		if (!pluginDirectoryWatchers.containsKey(pluginDir)) {
			pluginDirectoryWatchers.put(pluginDir, new PluginDirectoryWatcher(this, pluginDir));
		}
		pluginDirectoryWatchers.get(pluginDir).start();
	}

	public void stopWatchingPluginDirectory(File pluginDir) throws PluginException {
		if (pluginDirectoryWatchers.containsKey(pluginDir)) {
			pluginDirectoryWatchers.get(pluginDir).stop();
		}
	}

	private File getPluginDirectory() throws PluginException {
		File systemPluginsDir = applicationConfiguration.getSystemPluginDir();
		if (checkPluginDirectory(systemPluginsDir, true)) {
			return systemPluginsDir;
		}
		File userPluginsDir = applicationConfiguration.getUserPluginDir();
		if (checkPluginDirectory(userPluginsDir, true)) {
			return userPluginsDir;
		}
		throw new PluginException("No plugin directory avaliable");
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void setApplicationConfiguration(ApplicationConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setDownloadManager(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

	public void setPluginSiteManager(PluginSiteManager pluginSiteManager) {
		this.pluginSiteManager = pluginSiteManager;
	}

	private boolean checkPluginDirectory(File pluginDirectory, boolean checkWritable) {
		if (pluginDirectory == null) {
			return false;
		}
		if (!pluginDirectory.exists()) {
			logger.debug(String.format("Plugin directory %1$s does not exist", pluginDirectory));
			return false;
		}
		if (!pluginDirectory.isDirectory()) {
			logger.warn(String.format("Plugin directory %1$s is not a directory", pluginDirectory));
			return false;
		}
		if (!pluginDirectory.canRead()) {
			logger.debug(String.format("Plugin directory %1$s is not readable", pluginDirectory));
			return false;
		}
		if (checkWritable && !pluginDirectory.canWrite()) {
			logger.debug(String.format("Plugin directory %1$s is not writeable", pluginDirectory));
			return false;
		}
		return true;
	}

	private String jarEntryToURL(JarFile jarFile, JarEntry jarEntry) {
		File file = new File(jarFile.getName());
		return "jar:" + file.toURI() + "!/" + jarEntry.getName();
	}

	private void postEvent(String topic) {
		Event event = new Event(topic, new HashMap());
		eventAdmin.postEvent(event);
	}

}
