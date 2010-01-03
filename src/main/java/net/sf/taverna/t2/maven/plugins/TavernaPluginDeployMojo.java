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
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Deploys the Taverna plugin using <code>scp</code> or <code>file</code>
 * protocol to the site URL specified in the
 * <code>&lt;distributionManagement&gt;</code> section of the POM.
 * 
 * @goal plugin-deploy
 * @phase deploy
 * 
 * @author David Withers
 */
public class TavernaPluginDeployMojo extends AbstractMojo {

	/**
	 * Directory containing the generated Taverna plugin.
	 * 
	 * @parameter alias="outputDirectory"
	 *            expression="${project.build.directory}"
	 * @required
	 */
	private File buildDirectory;

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @readonly
     */
    private ArtifactRepository deploymentRepository;

    /**
	 * @component
	 */
	private WagonManager wagonManager;

	public void execute() throws MojoExecutionException {
		if (artifact == null) {
			throw new MojoExecutionException(
					"The Taverna Plugin does not exist, please run taverna:plugin-generate first");
		}

		File artifactFile = artifact.getFile();
		if (artifactFile == null) {
			throw new MojoExecutionException(
					"The Taverna Plugin does not exist, please run taverna:plugin-generate first");
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
			wagon = wagonManager.getWagon(repository);
		} catch (UnsupportedProtocolException e) {
			throw new MojoExecutionException("Unsupported protocol: '" + repository.getProtocol()
					+ "'", e);
		} catch (WagonConfigurationException e) {
			throw new MojoExecutionException("Unable to configure Wagon: '"
					+ repository.getProtocol() + "'", e);
		}

		Debug debug = new Debug();
		if (getLog().isDebugEnabled()) {
			wagon.addSessionListener(debug);
			wagon.addTransferListener(debug);
		}

		// connect to the plugin site
		try {
			wagon.connect(repository, wagonManager.getAuthenticationInfo(id), wagonManager
					.getProxy(repository.getProtocol()));
		} catch (ConnectionException e) {
			throw new MojoExecutionException("Error connecting to plugin site at " + url, e);
		} catch (AuthenticationException e) {
			throw new MojoExecutionException("Authentication error connecting to plugin site at "
					+ url, e);
		}

		// upload the plugin to the plugin site
		try {
			getLog().info("Deploying the Taverna plugin to plugin site at " + url + "");
			wagon.put(artifactFile, artifactFile.getName());
		} catch (TransferFailedException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Error transferring " + artifactFile.getName()
					+ "  to plugin site at " + url, e);
		} catch (ResourceDoesNotExistException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException(artifactFile.getName() + " does not exist", e);
		} catch (AuthorizationException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Authentication error transferring "
					+ artifactFile.getName() + "  to plugin site at " + url, e);
		}

		// fetch the plugin list
		Document document;
		File pluginlistFile = new File(buildDirectory, "pluginlist.xml");
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			if (wagon.resourceExists("pluginlist.xml")) {
				getLog().info("Fetching pluginlist.xml from " + url);
				wagon.get("pluginlist.xml", pluginlistFile);
				document = documentBuilder.parse(pluginlistFile);
			} else {
				getLog().info("Creating new pluginlist.xml");
				document = documentBuilder.newDocument();
				Node pluginsNode = document.createElement("plugins");
				document.appendChild(pluginsNode);
			}
		} catch (AuthorizationException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Authentication error connecting to plugin site at "
					+ url, e);
		} catch (ParserConfigurationException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Error configuring XML parser", e);
		} catch (TransferFailedException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Error transferring files from plugin site at " + url,
					e);
		} catch (ResourceDoesNotExistException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("pluginlist.xml does not exist at " + url, e);
		} catch (SAXException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Error parsing pluginlist.xml", e);
		} catch (IOException e) {
			disconnectWagon(wagon, debug);
			throw new MojoExecutionException("Error parsing pluginlist.xml", e);
		}

		if (addPlugin(document, artifactFile.getName())) {
			// write the new plugin list
			try {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.transform(new DOMSource(document), new StreamResult(pluginlistFile));
			} catch (TransformerConfigurationException e) {
				disconnectWagon(wagon, debug);
				throw new MojoExecutionException("Error configuring XML transformer", e);
			} catch (TransformerException e) {
				disconnectWagon(wagon, debug);
				throw new MojoExecutionException("Error writing pluginlist.xml", e);
			}

			// upload the new plugin list to the plugin site
			try {
				wagon.put(pluginlistFile, "pluginlist.xml");
			} catch (TransferFailedException e) {
				throw new MojoExecutionException(
						"Error transferring pluginlist.xml to plugin site at " + url, e);
			} catch (ResourceDoesNotExistException e) {
				throw new MojoExecutionException("pluginlist.xml does not exist", e);
			} catch (AuthorizationException e) {
				throw new MojoExecutionException(
						"Authentication error transferring pluginlist.xml to plugin site at " + url,
						e);
			} finally {
				disconnectWagon(wagon, debug);
			}
		} else {
			disconnectWagon(wagon, debug);
		}
	}

	/**
	 * Disconnect the wagon.
	 * 
	 * @param wagon
	 *            the wagon to disconnect
	 * @param debug
	 */
	private void disconnectWagon(Wagon wagon, Debug debug) {
		if (getLog().isDebugEnabled()) {
			wagon.removeTransferListener(debug);
			wagon.removeSessionListener(debug);
		}
		try {
			wagon.disconnect();
		} catch (ConnectionException e) {
			getLog().error("Error disconnecting wagon - ignored", e);
		}
	}

	/**
	 * Adds a plugin element to the plugins document if the plugin doesn't
	 * already exist.
	 * 
	 * @param document
	 *            the plugins document
	 * @param pluginFileName
	 *            the plugin file name
	 * @return true if a plugin element was added to the plugins document; false
	 *         if the plugin already exits
	 */
	private boolean addPlugin(Document document, String pluginFileName) {
		Node pluginsNode = document.getFirstChild();
		NodeList pluginNodes = pluginsNode.getChildNodes();
		for (int i = 0; i < pluginNodes.getLength(); i++) {
			Node pluginNode = pluginNodes.item(i);
			if (pluginFileName.equals(pluginNode.getTextContent())) {
				getLog().info(pluginFileName + " already exists in pluginlist.xml");
				return false;
			}
		}
		getLog().info("Adding " + pluginFileName + " to pluginlist.xml");
		Element element = document.createElement("plugin");
		element.setTextContent(pluginFileName);
		pluginsNode.appendChild(element);
		return true;
	}

}
