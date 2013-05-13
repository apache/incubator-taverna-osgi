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
import java.util.Set;

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
import org.sonatype.aether.RepositorySystemSession;

/**
 * Prepares the plugin OSGi bundles.
 *
 * @author David Withers
 */
@Mojo(name = "plugin-prepare-bundles", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TavernaPluginPrepareBundlesMojo extends AbstractMojo {

	@Component
	private MavenProject project;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	/**
	 * The directory where the plugin OSGi bundles file will be put.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	protected File outputDirectory;

	private MavenOsgiUtils osgiUtils;

	public void execute() throws MojoExecutionException, MojoFailureException {
		osgiUtils = new MavenOsgiUtils(project, repositorySystemSession,
				projectDependenciesResolver);
		outputDirectory.mkdirs();

		Set<BundleArtifact> bundleDependencies = osgiUtils.getBundleDependencies(
				Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
		try {
			for (BundleArtifact bundleArtifact : bundleDependencies) {
				Artifact artifact = bundleArtifact.getArtifact();
				FileUtils.copyFileToDirectory(bundleArtifact.getArtifact().getFile(), new File(
						outputDirectory, artifact.getGroupId()));
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error copying dependecies to archive directory", e);
		}
	}

}
