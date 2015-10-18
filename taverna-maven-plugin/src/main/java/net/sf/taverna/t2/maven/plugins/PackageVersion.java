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
