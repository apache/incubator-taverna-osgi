/*******************************************************************************
 * Copyright (C) 2009 The University of Manchester
 *
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
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

import uk.org.taverna.commons.plugin.xml.jaxb.PluginInfo;
import uk.org.taverna.commons.plugin.xml.jaxb.PluginVersions;
import uk.org.taverna.commons.plugin.xml.jaxb.Plugins;
import uk.org.taverna.commons.versions.xml.jaxb.Version;

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

	@Override
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
