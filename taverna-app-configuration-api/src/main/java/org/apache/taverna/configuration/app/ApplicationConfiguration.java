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
package org.apache.taverna.configuration.app;

import java.io.File;

import org.apache.taverna.profile.xml.jaxb.ApplicationProfile;

/**
 * Represent the application config as it has been specified in
 * {@value #PROPERTIES}. This configuration specifies the application's name
 * and title, etc.
 * <p>
 * An application would typically provide the {@value #PROPERTIES} file on the classpath under
 * a <code>conf</code> directory, or in a <code>conf</code> directory in the
 * application's distribution directory.
 *
 * @author Stian Soiland-Reyes
 * @author David Withers
 */
public interface ApplicationConfiguration {

	public static final String CONF_DIR = "conf/";
	public static final String PLUGINS_DIR = "plugins";

	public String getName();

	public String getTitle();

	public File getStartupDir();

	public File getApplicationHomeDir();

	public File getUserPluginDir();

	public File getSystemPluginDir();

	public File getLogFile();

	public File getLogDir();

	public ApplicationProfile getApplicationProfile();

}