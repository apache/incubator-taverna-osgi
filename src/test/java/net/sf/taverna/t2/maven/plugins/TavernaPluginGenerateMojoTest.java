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
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
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
	@Before
	public void setUp() throws Exception {
		super.setUp();

		File pluginXml = new File( getBasedir(), "src/test/resources/unit/plugin-config.xml" );
        tavernaPluginGenerateMojo = (TavernaPluginGenerateMojo) lookupMojo( "plugin-generate", pluginXml );

		MavenProject mavenProject = (MavenProject) getVariableValueFromObject(tavernaPluginGenerateMojo, "project");

		Artifact artifact = new DefaultArtifact("net.sf.taverna.t2", "example-plugin", VersionRange
				.createFromVersion("0.1.0"), "compile", "jar", "", null);
        artifact.setRepository(new DefaultArtifactRepository("id1",
				"http://www.mygrid.org.uk/maven/repository", new DefaultRepositoryLayout()));
		mavenProject.setArtifact(artifact);

        
		Artifact dependency = new DefaultArtifact("com.example.test", "test-artifact", VersionRange
				.createFromVersion("1.3.5"), "compile", "jar", "", null);
		dependency.setGroupId("com.example.test");
		dependency.setArtifactId("test-artifact");
		dependency.setVersion("1.3.5");
		dependency.setRepository(new DefaultArtifactRepository("id2",
				"http://www.example.com/maven/repository", new DefaultRepositoryLayout()));
		mavenProject.setDependencyArtifacts(Collections.singleton(dependency));

		ArtifactRepository localRepository = new StubArtifactRepository(getBasedir());
		MavenSession session = new MavenSession(container, null, localRepository, null, null, null, null, null, null, null);
		setVariableValueToObject(tavernaPluginGenerateMojo, "session", session);
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
