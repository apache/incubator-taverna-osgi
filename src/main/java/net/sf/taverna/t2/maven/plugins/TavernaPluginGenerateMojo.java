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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates a Taverna plugin definition file.
 * 
 * @goal plugin-generate
 * @phase deploy
 * @requiresDependencyResolution runtime
 * 
 * @author David Withers
 */
public class TavernaPluginGenerateMojo extends AbstractMojo {

	private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	private static final String SCHEMA_LOCATION = "http://taverna.sf.net/2008/xml/plugins http://taverna.sourceforge.net/2008/xml/plugins/plugins-2008-10-16.xsd";

	private static final String PLUGINS_NAMESPACE = "http://taverna.sf.net/2008/xml/plugins";

	/**
	 * Directory containing the generated Taverna plugin.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File buildDirectory;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The Maven session.
	 * 
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	private MavenSession session;

	/**
	 * The version of Taverna that the plugin is compatible with.
	 * 
	 * @parameter
	 * @required
	 */
	private String provider;

	/**
	 * The version of Taverna that the plugin is compatible with.
	 * 
	 * @parameter
	 * @required
	 */
	private String tavernaVersion;

	/**
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * @component 
	 *            role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
	 */
	private ArtifactMetadataSource artifactMetadataSource;

	public void execute() throws MojoExecutionException, MojoFailureException {
		File pluginFile;
		try {
			pluginFile = createPlugin();
		} catch (ParserConfigurationException e) {
			throw new MojoExecutionException("Error generating Taverna plugin", e);
		} catch (TransformerException e) {
			throw new MojoExecutionException("Error generating Taverna plugin", e);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("Error generating Taverna plugin", e);
		} catch (ArtifactNotFoundException e) {
			throw new MojoExecutionException("Error generating Taverna plugin", e);
		}

		project.getArtifact().setFile(pluginFile);
	}

	/**
	 * Generates the Taverna plugin definition file.
	 * 
	 * @return the <code>File</code> that the Taverna plugin definition has been
	 *         written to
	 * @throws ParserConfigurationException if the XML document cannot be created
	 * @throws TransformerException if the XML file cannot be written
	 * @throws ArtifactNotFoundException
	 * @throws ArtifactResolutionException
	 */
	private File createPlugin() throws ParserConfigurationException, TransformerException,
			ArtifactResolutionException, ArtifactNotFoundException {
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String artifactVersion = project.getVersion();
		String fileName = artifactId + "-" + artifactVersion + ".xml";

		buildDirectory.mkdirs();
		File pluginFile = new File(buildDirectory, fileName);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();

		Element pluginElement = document.createElementNS(PLUGINS_NAMESPACE, "plugins:plugin");
		pluginElement.setAttribute("xmlns:xsi", XSI_NAMESPACE);
		pluginElement.setAttribute("xsi:schemaLocation", SCHEMA_LOCATION);
		document.appendChild(pluginElement);

		Element providerElement = document.createElement("provider");
		providerElement.setTextContent(provider);
		pluginElement.appendChild(providerElement);

		Element identifier = document.createElement("identifier");
		identifier.setTextContent(groupId + "." + artifactId);
		pluginElement.appendChild(identifier);

		Element version = document.createElement("version");
		version.setTextContent(artifactVersion);
		pluginElement.appendChild(version);

		Element name = document.createElement("name");
		name.setTextContent(project.getName());
		pluginElement.appendChild(name);

		Element description = document.createElement("description");
		description.setTextContent(project.getDescription());
		pluginElement.appendChild(description);

		Element enabled = document.createElement("enabled");
		enabled.setTextContent("true");
		pluginElement.appendChild(enabled);

		Element repositories = document.createElement("repositories");
		pluginElement.appendChild(repositories);

		for (String repository : getRepositories("runtime")) {
			Element repositoryElement = document.createElement("repository");
			repositoryElement.setTextContent(repository);
			repositories.appendChild(repositoryElement);
		}

		Element profile = document.createElement("profile");
		pluginElement.appendChild(profile);

		for (Object artifact : project.getDependencyArtifacts()) {
			Artifact dependency = (Artifact) artifact;
			Element dependencyElement = document.createElement("dependency");
			Element groupIdElement = document.createElement("groupId");
			groupIdElement.setTextContent(dependency.getGroupId());
			dependencyElement.appendChild(groupIdElement);
			Element artifactIdElement = document.createElement("artifactId");
			artifactIdElement.setTextContent(dependency.getArtifactId());
			dependencyElement.appendChild(artifactIdElement);
			Element versionElement = document.createElement("version");
			versionElement.setTextContent(dependency.getVersion());
			dependencyElement.appendChild(versionElement);
			profile.appendChild(dependencyElement);
		}

		Element compatibility = document.createElement("compatibility");
		pluginElement.appendChild(compatibility);

		Element application = document.createElement("application");
		compatibility.appendChild(application);

		Element applicationVersion = document.createElement("version");
		applicationVersion.setTextContent(tavernaVersion);
		application.appendChild(applicationVersion);

		writeDocument(document, pluginFile);

		return pluginFile;
	}

	/**
	 * Writes the plugin document to the file.
	 * 
	 * @param document
	 *            the plugin document
	 * @param pluginFile
	 *            the file to write to
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	private void writeDocument(Document document, File pluginFile) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new DOMSource(document), new StreamResult(pluginFile));
	}

	/**
	 * Returns the repositories required to resolve the dependencies for the
	 * specified scope.
	 * 
	 * @param scope
	 *            the scope of the dependency artifacts
	 * @return the repositories required to resolve the dependencies for the
	 *         specified scope
	 * @throws ArtifactResolutionException
	 *             if an artifact cannot be resolved
	 * @throws ArtifactNotFoundException
	 *             if an artifact cannot be found
	 */
	private List<String> getRepositories(String scope) throws ArtifactResolutionException,
			ArtifactNotFoundException {
		List<String> repositories = new ArrayList<String>();

		ArtifactFilter filter = new ScopeArtifactFilter(scope);
		ArtifactResolutionResult result = artifactResolver.resolveTransitively(project
				.getDependencyArtifacts(), project.getArtifact(), project.getManagedVersionMap(),
				session.getLocalRepository(),
				project.getRemoteArtifactRepositories(),
				artifactMetadataSource, filter);

		for (Object element : result.getArtifactResolutionNodes()) {
			ResolutionNode node = (ResolutionNode) element;
			for (Object remoteRepository : node.getRemoteRepositories()) {
				ArtifactRepository repository = (ArtifactRepository) remoteRepository;
				String repositoryUrl = repository.getUrl();
				if (!repositories.contains(repositoryUrl)) {
					repositories.add(repositoryUrl);
				}
			}
		}

		return repositories;
	}

}
