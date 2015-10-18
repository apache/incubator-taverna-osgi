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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import org.apache.taverna.configuration.Configurable;
import org.apache.taverna.configuration.ConfigurationUIFactory;

public class DummyUIFactory2 implements ConfigurationUIFactory {

	public boolean canHandle(String uuid) {
		return getConfigurable().getUUID().equals(uuid);
	}

	public Configurable getConfigurable() {
		return new DummyConfigurable2();
	}

	public JPanel getConfigurationPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	static class DummyConfigurable2 implements Configurable {

		public void deleteProperty(String key) {
			// TODO Auto-generated method stub

		}

		public String getCategory() {
			// TODO Auto-generated method stub
			return null;
		}

		public Map<String, String> getDefaultPropertyMap() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getProperty(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		public Map<String, String> getInternalPropertyMap() {
			// TODO Auto-generated method stub
			return null;
		}

		public List<String> getPropertyStringList(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getUUID() {
			return "456";
		}

		public void restoreDefaults() {
			// TODO Auto-generated method stub

		}

		public void setProperty(String key, String value) {
			// TODO Auto-generated method stub

		}

		public void setPropertyStringList(String key, List<String> value) {
			// TODO Auto-generated method stub

		}

		public String getDefaultProperty(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		public void clear() {
			// TODO Auto-generated method stub

		}

		public Set<String> getKeys() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getDisplayName() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getFilePrefix() {
			// TODO Auto-generated method stub
			return null;
		}



	}

}
