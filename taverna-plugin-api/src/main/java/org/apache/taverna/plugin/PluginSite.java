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

/**
 * A plugin site specifies the location of a site that contains plugins.
 * <p>
 * There are two types of plugin site:
 * <dl>
 * <dt>SYSTEM</dt>
 * <dd>plugin sites specified by the application profile</dd>
 * <dt>USER</dt>
 * <dd>plugin sites that can be added and removed by the user</dd>
 * </dl>
 *
 * @author David Withers
 */
public interface PluginSite {

	public static enum PluginSiteType {
		SYSTEM, USER
	};

	/**
	 * Returns the name of the plugin site.
	 *
	 * @return the name of the plugin site
	 */
	public String getName();

	/**
	 * Returns the URL of the plugin site.
	 *
	 * @return the URL of the plugin site
	 */
	public String getUrl();

	/**
	 * Returns the type of the plugin site.
	 * <p>
	 * The type is either {@code SYSTEM} or {@code USER}
	 *
	 * @return the type of the plugin site
	 */
	public PluginSiteType getType();

}
