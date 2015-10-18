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
import org.apache.taverna.plugin.xml.jaxb.PluginInfo;
import org.apache.taverna.profile.xml.jaxb.BundleInfo;
import org.eclipse.aether.RepositorySystemSession;

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
