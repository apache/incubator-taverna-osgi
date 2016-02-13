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

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;
import org.apache.taverna.plugin.Plugin;
import org.apache.taverna.plugin.PluginException;

/**
 * Watches a plugin directory and adds or removes plugins when plugin files are added or removed
 * from the directory.
 * 
 * @author David Withers
 */
public class PluginDirectoryWatcher extends FileAlterationListenerAdaptor {

	private static final Logger logger = Logger.getLogger(PluginDirectoryWatcher.class);

	private final PluginManagerImpl pluginManager;
	private final File directory;

	private FileAlterationMonitor monitor;

	public PluginDirectoryWatcher(PluginManagerImpl pluginManager, File directory) {
		this.pluginManager = pluginManager;
		this.directory = directory;
		FileAlterationObserver observer = new FileAlterationObserver(directory);
		observer.addListener(this);
		monitor = new FileAlterationMonitor();
		monitor.addObserver(observer);
	}

	/**
	 * Starts watching the plugin directory.
	 * 
	 * @throws PluginException
	 */
	public void start() throws PluginException {
		try {
			monitor.start();
		} catch (Exception e) {
			throw new PluginException(String.format("Error starting watch on %1$s.",
					directory.getAbsolutePath()), e);
		}
	}

	/**
	 * Stops watching the plugin directory.
	 * 
	 * @throws PluginException
	 */
	public void stop() throws PluginException {
		try {
			monitor.stop();
		} catch (Exception e) {
			throw new PluginException(String.format("Error stopping watch on %1$s.",
					directory.getAbsolutePath()), e);
		}
	}

	@Override
	public void onFileCreate(File file) {
		try {
			Plugin plugin = pluginManager.installPlugin(file.toPath());
			plugin.start();
		} catch (PluginException e) {
			logger.warn("Error loading plugin file " + file, e);
		}
	}

	@Override
	public void onFileChange(File file) {
		onFileDelete(file);
		onFileCreate(file);
	}

	@Override
	public void onFileDelete(File file) {
		pluginManager.uninstallPlugin(file);
	}

}
