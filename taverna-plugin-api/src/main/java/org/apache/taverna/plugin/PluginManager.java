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
package org.apache.taverna.plugin;

import java.nio.file.Path;
import java.util.List;

import org.apache.taverna.plugin.xml.jaxb.PluginVersions;

/**
 * Manages installing plugins and checking for plugin updates.
 *
 * @author David Withers
 */
public interface PluginManager {

	public static final String EVENT_TOPIC_ROOT = "org/apache/taverna/plugin/PluginManager/";
	public static final String PLUGIN_INSTALLED = EVENT_TOPIC_ROOT + "PLUGIN_INSTALLED";
	public static final String PLUGIN_UNINSTALLED = EVENT_TOPIC_ROOT + "PLUGIN_UNINSTALLED";
	public static final String UPDATES_AVAILABLE = EVENT_TOPIC_ROOT + "UPDATES_AVAILABLE";

	/**
	 * Loads plugins from the system and user plugin directories.
	 * <p>
	 * If the plugins are not already installed they will be installed and started.
	 *
	 * @throws PluginException
	 */
	public void loadPlugins() throws PluginException;

	/**
	 * Check if there are new versions of installed plugins available.
	 * <p>
	 * If updates are available and event with topic {@link UPDATES_AVAILABLE} will be posted.
	 *
	 * @throws PluginException
	 */
	public void checkForUpdates() throws PluginException;

	/**
	 * Returns updated versions of installed plugins.
	 * <p>
	 * Only plugins that the user has permission to update are returned.
	 *
	 * @return
	 */
	public List<PluginVersions> getPluginUpdates() throws PluginException;

	/**
	 * Returns new plugins available from all plugin sites.
	 *
	 * @return new plugins available from all plugin sites.
	 * @throws PluginException
	 */
	public List<PluginVersions> getAvailablePlugins() throws PluginException;

	/**
	 * Returns all the installed plugins.
	 *
	 * @return
	 * @throws PluginException
	 */
	public List<Plugin> getInstalledPlugins() throws PluginException;

	/**
	 * Installs a plugin from a plugin file.
	 *
	 * @param pluginFile
	 *            the file to install the plugin from
	 * @return the installed plugin
	 * @throws PluginException
	 */
	public Plugin installPlugin(Path pluginFile) throws PluginException;

	/**
	 * Installs a plugin from an update site.
	 *
	 * @param pluginSiteURL
	 * @param pluginFile
	 * @return
	 * @throws PluginException
	 */
	public Plugin installPlugin(String pluginSiteURL, String pluginFile) throws PluginException;

	public Plugin updatePlugin(PluginVersions pluginVersions) throws PluginException;

}
