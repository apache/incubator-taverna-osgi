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

import org.apache.taverna.plugin.PluginSite;

/**
 * PluginSite implementation.
 *
 * @author David Withers
 */
public class PluginSiteImpl implements PluginSite {

	private String name, url;

	private PluginSiteType type;

	public PluginSiteImpl() {
	}

	public PluginSiteImpl(String name, String url) {
		this(name, url, PluginSiteType.USER);
	}

	public PluginSiteImpl(String name, String url, PluginSiteType type) {
		this.name = name;
		this.url = url;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public PluginSiteType getType() {
		return type;
	}

	public void setType(PluginSiteType type) {
		this.type = type;
	}

}
