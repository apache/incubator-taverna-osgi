/*******************************************************************************
 * Copyright (C) 2013 The University of Manchester
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;

/**
 *
 *
 * @author David Withers
 */
public abstract class AbstractDeployMojo extends AbstractMojo {

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

	@Component
	protected WagonManager wagonManager;

	/**
	 * Disconnect the wagon.
	 *
	 * @param wagon
	 *            the wagon to disconnect
	 * @param debug
	 */
	protected void disconnectWagon(Wagon wagon, Debug debug) {
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


}
