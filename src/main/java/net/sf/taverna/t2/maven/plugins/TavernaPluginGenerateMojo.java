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
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Organization;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.sonatype.aether.RepositorySystemSession;

import uk.org.taverna.commons.plugin.xml.jaxb.PluginInfo;
import uk.org.taverna.commons.profile.xml.jaxb.BundleInfo;

/**
 * Generates a Taverna plugin definition file.
 *
 * @author David Withers
 */
@Mojo(name = "plugin-generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TavernaPluginGenerateMojo extends AbstractMojo {

	public static final String PLUGIN_FILE = "plugin.xml";

	public static final String META_INF_TAVERNA = "META-INF/taverna";

	public static final String SCHEMA_LOCATION = "http://ns.taverna.org.uk/2013/application/plugin http://localhost/2013/application/plugin/ApplicationPlugin.xsd";

	@Component
	private MavenProject project;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	protected File outputDirectory;

	@Parameter(defaultValue = "${project.description}", required = true)
	protected String description;

	@Parameter(defaultValue = "${project.organization}", required = true)
	protected Organization organization;

	private MavenOsgiUtils osgiUtils;

	private Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			osgiUtils = new MavenOsgiUtils(project, repositorySystemSession,
					projectDependenciesResolver, getLog());
			createPluginDefinition();
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error generating Taverna plugin", e);
		}
	}

	/**
	 * Generates the Taverna plugin definition file.
	 *
	 * @return the <code>File</code> that the Taverna plugin definition has been written to
	 * @throws JAXBException
	 * @throws MojoExecutionException
	 */
	private File createPluginDefinition() throws JAXBException, MojoExecutionException {
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String version = maven2OsgiConverter.getVersion(project.getVersion());
		if (version.endsWith("SNAPSHOT")) {
			version = version.substring(0, version.indexOf("SNAPSHOT")) + Utils.timestamp();
		}

		File pluginDirectory = new File(outputDirectory, META_INF_TAVERNA);
		pluginDirectory.mkdirs();
		File pluginFile = new File(pluginDirectory, PLUGIN_FILE);

		PluginInfo pluginInfo = new PluginInfo();
		pluginInfo.setId(groupId + "." + artifactId);
		pluginInfo.setName(project.getName());
		pluginInfo.setVersion(version);
		pluginInfo.setDescription(description);
		pluginInfo.setOrganization(organization.getName());
		System.out.println(organization.getName());
		System.out.println(project.getOrganization().getName());

		Set<BundleArtifact> bundleDependencies = osgiUtils.getBundleDependencies(
				Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);

		List<BundleInfo> runtimeBundles = osgiUtils.getBundles(bundleDependencies);
		if (!runtimeBundles.isEmpty()) {
			List<BundleInfo> bundles = pluginInfo.getBundle();
			for (BundleInfo bundle : runtimeBundles) {
				bundles.add(bundle);
			}
		}

		JAXBContext jaxbContext = JAXBContext.newInstance(PluginInfo.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SCHEMA_LOCATION);
		marshaller.marshal(pluginInfo, pluginFile);

		return pluginFile;
	}

}
