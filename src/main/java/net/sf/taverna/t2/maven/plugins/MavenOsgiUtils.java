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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;

import uk.org.taverna.commons.profile.xml.jaxb.BundleInfo;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

/**
 * @author David Withers
 */
public class MavenOsgiUtils {

	private final MavenProject project;
	private final RepositorySystemSession repositorySystemSession;
	private final ProjectDependenciesResolver projectDependenciesResolver;
	private final Log log;

	public MavenOsgiUtils(MavenProject project, RepositorySystemSession repositorySystemSession,
			ProjectDependenciesResolver projectDependenciesResolver, Log log) {
		this.project = project;
		this.repositorySystemSession = repositorySystemSession;
		this.projectDependenciesResolver = projectDependenciesResolver;
		this.log = log;
	}

	public Set<BundleArtifact> getBundleDependencies(String... scopes)
			throws MojoExecutionException {
		ScopeDependencyFilter scopeFilter = new ScopeDependencyFilter(Arrays.asList(scopes), null);

		DefaultDependencyResolutionRequest dependencyResolutionRequest = new DefaultDependencyResolutionRequest(
				project, repositorySystemSession);
		dependencyResolutionRequest.setResolutionFilter(scopeFilter);

		DependencyResolutionResult dependencyResolutionResult;
		try {
			dependencyResolutionResult = projectDependenciesResolver
					.resolve(dependencyResolutionRequest);
		} catch (DependencyResolutionException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}

		DependencyNode dependencyGraph = dependencyResolutionResult.getDependencyGraph();
		if (dependencyGraph != null) {
			checkBundleDependencies(dependencyGraph.getChildren());
			return getBundleArtifacts(dependencyGraph.getChildren());
		} else {
			return new HashSet<BundleArtifact>();
		}
	}

	public Set<BundleArtifact> getBundleArtifacts(List<DependencyNode> nodes) {
		Set<BundleArtifact> bundleArtifacts = new HashSet<BundleArtifact>();
		for (DependencyNode node : nodes) {
			Artifact artifact = RepositoryUtils.toArtifact(node.getDependency().getArtifact());
			String symbolicName = getManifestAttribute(artifact, Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName != null) {
				String version = getManifestAttribute(artifact, Constants.BUNDLE_VERSION);
				bundleArtifacts.add(new BundleArtifact(artifact, symbolicName, version));
				bundleArtifacts.addAll(getBundleArtifacts(node.getChildren()));
			} else {
				log.warn("Not an OSGi bundle : " + artifact.getId());
			}
		}
		return bundleArtifacts;
	}

	public Set<Artifact> getArtifacts(List<DependencyNode> nodes) {
		Set<Artifact> artifacts = new HashSet<Artifact>();
		for (DependencyNode node : nodes) {
			Artifact artifact = RepositoryUtils.toArtifact(node.getDependency().getArtifact());
			if (isBundle(artifact)) {
				artifacts.add(artifact);
				artifacts.addAll(getArtifacts(node.getChildren()));
			}
		}
		return artifacts;
	}

	public List<BundleInfo> getBundles(Set<BundleArtifact> bundleDependencies)
			throws MojoExecutionException {
		List<BundleInfo> bundles = new ArrayList<BundleInfo>();
		for (BundleArtifact bundleArtifact : bundleDependencies) {
			Artifact artifact = bundleArtifact.getArtifact();
			BundleInfo bundle = new BundleInfo();
			bundle.setSymbolicName(bundleArtifact.getSymbolicName());
			bundle.setVersion(bundleArtifact.getVersion());
			bundle.setFileName(new File(artifact.getGroupId(), artifact.getFile().getName())
					.getPath());
			bundles.add(bundle);
		}
		Collections.sort(bundles, new BundleComparator());
		return bundles;
	}

	public void checkBundleDependencies(List<DependencyNode> nodes) {
		Map<Artifact, Set<Package>> unresolvedArtifacts = new HashMap<Artifact, Set<Package>>();
		Set<Artifact> artifacts = getArtifacts(nodes);
		Map<String, Set<PackageVersion>> exports = calculatePackageVersions(artifacts, true);
		for (Artifact artifact : artifacts) {
			if (isBundle(artifact)) {
				Parameters imports = getManifestAttributeParameters(artifact,
						Constants.IMPORT_PACKAGE);
				if (imports != null) {
					for (String packageName : imports.keySet()) {
						boolean exportMissing = true;
						VersionRange importRange = null;
						Attrs attrs = imports.get(packageName);
						if (isOptional(attrs)) {
							exportMissing = false;
						} else if (packageName.startsWith("javax.") || packageName.startsWith("org.osgi.")) {
							exportMissing = false;
						} else if (attrs == null || attrs.get(Constants.VERSION_ATTRIBUTE) == null) {
							if (exports.containsKey(packageName)) {
								exportMissing = false;
							}
						} else {
							importRange = getVersionRange(attrs);
							if (exports.containsKey(packageName)) {
								for (PackageVersion exportVersion : exports.get(packageName)) {
									if (importRange.includes(exportVersion.getVersionRange()
											.getLow())) {
										exportMissing = false;
										break;
									}
								}
							}
						}
						if (exportMissing) {
							if (!unresolvedArtifacts.containsKey(artifact)) {
								unresolvedArtifacts.put(artifact, new HashSet<Package>());
							}
							unresolvedArtifacts.get(artifact).add(
									new Package(packageName, importRange));
						}
					}
				}
			}
		}
		for (Entry<Artifact, Set<Package>> unresolvedArtifact : unresolvedArtifacts.entrySet()) {
			log.warn("Bundle : " + unresolvedArtifact.getKey().getId()
					+ " has unresolved package dependencies:");
			for (Package unresolvedPackage : unresolvedArtifact.getValue()) {
				log.warn("    " + unresolvedPackage);
			}
		}
	}

	public Map<String, Set<PackageVersion>> calculatePackageVersions(Set<Artifact> artifacts,
			boolean export) {
		Map<String, Set<PackageVersion>> packageVersions = new HashMap<String, Set<PackageVersion>>();
		for (Artifact artifact : artifacts) {
			if (isBundle(artifact)) {
				Parameters exports = getManifestAttributeParameters(artifact,
						export ? Constants.EXPORT_PACKAGE : Constants.IMPORT_PACKAGE);
				if (exports != null) {
					for (String packageName : exports.keySet()) {
						if (!packageVersions.containsKey(packageName)) {
							packageVersions.put(packageName, new HashSet<PackageVersion>());
						}
						packageVersions.get(packageName).add(
								new PackageVersion(getVersionRange(exports.get(packageName)),
										artifact));
					}
				}
			}
		}
		return packageVersions;
	}

	public Version getVersion(Attrs attrs) {
		if (attrs == null) {
			return Version.LOWEST;
		}
		return Version.parseVersion(attrs.get(Constants.VERSION_ATTRIBUTE));
	}

	public VersionRange getVersionRange(Attrs attrs) {
		if (attrs == null) {
			return new VersionRange("0");
		}
		String version = attrs.get(Constants.VERSION_ATTRIBUTE);
		if (version == null) {
			return new VersionRange("0");
		}
		return new VersionRange(version);
	}

	public boolean isBundle(Artifact artifact) {
		return getManifestAttribute(artifact, Constants.BUNDLE_SYMBOLICNAME) != null;
	}

	public boolean isOptional(Attrs attrs) {
		return attrs != null && "optional".equals(attrs.get(Constants.RESOLUTION_DIRECTIVE));
	}

	public Parameters getManifestAttributeParameters(Artifact artifact, String attributeName) {
		String attributeValue = getManifestAttribute(artifact, attributeName);
		if (attributeValue != null) {
			return OSGiHeader.parseHeader(attributeValue);
		}
		return null;
	}

	public String getManifestAttribute(Artifact artifact, String attributeName) {
		Manifest manifest = getManifest(artifact);
		if (manifest != null) {
			Attributes mainAttributes = manifest.getMainAttributes();
			return mainAttributes.getValue(attributeName);
		}
		return null;
	}

	public Manifest getManifest(Artifact artifact) {
		if (artifact != null) {
			File file = artifact.getFile();
			if (file != null) {
				try {
					JarFile jarFile = new JarFile(artifact.getFile());
					return jarFile.getManifest();
				} catch (IOException e) {
					return null;
				}
			}
		}
		return null;
	}

	private final class BundleComparator implements Comparator<BundleInfo> {

		@Override
		public int compare(BundleInfo bundle1, BundleInfo bundle2) {
			return bundle1.getSymbolicName().compareTo(bundle2.getSymbolicName());
		}

	}

	// public static void main(String[] args) throws Exception {
	// MavenOsgiUtils mavenOsgiUtils = new MavenOsgiUtils();
	// Parameters exports = mavenOsgiUtils.getImports(new
	// File("/Users/david/Documents/workspace-trunk/taverna-plugin-impl/target/taverna-plugin-impl-0.1.0-SNAPSHOT.jar"));
	// for (String key : exports.keySet()) {
	// System.out.println(key + " " + mavenOsgiUtils.getVersionRange(exports.get(key)));
	// }
	// }
}
