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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.apache.taverna.profile.xml.jaxb.ApplicationProfile;
import org.apache.taverna.profile.xml.jaxb.BundleInfo;
import org.apache.taverna.profile.xml.jaxb.UpdateSite;
import org.apache.taverna.versions.xml.jaxb.Version;
import org.apache.taverna.versions.xml.jaxb.Versions;

/**
 * Deploys the application profile using <code>scp</code> or <code>file</code> protocol to the site
 * URL specified in the <code>&lt;distributionManagement&gt;</code> section of the POM.
 *
 * @author David Withers
 */
@Mojo(name = "profile-deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class TavernaProfileDeployMojo extends AbstractDeployMojo {

	private static final String UPDATES_FILE = "updates.xml";

	private File tempDirectory;

	public void execute() throws MojoExecutionException {
		tempDirectory = new File(buildDirectory, TavernaProfileGenerateMojo.TAVERNA_TMP);
		tempDirectory.mkdirs();
		if (artifact == null) {
			throw new MojoExecutionException(
					"The application profile does not exist, please run taverna:profile-generate first");
		}

		File artifactFile = artifact.getFile();
		if (artifactFile == null) {
			throw new MojoExecutionException(
					"The application profile does not exist, please run taverna:profile-generate first");
		}

		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(ApplicationProfile.class, UpdateSite.class);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error setting up JAXB context ", e);
		}

		ApplicationProfile applicationProfile;
		try {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			applicationProfile = (ApplicationProfile) unmarshaller.unmarshal(artifactFile);
		} catch (JAXBException e) {
			throw new MojoExecutionException("Error reading " + artifactFile, e);
		}

		if (deploymentRepository == null) {
			throw new MojoExecutionException(
					"Missing repository information in the distribution management element in the project.");
		}

		String url = deploymentRepository.getUrl();
		String id = deploymentRepository.getId();

		if (url == null) {
			throw new MojoExecutionException(
					"The URL to the update site is missing in the project descriptor.");
		}
		getLog().debug("The Taverna application will be deployed to '" + url + "'");

		Repository repository = new Repository(id, url);

		// create the wagon
		Wagon wagon;
		try {
			wagon = wagonManager.getWagon(repository.getProtocol());
		} catch (UnsupportedProtocolException e) {
			throw new MojoExecutionException("Unsupported protocol: '" + repository.getProtocol()
					+ "'", e);
		}

		Debug debug = new Debug();
		if (getLog().isDebugEnabled()) {
			wagon.addSessionListener(debug);
			wagon.addTransferListener(debug);
		}

		// connect to the update site
		try {
			wagon.connect(repository, wagonManager.getAuthenticationInfo(id),
					wagonManager.getProxy(repository.getProtocol()));
		} catch (ConnectionException e) {
			throw new MojoExecutionException("Error connecting to " + url, e);
		} catch (AuthenticationException e) {
			throw new MojoExecutionException("Authentication error connecting to " + url, e);
		}

		try {
			// upload the application profile to the update site
			String deployedProfileFile = "ApplicationProfile" + "-" + applicationProfile.getVersion()
					+ ".xml";
			Utils.uploadFile(artifactFile, deployedProfileFile, wagon, getLog());

			// fetch the applications file
			UpdateSite updateSite;
			File updatesFile = new File(tempDirectory, UPDATES_FILE);
			try {
				Utils.downloadFile(UPDATES_FILE, updatesFile, wagon, getLog());
				try {
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
					updateSite = (UpdateSite) unmarshaller
							.unmarshal(updatesFile);
				} catch (JAXBException e) {
					throw new MojoExecutionException("Error reading " + updatesFile, e);
				}
			} catch(ResourceDoesNotExistException e) {
				getLog().info("Creating new application versions file");
				updateSite = new UpdateSite();
				Versions versions = new Versions();
				versions.setId(applicationProfile.getId());
				versions.setName(applicationProfile.getName());
				updateSite.setVersions(versions);
			}

			Version latestVersion = updateSite.getVersions().getLatestVersion();
			if (latestVersion != null) {
				File latestProfileFile = new File(tempDirectory, "ApplicationProfile-" + latestVersion.getVersion()
						+ ".xml");
				try {
					Utils.downloadFile(latestVersion.getFile(), latestProfileFile, wagon, getLog());
				} catch (ResourceDoesNotExistException e) {
					throw new MojoExecutionException(latestVersion.getFile() + " does not exist", e);
				}
				ApplicationProfile latestProfile;
				try {
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
					latestProfile = (ApplicationProfile) unmarshaller.unmarshal(latestProfileFile);
				} catch (JAXBException e) {
					throw new MojoExecutionException("Error reading " + latestProfileFile, e);
				}
				Set<BundleInfo> requiredBundles = getRequiredBundles(latestProfile, applicationProfile);
				if (requiredBundles.isEmpty()) {
					getLog().warn("No new bundles to upload");
				} else {
					// upload new bundles to the update site
					uploadBundles(requiredBundles, wagon);
				}
			}

			if (addApplicationVersion(updateSite.getVersions(), applicationProfile,
					deployedProfileFile)) {
				// write the new application versions list
				try {
					Marshaller marshaller = jaxbContext.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, TavernaProfileGenerateMojo.SCHEMA_LOCATION);
					marshaller.marshal(updateSite, updatesFile);
				} catch (JAXBException e) {
					throw new MojoExecutionException("Error writing " + UPDATES_FILE, e);
				}

				// upload the new application versions list to the update site
				Utils.uploadFile(updatesFile, UPDATES_FILE, wagon, getLog());
			}
		}
		finally {
			disconnectWagon(wagon, debug);
		}
	}

	/**
	 * Adds an new application version to the application versions if the version doesn't already
	 * exist.
	 *
	 * @param applicationVersions
	 *            the ApplicationVersions document
	 * @param applicationProfile
	 *            the applicationProfile
	 * @param profileURL
	 * @return true if a new version was added to the ApplicationVersions document; false if the
	 *         version already exits
	 */
	private boolean addApplicationVersion(Versions applicationVersions,
			ApplicationProfile applicationProfile, String profileURL) {
		Version latestVersion = applicationVersions.getLatestVersion();
		if (latestVersion != null
				&& latestVersion.getVersion().equals(applicationProfile.getVersion())) {
			getLog().error(
					String.format("%1$s version %2$s has already been deployed",
							applicationProfile.getName(), applicationProfile.getVersion()));
			return false;
		}

		Version newApplicationVersion = new Version();
		newApplicationVersion.setVersion(applicationProfile.getVersion());
		newApplicationVersion.setFile(profileURL);

		getLog().info(
				String.format("Adding %1$s version %2$s", applicationProfile.getName(),
						applicationProfile.getVersion()));
		if (applicationVersions.getLatestVersion() != null) {
			applicationVersions.getPreviousVersion().add(applicationVersions.getLatestVersion());
		}
		applicationVersions.setLatestVersion(newApplicationVersion);
		return true;
	}

	/**
	 * @param requiredBundles
	 * @throws MojoExecutionException
	 */
	private void uploadBundles(Set<BundleInfo> requiredBundles, Wagon wagon) throws MojoExecutionException {
		File libDirectory = new File(tempDirectory, "lib");
		for (BundleInfo bundle : requiredBundles) {
			Utils.uploadFile(new File(libDirectory, bundle.getFileName()), "lib/" + bundle.getFileName(), wagon, getLog());
		}
	}

	private Set<BundleInfo> getRequiredBundles(ApplicationProfile currentProfile,
			ApplicationProfile newProfile) {
		Set<BundleInfo> requiredBundles = new HashSet<BundleInfo>();
		Map<String, BundleInfo> currentBundles = new HashMap<String, BundleInfo>();
		for (BundleInfo bundle : currentProfile.getBundle()) {
			currentBundles.put(bundle.getSymbolicName(), bundle);
		}
		for (BundleInfo bundle : newProfile.getBundle()) {
			if (currentBundles.containsKey(bundle.getSymbolicName())) {
				BundleInfo currentBundle = currentBundles.get(bundle.getSymbolicName());
				if (!bundle.getVersion().equals(currentBundle.getVersion())) {
					requiredBundles.add(bundle);
				}
			} else {
				requiredBundles.add(bundle);
			}
		}
		return requiredBundles;
	}

}
