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

import java.io.File;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * A plugin adds functionality to the application by providing implementations of application
 * services.
 *
 * @author David Withers
 */
public interface Plugin {

	public static enum State {
		UNINSTALLED, INSTALLED, STARTED, STOPPED
	}

	public String getId();

	public String getName();

	public String getDescription();

	public String getOrganization();

	public Version getVersion();

	/**
	 * Returns the state of the plugin.
	 *
	 * @return the state of the plugin
	 */
	public State getState();

	/**
	 * Starts the plugin and sets the state to STARTED.
	 * <p>
	 * If the plugin state is STARTED this method will have no effect.
	 * <p>
	 * All plugin bundles are not currently started will be started.
	 *
	 * @throws PluginException
	 *             if the plugin state is UNINSTALLED or any of the plugin bundles cannot be started
	 */
	public void start() throws PluginException;

	/**
	 * Stops the plugin and sets the state to STOPPED.
	 * <p>
	 * If the plugin state is not STARTED this method will have no effect.
	 * <p>
	 * All plugin bundles not used elsewhere will be stopped.
	 *
	 * @throws PluginException
	 *             if any of the plugin bundles cannot be stopped
	 */
	public void stop() throws PluginException;

	public void uninstall() throws PluginException;

	public File getFile();

	public Set<Bundle> getBundles();

}
