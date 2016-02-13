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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URL;

import org.apache.taverna.configuration.app.ApplicationConfiguration;
import org.apache.taverna.download.DownloadException;
import org.apache.taverna.download.DownloadManager;
import org.apache.taverna.plugin.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 *
 * @author David Withers
 */
@Ignore
public class PluginSiteManagerImplTest {

	private PluginSiteManagerImpl pluginSiteManager;
	private ApplicationConfiguration applicationConfiguration;
	private DownloadManager downloadManager;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		pluginSiteManager = new PluginSiteManagerImpl();
		applicationConfiguration = mock(ApplicationConfiguration.class);
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#PluginSiteManagerImpl()}.
	 * @throws Exception 
	 */
	@Test
	public void testPluginSiteManagerImpl() throws Exception {
		new PluginSiteManagerImpl();
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#getPluginSites()}.
	 */
	@Test
	public void testGetPluginSites() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#createPluginSite(java.net.URL)}.
	 * @throws DownloadException
	 */
	@Test
	public void testCreatePluginSite() throws Exception {
		downloadManager = mock(DownloadManager.class);
		doNothing().when(downloadManager).download(URI.create("file:///"), null, "");

		pluginSiteManager.setDownloadManager(downloadManager);

		pluginSiteManager.createPluginSite(new URL("file:///"));

	}

	@Test(expected=PluginException.class)
	public void testCreatePluginSiteDownloadException() throws Exception {
		downloadManager = mock(DownloadManager.class);
		doThrow(DownloadException.class).when(downloadManager).download(URI.create("file:///"), null, "");

		pluginSiteManager.setDownloadManager(downloadManager);

		pluginSiteManager.createPluginSite(new URL("file:///"));
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#addPluginSite(org.apache.taverna.plugin.PluginSite)}.
	 */
	@Test
	public void testAddPluginSite() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#removePluginSite(org.apache.taverna.plugin.PluginSite)}.
	 */
	@Test
	public void testRemovePluginSite() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#getPlugins(org.apache.taverna.plugin.PluginSite)}.
	 */
	@Test
	public void testGetPlugins() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#setApplicationConfiguration(org.apache.taverna.configuration.app.ApplicationConfiguration)}.
	 */
	@Test
	public void testSetApplicationConfiguration() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.apache.taverna.plugin.impl.PluginSiteManagerImpl#setDownloadManager(org.apache.taverna.download.DownloadManager)}.
	 */
	@Test
	public void testSetDownloadManager() {
		fail("Not yet implemented");
	}

}
