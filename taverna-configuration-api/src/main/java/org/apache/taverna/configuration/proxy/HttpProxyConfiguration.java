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
package org.apache.taverna.configuration.proxy;

import org.apache.taverna.configuration.Configurable;

/**
 * The HttpProxyConfiguration handles the configuration of HTTP
 * proxy when Taverna is launched.
 *
 * @author David Withers
 */
public interface HttpProxyConfiguration extends Configurable {

	/**
	 * The acceptable values for which proxy values to use
	 */
	public static String USE_SYSTEM_PROPERTIES_OPTION = "useSystemProperties";
	public static String USE_NO_PROXY_OPTION = "useNoProxy";
	public static String USE_SPECIFIED_VALUES_OPTION = "useSpecifiedValues";

	/**
	 * The key within the Properties where the value will indicate which set of
	 * proxy values to use
	 */
	public static String PROXY_USE_OPTION = "proxyUseOption";

	/**
	 * The keys within the Properties for the ad hoc Taverna proxy settings
	 */
	public static String TAVERNA_PROXY_HOST = "tavernaProxyHost";
	public static String TAVERNA_PROXY_PORT = "tavernaProxyPort";
	public static String TAVERNA_PROXY_USER = "tavernaProxyUser";
	public static String TAVERNA_PROXY_PASSWORD = "tavernaProxyPassword";
	public static String TAVERNA_NON_PROXY_HOSTS = "tavernaNonProxyHosts";

	/**
	 * The keys within the Properties for the System proxy settings
	 */
	public static String SYSTEM_PROXY_HOST = "systemProxyHost";
	public static String SYSTEM_PROXY_PORT = "systemProxyPort";
	public static String SYSTEM_PROXY_USER = "systemProxyUser";
	public static String SYSTEM_PROXY_PASSWORD = "systemProxyPassword";
	public static String SYSTEM_NON_PROXY_HOSTS = "systemNonProxyHosts";

	/**
	 * The keys within the System Properties that are used for specifying HTTP
	 * proxy information
	 */
	public static String PROXY_HOST = "http.proxyHost";
	public static String PROXY_PORT = "http.proxyPort";
	public static String PROXY_USER = "http.proxyUser";
	public static String PROXY_PASSWORD = "http.proxyPassword";
	public static String NON_PROXY_HOSTS = "http.nonProxyHosts";

	/**
	 * Change the System Proxy settings according to the property values.
	 */
	public void changeProxySettings();

}