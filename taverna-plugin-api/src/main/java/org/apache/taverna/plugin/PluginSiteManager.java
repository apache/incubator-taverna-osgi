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

import java.net.URL;
import java.util.List;

import org.apache.taverna.plugin.xml.jaxb.PluginVersions;

/**
 * Manages plugin sites.
 *
 * @author David Withers
 */
public interface PluginSiteManager {

	/**
	 * Returns all the managed plugin sites.
	 * <p>
	 * If there are no plugin sites an empty list is returned.
	 *
	 * @return all the managed plugin sites
	 * @throws PluginException
	 */
	public List<PluginSite> getPluginSites();

	/**
	 * Contacts the plugin site at the specified URL and return a new plugin site.
	 *
	 * @param pluginSiteURL the plugin site URL
	 * @throws PluginException if there is a problem contacting the plugin site
	 */
	public PluginSite createPluginSite(URL pluginSiteURL) throws PluginException;

	/**
	 * Adds a plugin site.
	 * <p>
	 * If the plugin site already exists this method does nothing.
	 *
	 * @param pluginSite the plugin site to add
	 * @throws PluginException
	 */
	public void addPluginSite(PluginSite pluginSite) throws PluginException;

	/**
	 * Removes a plugin site.
	 * <p>
	 * If the plugin site does not exist this method does nothing.
	 *
	 * @param pluginSite the plugin site to remove
	 * @throws PluginException
	 */
	public void removePluginSite(PluginSite pluginSite) throws PluginException;

	/**
	 * Returns all the plugins available at the specified plugin site.
	 * <p>
	 * If no plugins are available an empty list is returned.
	 *
	 * @param pluginSite
	 *            the plugin site to contact
	 * @return all the plugins available at the specified plugin site
	 * @throws PluginException
	 *             if there is a plroblem contacting the plugin site
	 */
	public List<PluginVersions> getPlugins(PluginSite pluginSite) throws PluginException;

}
