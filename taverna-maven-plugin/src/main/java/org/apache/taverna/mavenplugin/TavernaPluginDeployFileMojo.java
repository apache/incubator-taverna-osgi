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
package org.apache.taverna.mavenplugin;

import static org.apache.taverna.mavenplugin.TavernaPluginGenerateMojo.META_INF_TAVERNA;
import static org.apache.taverna.mavenplugin.TavernaPluginGenerateMojo.PLUGIN_FILE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.apache.taverna.plugin.xml.jaxb.PluginInfo;
import org.apache.taverna.plugin.xml.jaxb.PluginVersions;
import org.apache.taverna.plugin.xml.jaxb.Plugins;
import org.apache.taverna.versions.xml.jaxb.Version;

/**
 * Deploys the Taverna plugin using <code>scp</code> or <code>file</code> protocol to the site URL
 * specified.
 *
 * @author David Withers
 */
@Mojo(name = "plugin-deploy-file", requiresProject=false, requiresDirectInvocation = true)
public class TavernaPluginDeployFileMojo extends AbstractWagonMojo {

	private static final String PLUGIN_FILE_ENTRY = META_INF_TAVERNA + "/" + PLUGIN_FILE;

	private static final String PLUGINS_FILE = "plugins.xml";

	@Parameter(defaultValue = "http://updates.taverna.org.uk/workbench/3.0/dev/", required = true)
	protected String site;

	@Parameter(property = "file", required = true)
	protected File file;

	@Parameter(property = "url", required = true)
	protected String url;

	@Parameter(property = "serverId", required = true)
	protected String serverId;

	public void execute() throws MojoExecutionException {
		if (!file.exists()) {
			throw new MojoExecutionException("The Taverna Plugin file " + file
					+ " does not exist");
		}

		JarFile pluginJarFile;
		try {
			pluginJarFile = new JarFile(file);
		} catch (ZipException e) {
			throw new MojoExecutionException(file + " is not a valid Taverna Plugin file", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Error opening Taverna Plugin file: " + file, e);
		}

		ZipEntry pluginFileEntry = pluginJarFile.getJarEntry(PLUGIN_FILE_ENTRY);
		if (pluginFileEntry == null) {
			throw new MojoExecutionException(file
					+ " is not a valid Taverna Plugin file, missing " + PLUGIN_FILE_ENTRY);
		}

		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(PluginInfo.class, Plugins.class);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error setting up JAXB context ", e);
		}

		PluginInfo plugin;
		try {
			InputStream inputStream = pluginJarFile.getInputStream(pluginFileEntry);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			plugin = (PluginInfo) unmarshaller.unmarshal(inputStream);
			inputStream.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading " + file, e);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error reading " + file, e);
		}

		getLog().debug("The Taverna plugin will be deployed to '" + url + "'");

		Repository repository = new Repository(serverId, url);

		// create the wagon
		Wagon wagon;
		try {
			wagon = wagonManager.getWagon(repository.getProtocol());
		} catch (UnsupportedProtocolException e) {
			throw new MojoExecutionException("Unsupported protocol: '" + repository.getProtocol()
					+ "'", e);
		}

		Debug debug = new Debug();
		if (getLog().isDebugEnabled()) {
			wagon.addSessionListener(debug);
			wagon.addTransferListener(debug);
		}

		// connect to the plugin site
		try {
			wagon.connect(repository, wagonManager.getAuthenticationInfo(serverId),
					wagonManager.getProxy(repository.getProtocol()));
		} catch (ConnectionException e) {
			throw new MojoExecutionException("Error connecting to plugin site at " + url, e);
		} catch (AuthenticationException e) {
			throw new MojoExecutionException("Authentication error connecting to plugin site at "
					+ url, e);
		}

		try {
			File pluginsFile = Files.createTempFile("taverna",null).toFile();

			// fetch the plugins file
			Plugins plugins;
			try {
				Utils.downloadFile(PLUGINS_FILE, pluginsFile, wagon, getLog());
				try {
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
					plugins = (Plugins) unmarshaller.unmarshal(pluginsFile);
				} catch (JAXBException e) {
					throw new MojoExecutionException("Error reading " + pluginsFile, e);
				}
			} catch (ResourceDoesNotExistException e) {
				getLog().info("Creating new plugins file");
				plugins = new Plugins();
			}

			String deployedPluginFile = plugin.getId() + "-" + plugin.getVersion() + ".jar";

			if (addPlugin(plugins, plugin, deployedPluginFile)) {
				// write the new plugin site file
				try {
					Marshaller marshaller = jaxbContext.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
							TavernaPluginGenerateMojo.SCHEMA_LOCATION);
					marshaller.marshal(plugins, pluginsFile);
				} catch (JAXBException e) {
					throw new MojoExecutionException("Error writing " + PLUGINS_FILE, e);
				}

				// upload the plugin to the update site
				Utils.uploadFile(file, deployedPluginFile, wagon, getLog());
				// upload the plugin site file
				Utils.uploadFile(pluginsFile, PLUGINS_FILE, wagon, getLog());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing " + PLUGINS_FILE, e);
		} finally {
			disconnectWagon(wagon, debug);
		}
	}

	private boolean addPlugin(Plugins plugins, PluginInfo pluginInfo, String pluginURL) {
		PluginVersions plugin = getPlugin(plugins, pluginInfo);
		Version latestVersion = plugin.getLatestVersion();
		if (latestVersion != null && latestVersion.getVersion().equals(pluginInfo.getVersion())) {
			getLog().error(
					String.format("%1$s version %2$s has already been deployed",
							pluginInfo.getName(), pluginInfo.getVersion()));
			return false;
		}
		Version newPluginVersion = new Version();
		newPluginVersion.setVersion(pluginInfo.getVersion());
		newPluginVersion.setFile(pluginURL);

		getLog().info(
				String.format("Adding %1$s version %2$s", pluginInfo.getName(),
						pluginInfo.getVersion()));
		if (plugin.getLatestVersion() != null) {
			plugin.getPreviousVersion().add(plugin.getLatestVersion());
		}
		plugin.setLatestVersion(newPluginVersion);
		return true;
	}

	private PluginVersions getPlugin(Plugins plugins, PluginInfo pluginInfo) {
		PluginVersions pluginVersions = null;
		for (PluginVersions existingPlugin : plugins.getPlugin()) {
			if (existingPlugin.getId().equals(pluginInfo.getId())) {
				pluginVersions = existingPlugin;
				break;
			}
		}
		if (pluginVersions == null) {
			pluginVersions = new PluginVersions();
			pluginVersions.setId(pluginInfo.getId());
			plugins.getPlugin().add(pluginVersions);
		}
		pluginVersions.setName(pluginInfo.getName());
		pluginVersions.setDescription(pluginInfo.getDescription());
		pluginVersions.setOrganization(pluginInfo.getOrganization());
		return pluginVersions;
	}

}
