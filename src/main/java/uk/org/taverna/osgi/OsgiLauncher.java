/*******************************************************************************
 * Copyright (C) 2012 The University of Manchester
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
package uk.org.taverna.osgi;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * OSGi Framework launcher.
 *
 * Handles loading and starting of OSGi bundles and Spring DM managed services.
 *
 * An implementation of an OSGi Service Platform Release 4.1 (or higher) must be available on the
 * classpath.
 *
 * @author David Withers
 */
public class OsgiLauncher {

	/**
	 * Default boot delegation packages.
	 */
	public static final String DEFAULT_BOOT_DELEGATION_PACKAGES = "sun.*,com.sun.*,java.*";

	/**
	 * Default system packages.
	 */
	public static final String DEFAULT_SYSTEM_PACKAGES = "com.sun.org.apache.xml.internal.utils";

	private static final Logger logger = Logger.getLogger(OsgiLauncher.class.getName());
	private static final long timeoutSeconds = 30;

	private Framework framework;
	private BundleContext context;

	private Map<String, String> frameworkConfiguration = new HashMap<String, String>();
	private List<URI> bundlesToInstall = new ArrayList<URI>();
	private List<Bundle> installedBundles = new ArrayList<Bundle>();
	private Set<String> startedSpringContexts = new HashSet<String>();
	private Bundle springOsgiExtender;

	/**
	 * Constructs an <code>OsgiLauncher</code> that loads bundles from a directory.
	 *
	 * Any file in the specified directory with a .jar extension will be loaded when the framework
	 * is started.
	 *
	 * @param storageDirectory
	 *            persistent storage area used by the framework
	 * @param storageDirectory
	 *            the directory containing bundles to load
	 */
	public OsgiLauncher(File storageDirectory, File bundleDir) {
		this(storageDirectory, Arrays.asList(bundleDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		})));
	}

	/**
	 * Constructs an <code>OsgiLauncher</code> that loads the specified bundles.
	 *
	 * @param storageDirectory
	 *            persistent storage area used by the framework
	 * @param bundleFiles
	 *            bundles to load
	 */
	public OsgiLauncher(File appDirectory, List<File> bundleFiles) {
		for (File bundleFile : bundleFiles) {
			bundlesToInstall.add(bundleFile.toURI());
		}
		setStorageDirectory(appDirectory);
		setCleanStorageDirectory(true);
		setBootDelegationPackages(DEFAULT_BOOT_DELEGATION_PACKAGES);
		setSystemPackages(DEFAULT_SYSTEM_PACKAGES);
	}

	public void setStorageDirectory(File storageDirectory) {
		frameworkConfiguration.put(Constants.FRAMEWORK_STORAGE, storageDirectory.getAbsolutePath());
	}

	/**
	 * Starts the OSGi framework, installs and starts the bundles.
	 *
	 * @throws BundleException
	 *             if the framework could not be started
	 */
	public void start() throws BundleException {
		logger.info("Loading the OSGi Framework Factory");
		FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator()
				.next();

		logger.info("Creating the OSGi Framework");
		framework = frameworkFactory.newFramework(frameworkConfiguration);
		logger.info("Starting the OSGi Framework");
		framework.start();

		context = framework.getBundleContext();
		context.addServiceListener(new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				ServiceReference serviceReference = event.getServiceReference();
				if (event.getType() == ServiceEvent.REGISTERED) {
					Object property = serviceReference
							.getProperty("org.springframework.context.service.name");
					if (property != null) {
						addStartedSpringContext(property.toString());
					}
				}
				logger.fine((event.getType() == ServiceEvent.REGISTERED ? "Registering : "
						: "Unregistering : ") + serviceReference);
			}
		});

		installedBundles = installBundles(bundlesToInstall);

		List<Bundle> bundlesToStart = new ArrayList<Bundle>();
		for (Bundle bundle : installedBundles) {
			if ("org.springframework.osgi.extender".equals(bundle.getSymbolicName())) {
				springOsgiExtender = bundle;
			} else {
				bundlesToStart.add(bundle);
			}
		}
		startBundles(bundlesToStart);
	}

	/**
	 * Starts SpringDM managed services.
	 *
	 * @param waitForServices
	 *            if true waits for services to start before returning
	 * @throws BundleException
	 *             if a service could not be started
	 */
	public void startServices(boolean waitForServices) throws BundleException {
		if (springOsgiExtender != null) {
			logger.info("Starting Spring OSGi Extender");
			springOsgiExtender.start();
			if (waitForServices) {
				logger.info("Waiting for spring contexts to be started");
				for (Bundle bundle : installedBundles) {
					if (bundle.getState() == Bundle.ACTIVE) {
						if (hasSpringContext(bundle)) {
							logger.fine("Waiting for " + bundle.getSymbolicName());
							waitForSpringContext(context, bundle.getSymbolicName(), timeoutSeconds);
						}
					}
				}
			}
		}
	}

	/**
	 * Stops the OSGI framework.
	 *
	 * @throws BundleException
	 *             if the framework has not been started
	 * @throws InterruptedException
	 *             if the thread is interrupted while the framework is stopping
	 */
	public void stop() throws BundleException, InterruptedException {
		if (framework == null || framework.getState() != Bundle.ACTIVE) {
			throw new BundleException("Framework not started");
		}
		framework.stop();
		framework.waitForStop(0);
		context = null;
	}

	/**
	 * Installs the bundles specified by the URIs into the framework.
	 *
	 * @param bundlesURIs
	 *            the URIs of the bundles to install
	 * @return the installed bundles
	 * @throws BundleException
	 *             if a bundle could not be installed
	 */
	public List<Bundle> installBundles(List<URI> bundlesURIs) throws BundleException {
		List<Bundle> installedBundles = new ArrayList<Bundle>();
		logger.info("Installing bundles into the OSGi Framework");
		for (URI bundleURI : bundlesURIs) {
			installedBundles.add(installBundle(bundleURI));
		}
		return installedBundles;
	}

	/**
	 * Installs the bundle specified by the URI into the framework.
	 *
	 * @param bundleURI
	 *            the URI of the bundle to install
	 * @return the installed bundle
	 * @throws BundleException
	 *             if the bundle could not be installed
	 */
	public Bundle installBundle(URI bundleURI) throws BundleException {
		logger.fine("Installing bundle " + bundleURI);
		return context.installBundle(bundleURI.toASCIIString());
	}

	/**
	 * Starts the bundles.
	 *
	 * If a bundle is a fragment bundle that bundle is not started.
	 *
	 * @param bundles
	 *            the bundles to start
	 * @throws BundleException
	 *             if a bundle could not be started
	 */
	public void startBundles(List<Bundle> bundles) throws BundleException {
		logger.info("Starting bundles in the OSGi Framework");
		for (Bundle bundle : bundles) {
			startBundle(bundle);
		}
	}

	/**
	 * Starts the bundle.
	 *
	 * If the bundle is a fragment bundle the bundle is not started.
	 *
	 * @param bundle
	 *            the bundle to start
	 * @throws BundleException
	 *             if the bundle could not be started
	 */
	public void startBundle(Bundle bundle) throws BundleException {
		if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
			logger.fine("Starting bundle " + bundle);
			bundle.start();
		}
	}

	/**
	 * Returns the context. Returns <code>null</code> if the framework is not started.
	 *
	 * @return the context
	 */
	public BundleContext getContext() {
		return context;
	}

	/**
	 * Adds boot delegation packages.
	 *
	 * Multiple packages must be separated by a ','.
	 *
	 * @param additionalBootDelegationPackages
	 *            boot delegation packages to add
	 */
	public void addBootDelegationPackages(String additionalBootDelegationPackages) {
		String bootDelegationPackages = frameworkConfiguration
				.get(Constants.FRAMEWORK_BOOTDELEGATION);
		if (bootDelegationPackages == null || bootDelegationPackages.isEmpty()) {
			bootDelegationPackages = additionalBootDelegationPackages;
		} else {
			bootDelegationPackages = bootDelegationPackages + ","
					+ additionalBootDelegationPackages;
		}
		frameworkConfiguration.put(Constants.FRAMEWORK_BOOTDELEGATION, bootDelegationPackages);
	}

	/**
	 * Sets the boot delegation packages.
	 *
	 * Multiple packages must be separated by a ','.
	 *
	 * @param bootDelegationPackages
	 *            the boot delegation packages
	 */
	public void setBootDelegationPackages(String bootDelegationPackages) {
		frameworkConfiguration.put(Constants.FRAMEWORK_BOOTDELEGATION, bootDelegationPackages);
	}

	/**
	 * Adds system packages.
	 *
	 * Multiple packages must be separated by a ','.
	 *
	 * @param additionalSystemPackages
	 *            system packages to add
	 */
	public void addSystemPackages(String additionalSystemPackages) {
		String systemPackages = frameworkConfiguration
				.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		if (systemPackages == null || systemPackages.isEmpty()) {
			systemPackages = additionalSystemPackages;
		} else {
			systemPackages = systemPackages + "," + additionalSystemPackages;
		}
		frameworkConfiguration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages);
	}

	/**
	 * Sets the system packages.
	 *
	 * Multiple packages must be separated by a ','.
	 *
	 * @param systemPackages
	 *            the system packages
	 */
	public void setSystemPackages(String systemPackages) {
		frameworkConfiguration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages);
	}

	/**
	 * Set whether the storage directory should be cleaned on startup.
	 *
	 * @param cleanStorageDirectory
	 *            whether the storage directory should be cleaned on startup
	 */
	public void setCleanStorageDirectory(boolean cleanStorageDirectory) {
		if (cleanStorageDirectory) {
			frameworkConfiguration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
					Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		} else {
			frameworkConfiguration.remove(Constants.FRAMEWORK_STORAGE_CLEAN);
		}
	}

	/**
	 * Returns true if a bundle contains spring context files.
	 *
	 * @param bundle
	 *            the bundle to check
	 * @return true if a bundle contains spring context files
	 */
	private boolean hasSpringContext(Bundle bundle) {
		String springFilesLocation = "META-INF/spring";
		// check for custom spring files location
		Dictionary headers = bundle.getHeaders();
		if (headers != null) {
			Object header = headers.get("Spring-Context");
			if (header != null) {
				springFilesLocation = header.toString().trim();
			}
		}
		Enumeration springFiles = bundle.findEntries(springFilesLocation, "*.xml", false);
		return springFiles != null && springFiles.hasMoreElements();
	}

	private synchronized void waitForSpringContext(BundleContext context, String springContext,
			long timeoutSeconds) {
		long timeLeftToWait = timeoutSeconds * 1000;
		long startTime = System.currentTimeMillis();

		while (!startedSpringContexts.contains(springContext) && timeLeftToWait > 0) {
			try {
				wait(timeLeftToWait);
			} catch (InterruptedException e) {
			}
			timeLeftToWait = timeLeftToWait - (System.currentTimeMillis() - startTime);
		}
	}

	private synchronized void addStartedSpringContext(String springContext) {
		startedSpringContexts.add(springContext);
		notifyAll();
	}

}
