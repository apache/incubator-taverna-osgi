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
package org.apache.taverna.plugin.impl;

import static org.apache.taverna.plugin.Plugin.State.STARTED;
import static org.apache.taverna.plugin.Plugin.State.STOPPED;
import static org.apache.taverna.plugin.Plugin.State.UNINSTALLED;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.taverna.plugin.Plugin;
import org.apache.taverna.plugin.PluginException;
import org.apache.taverna.plugin.xml.jaxb.PluginInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * @author David Withers
 */
public class PluginImpl implements Plugin {

	private static final Logger logger = Logger.getLogger(PluginImpl.class);

	private PluginManagerImpl pluginManager;

	private State state = UNINSTALLED;

	private File file;
	private String id, name, description, organization;
	private Version version;
	private Set<Bundle> bundles = new HashSet<Bundle>();

	public PluginImpl(PluginManagerImpl pluginManager, File file, PluginInfo pluginInfo) {
		this.pluginManager = pluginManager;
		this.file = file;
		id = pluginInfo.getId();
		name = pluginInfo.getName();
		description = pluginInfo.getDescription();
		organization = pluginInfo.getOrganization();
		version = Version.parseVersion(pluginInfo.getVersion());
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getOrganization() {
		return organization;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
	}

	@Override
	public void start() throws PluginException {
		if (state == STARTED) {
			return;
		}
		if (state == UNINSTALLED) {
			throw new PluginException("Cannot start an uninstalled plugin");
		}
		List<Bundle> startedBundles = new ArrayList<Bundle>();
		for (Bundle bundle : getBundles()) {
			if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				if (bundle.getState() != Bundle.ACTIVE) {
					try {
						bundle.start();
						startedBundles.add(bundle);
					} catch (BundleException e) {
						// clean up by stopping bundles already started
						for (Bundle startedBundle : startedBundles) {
							try {
								startedBundle.stop();
							} catch (BundleException ex) {
								logger.warn("Error unistalling bundle", ex);
							}
						}
						throw new PluginException(String.format("Error starting bundle %1$s",
								bundle.getSymbolicName()), e);
					}
				}
			}
		}
	}

	@Override
	public void stop() throws PluginException {
		if (state == STARTED) {
			List<Plugin> installedPlugins = pluginManager.getInstalledPlugins();
			for (Bundle bundle : getBundles()) {
				// check if bundle is used by other plugins
				boolean bundleUsed = false;
				for (Plugin installedPlugin : installedPlugins) {
					if (!installedPlugin.equals(this) && installedPlugin.getState() == STARTED) {
						if (installedPlugin.getBundles().contains(bundle)) {
							bundleUsed = true;
							break;
						}
					}
				}
				if (!bundleUsed) {
					try {
						logger.info("Stopping bundle " + bundle.getSymbolicName());
						bundle.stop();
					} catch (BundleException e) {
						logger.warn(
								String.format("Error stopping bundle %1$s for plugin %2$s",
										bundle.getSymbolicName(), getName()), e);
					}
				}
			}
			state = STOPPED;
		}
	}

	@Override
	public void uninstall() throws PluginException {
		if (state != UNINSTALLED) {
			pluginManager.uninstallPlugin(this);
			state = UNINSTALLED;
		}
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public Set<Bundle> getBundles() {
		return bundles;
	}

}
