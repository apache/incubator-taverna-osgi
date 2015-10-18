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
/**
 *
 */
package org.apache.taverna.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.taverna.configuration.AbstractConfigurable;
import org.apache.taverna.configuration.ConfigurationManager;

public class DummyConfigurable extends AbstractConfigurable {

	public DummyConfigurable(ConfigurationManager configurationManager) {
		super(configurationManager);
	}

	Map<String,String> defaults = null;

	public String getCategory() {
		return "test";
	}

	public Map<String, String> getDefaultPropertyMap() {
		if (defaults==null) {
			defaults = new HashMap<String, String>();
			defaults.put("name","john");
			defaults.put("colour","blue");
		}
		return defaults;
	}

	public String getUUID() {
		return "cheese";
	}

	public String getDisplayName() {
		return "dummyName";
	}

	public String getFilePrefix() {
		return "dummyPrefix";
	}

}
