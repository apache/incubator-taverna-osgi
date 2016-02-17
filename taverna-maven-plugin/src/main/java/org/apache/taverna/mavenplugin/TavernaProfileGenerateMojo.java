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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
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
import org.apache.taverna.profile.xml.jaxb.ApplicationProfile;
import org.apache.taverna.profile.xml.jaxb.BundleInfo;
import org.apache.taverna.profile.xml.jaxb.FrameworkConfiguration;
import org.apache.taverna.profile.xml.jaxb.Updates;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Generates an application profile file.
 *
 * @author David Withers
 */
@Mojo(name = "profile-generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TavernaProfileGenerateMojo extends AbstractMojo {

	public static final String SYSTEM_PACKAGES = "org.osgi.framework.system.packages.extra";

	public static final String SCHEMA_LOCATION = "http://ns.taverna.org.uk/2013/application/profile http://localhost/2013/application/profile/ApplicationProfile.xsd";

	public static final String TAVERNA_TMP = "taverna-tmp";

	public static final String APPLICATION_PROFILE_FILE = "ApplicationProfile.xml";

	@Component
	private MavenProject project;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	/**
	 * The directory where the generated <code>ApplicationProfile.xml</code> file will be put.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	protected File outputDirectory;

	@Parameter(defaultValue = "SNAPSHOT")
	private String buildNumber;

	@Parameter
	private List<FrameworkConfiguration> frameworkConfigurations;

	@Parameter(required = true)
	private String updateSite;

	@Parameter(defaultValue = "updates.xml")
	private String updatesFile;

	@Parameter(defaultValue = "lib")
	private String libDirectory;

	@Parameter(required = true)
	private String pluginSite;

	@Parameter(defaultValue = "plugins.xml")
	private String pluginsFile;

	private Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();

	private MavenOsgiUtils osgiUtils;

	private File tempDirectory;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			osgiUtils = new MavenOsgiUtils(project, repositorySystemSession,
					projectDependenciesResolver, getSystemPackages(), getLog());
			tempDirectory = new File(outputDirectory, TAVERNA_TMP);

			File profileFile = createApplicationProfile();
			project.getArtifact().setFile(profileFile);

			copyDependencies();

		} catch (JAXBException e) {
			throw new MojoExecutionException("Error generating application profile", e);
		}
	}

	private void copyDependencies() throws MojoExecutionException {
		File libDirectory = new File(tempDirectory, "lib");
		libDirectory.mkdirs();

		try {
			for (Artifact artifact : project.getArtifacts()) {
				FileUtils.copyFileToDirectory(artifact.getFile(),
						new File(libDirectory, artifact.getGroupId()));
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error copying dependecies to lib directory", e);
		}

	}

	/**
	 * Generates the application profile file.
	 *
	 * @return the <code>File</code> that the application profile has been written to
	 * @throws JAXBException
	 *             if the application profile cannot be created
	 * @throws MojoExecutionException
	 */
	private File createApplicationProfile() throws JAXBException, MojoExecutionException {
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String version = maven2OsgiConverter.getVersion(project.getVersion());
		if (version.endsWith("SNAPSHOT")) {
			version = version.substring(0, version.indexOf("SNAPSHOT")) + buildNumber;
		}

		tempDirectory.mkdirs();
		File applicationProfileFile = new File(tempDirectory, APPLICATION_PROFILE_FILE);

		ApplicationProfile applicationProfile = new ApplicationProfile();
		applicationProfile.setId(groupId + "." + artifactId);
		applicationProfile.setName(project.getName());
		applicationProfile.setVersion(version);

		Updates updates = new Updates();
		updates.setUpdateSite(updateSite);
		updates.setUpdatesFile(updatesFile);
		updates.setLibDirectory(libDirectory);
		updates.setPluginSite(pluginSite);
		updates.setPluginsFile(pluginsFile);
		applicationProfile.setUpdates(updates);

		List<FrameworkConfiguration> frameworkConfiguration = applicationProfile
				.getFrameworkConfiguration();
		for (FrameworkConfiguration configuration : frameworkConfigurations) {
			frameworkConfiguration.add(configuration);
		}

		Set<BundleArtifact> bundleDependencies = osgiUtils.getBundleDependencies(
				Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
		List<BundleInfo> runtimeBundles = osgiUtils.getBundles(bundleDependencies);
		if (!runtimeBundles.isEmpty()) {
			List<BundleInfo> bundles = applicationProfile.getBundle();
			for (BundleInfo bundle : runtimeBundles) {
				bundles.add(bundle);
			}
		}

		JAXBContext jaxbContext = JAXBContext.newInstance(ApplicationProfile.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SCHEMA_LOCATION);
		marshaller.marshal(applicationProfile, applicationProfileFile);

		return applicationProfileFile;
	}

	private Set<String> getSystemPackages() {
		Set<String> systemPackages = new HashSet<String>();
		if (frameworkConfigurations != null) {
			for (FrameworkConfiguration configuration : frameworkConfigurations) {
				if (SYSTEM_PACKAGES.equals(configuration.getName())) {
					String packagesString = configuration.getValue();
					if (packagesString != null) {
						String[] packages = packagesString.split(",");
						for (String packageString : packages) {
							String[] packageProperties = packageString.split(";");
							if (packageProperties.length > 0) {
								systemPackages.add(packageProperties[0]);
							}
						}
					}
				}
			}
		}
		return systemPackages;
	}

}
