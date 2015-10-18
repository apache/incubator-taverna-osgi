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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for TavernaPluginGenerateMojo.
 *
 * @author David Withers
 */
public class TavernaPluginGenerateMojoTest extends AbstractMojoTestCase {

	private TavernaPluginGenerateMojo tavernaPluginGenerateMojo;

	/**
	 * @throws java.lang.Exception
	 */
	@Ignore
	public void setUp() throws Exception {
//		super.setUp();
//
//		File pluginXml = new File( getBasedir(), "src/test/resources/unit/plugin-config.xml" );
//        tavernaPluginGenerateMojo = (TavernaPluginGenerateMojo) lookupMojo( "plugin-generate", pluginXml );
//
//		MavenProject mavenProject = (MavenProject) getVariableValueFromObject(tavernaPluginGenerateMojo, "project");
//
//		Artifact artifact = new DefaultArtifact("net.sf.taverna.t2", "example-plugin", VersionRange
//				.createFromVersion("0.1.0"), "compile", "jar", "", null);
//        artifact.setRepository(new DefaultArtifactRepository("id1",
//				"http://www.mygrid.org.uk/maven/repository", new DefaultRepositoryLayout()));
//		mavenProject.setArtifact(artifact);
//
//
//		Artifact dependency = new DefaultArtifact("com.example.test", "test-artifact", VersionRange
//				.createFromVersion("1.3.5"), "compile", "jar", "", null);
//		dependency.setGroupId("com.example.test");
//		dependency.setArtifactId("test-artifact");
//		dependency.setVersion("1.3.5");
//		dependency.setRepository(new DefaultArtifactRepository("id2",
//				"http://www.example.com/maven/repository", new DefaultRepositoryLayout()));
//		mavenProject.setDependencyArtifacts(Collections.singleton(dependency));
//
//		MavenSession session = new MavenSession(getContainer(), (RepositorySystemSession) null, (MavenExecutionRequest) null, (MavenExecutionResult) null);
//		setVariableValueToObject(tavernaPluginGenerateMojo, "session", session);
	}

	/**
	 * Test method for
	 * {@link net.sf.taverna.t2.maven.plugins.TavernaPluginGenerateMojo#execute()}
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testExecute() throws Exception {
//		tavernaPluginGenerateMojo.execute();
	}

}
