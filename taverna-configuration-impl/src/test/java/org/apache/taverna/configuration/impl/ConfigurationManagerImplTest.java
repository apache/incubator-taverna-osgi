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
package org.apache.taverna.configuration.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.apache.taverna.configuration.AbstractConfigurable;
import org.apache.taverna.configuration.Configurable;
import org.apache.taverna.configuration.DummyConfigurable;
import org.apache.taverna.configuration.app.ApplicationConfiguration;
import org.apache.taverna.profile.xml.jaxb.ApplicationProfile;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationManagerImplTest {

	private File configFile;

	private ConfigurationManagerImpl manager;

	private DummyConfigurable dummyConfigurable;

	@Before
	public void setup() throws Exception {
		dummyConfigurable = new DummyConfigurable(manager);
		File f = new File(System.getProperty("java.io.tmpdir"));
		File configTestsDir = new File(f, "configTests");
		if (!configTestsDir.exists())
			configTestsDir.mkdir();
		final File d = new File(configTestsDir, UUID.randomUUID().toString());
		d.mkdir();
		manager = new ConfigurationManagerImpl(new ApplicationConfiguration() {
			public Path getApplicationHomeDir() {
				return d.toPath();
			}

			public String getName() {
				return null;
			}

			public String getTitle() {
				return null;
			}

			public Path getStartupDir() {
				return null;
			}

			public Path getUserPluginDir() {
				return null;
			}

			public Path getSystemPluginDir() {
				return null;
			}

			public Path getLogFile() {
				return null;
			}

			public Path getLogDir() {
				return null;
			}

			public Properties getProperties() {
				return null;
			}

			@Override
			public ApplicationProfile getApplicationProfile() {
				// TODO Auto-generated method stub
				return null;
			}
		});
		configFile = new File(d, "conf/"+manager.generateFilename(dummyConfigurable));
		dummyConfigurable.restoreDefaults();
	}

	@Test
	public void testStore() throws Exception {
		Configurable conf = dummyConfigurable;
		manager.store(conf);
		assertTrue(configFile.exists());
	}

	@Test
	public void testDefaultValues() throws Exception {
		Configurable conf = dummyConfigurable;
		assertEquals("name should equal john", "john", conf.getProperty("name"));
		manager.store(conf);
		Properties props = new Properties();
		props.load(new FileInputStream(configFile));
		assertFalse("stored properties should not contain the default value",
				props.containsKey("name"));
		manager.populate(conf);
		assertEquals("default property name should still exist after re-populating", "john",
				conf.getProperty("name"));
	}

	@Test
	public void testRemoveNotDefaultValue() throws Exception {
		Configurable conf = dummyConfigurable;
		conf.setProperty("hhh", "iii");
		manager.store(conf);
		Properties props = new Properties();
		props.load(new FileInputStream(configFile));
		assertEquals("The stored file should contain the new entry", "iii", props.get("hhh"));
		conf.deleteProperty("hhh");
		manager.store(conf);
		manager.populate(conf);
		assertNull("The removed value should no longer exist", conf.getProperty("hhh"));
		props.clear();
		props.load(new FileInputStream(configFile));
		assertNull("The stored file should no longer contain the deleted entry", props.get("hhh"));
	}

	@Test
	public void testNewValues() throws Exception {
		Configurable conf = dummyConfigurable;
		conf.setProperty("country", "france");
		assertEquals("country should equal france", "france", conf.getProperty("country"));
		manager.store(conf);
		Properties props = new Properties();
		props.load(new FileInputStream(configFile));
		assertTrue("stored properties should contain the default value",
				props.containsKey("country"));
		assertEquals("stored property country should equal france", "france",
				props.getProperty("country"));
		manager.populate(conf);
		assertEquals("default property name should still exist after re-populating", "france",
				conf.getProperty("country"));
	}

	@Test
	public void testDeleteDefaultProperty() throws Exception {
		AbstractConfigurable conf = dummyConfigurable;
		assertEquals("name should equal john", "john", conf.getProperty("name"));
		conf.deleteProperty("name");
		manager.store(conf);
		manager.populate(conf);
		assertNull("value for name should be null", conf.getProperty("name"));

		Properties props = new Properties();
		props.load(new FileInputStream(configFile));
		assertTrue("Key name should be in stored props because its a deleted default value",
				props.containsKey("name"));
		assertEquals("name should have the special value to indicate its been deleted",
				AbstractConfigurable.DELETED_VALUE_CODE, props.getProperty("name"));
	}

	@Test
	public void testFilename() {
		assertTrue(configFile.getAbsolutePath().endsWith("dummyPrefix-cheese.config"));
	}
}
