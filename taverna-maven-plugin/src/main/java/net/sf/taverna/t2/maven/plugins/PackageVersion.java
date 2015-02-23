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

import org.apache.maven.artifact.Artifact;

import aQute.bnd.version.VersionRange;

/**
 *
 *
 * @author David Withers
 */
public class PackageVersion {

	private VersionRange versionRange;
	private Artifact artifact;
	private boolean optional;

	public PackageVersion(VersionRange versionRange, Artifact artifact) {
		this(versionRange, artifact, false);
	}

	public PackageVersion(VersionRange versionRange, Artifact artifact, boolean optional) {
		this.versionRange = versionRange;
		this.artifact = artifact;
		this.optional = optional;
	}

	@Override
	public String toString() {
		return versionRange + (optional ? "" : "") + "(from " + artifact.getId() + ")";
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(VersionRange versionRange) {
		this.versionRange = versionRange;
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

}
