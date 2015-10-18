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
package net.sf.taverna.t2.maven.plugins;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the POM.
 *
 * @author David Withers
 */
@Mojo(name = "plugin-deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class TavernaPluginDeployMojo extends AbstractDeployMojo {

	private static final String PLUGINS_FILE = "plugins.xml";

	private File tempDirectory;

	public void execute() throws MojoExecutionException {
		tempDirectory = new File(buildDirectory, TavernaProfileGenerateMojo.TAVERNA_TMP);
		tempDirectory.mkdirs();
		if (artifact == null) {
			throw new MojoExecutionException(
					"The Taverna Plugin does not exist, please run taverna:plugin-generate first");
		}

		File artifactFile = artifact.getFile();
		if (artifactFile == null) {
			throw new MojoExecutionException(
					"The Taverna Plugin does not exist, please run taverna:plugin-generate first");
		}

		File pluginDirectory = new File(outputDirectory, TavernaPluginGenerateMojo.META_INF_TAVERNA);
		File pluginFile = new File(pluginDirectory, TavernaPluginGenerateMojo.PLUGIN_FILE);
		if (!pluginFile.exists()) {
			throw new MojoExecutionException(
					"The Taverna Plugin does not exist, please run taverna:plugin-generate first");
		}

		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(PluginInfo.class, Plugins.class);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error setting up JAXB context ", e);
		}

		PluginInfo plugin;
		try {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			plugin = (PluginInfo) unmarshaller.unmarshal(pluginFile);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error reading " + pluginFile, e);
		}

		if (deploymentRepository == null) {
			throw new MojoExecutionException(
					"Missing repository information in the distribution management element in the project.");
		}

		String url = deploymentRepository.getUrl();
		String id = deploymentRepository.getId();

		if (url == null) {
			throw new MojoExecutionException(
					"The URL to the Taverna plugin site is missing in the project descriptor.");
		}
		getLog().debug("The Taverna plugin will be deployed to '" + url + "'");

		Repository repository = new Repository(id, url);

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
			wagon.connect(repository, wagonManager.getAuthenticationInfo(id),
					wagonManager.getProxy(repository.getProtocol()));
		} catch (ConnectionException e) {
			throw new MojoExecutionException("Error connecting to plugin site at " + url, e);
		} catch (AuthenticationException e) {
			throw new MojoExecutionException("Authentication error connecting to plugin site at "
					+ url, e);
		}

		try {
			String deployedPluginFile = project.getGroupId() + "." + project.getArtifactId() + "-"
					+ plugin.getVersion() + ".jar";

			// fetch the plugins file
			Plugins plugins;
			File pluginsFile = new File(tempDirectory, PLUGINS_FILE);
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
				Utils.uploadFile(artifactFile, deployedPluginFile, wagon, getLog());
				// upload the plugin site file
				Utils.uploadFile(pluginsFile, PLUGINS_FILE, wagon, getLog());
			}
		} finally {
			disconnectWagon(wagon, debug);
		}
	}

	private boolean addPlugin(Plugins plugins, PluginInfo pluginInfo, String pluginURL) {
		PluginVersions plugin = getPlugin(plugins, pluginInfo);
		Version latestVersion = plugin.getLatestVersion();
		if (latestVersion != null && latestVersion.getVersion().equals(pluginInfo.getVersion())) {
			getLog().error(
					String.format("%1$s version %2$s has already been deployed", pluginInfo.getName(),
							pluginInfo.getVersion()));
			return false;
		}
		Version newPluginVersion = new Version();
		newPluginVersion.setVersion(pluginInfo.getVersion());
		newPluginVersion.setFile(pluginURL);

		getLog().info(
				String.format("Adding %1$s version %2$s", pluginInfo.getName(), pluginInfo.getVersion()));
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
