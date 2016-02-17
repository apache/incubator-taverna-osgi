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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Abstract Mojo for deploying artifacts.
 *
 * @author David Withers
 */
public abstract class AbstractDeployMojo extends AbstractWagonMojo {

	@Component
	protected MavenProject project;

	/**
	 * Directory containing the generated Taverna plugin.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	protected File buildDirectory;

	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	protected File outputDirectory;

	@Parameter(defaultValue = "${project.artifact}", required = true, readonly = true)
	protected Artifact artifact;

	@Parameter(defaultValue = "${project.distributionManagementArtifactRepository}", required = true, readonly = true)
	protected ArtifactRepository deploymentRepository;

}
