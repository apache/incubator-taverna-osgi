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
package org.apache.taverna.configuration;

/**
 * Handles the configuration for a {@link Configurable} object
 *
 * @author David Withers
 */
public interface ConfigurationManager {

	/**
	 * Write out the properties configuration to disk based on the UUID of the
	 * {@link Configurable}
	 * <br>
	 * Default values are not stored within the file, but only those that have been changed or deleted.
	 *
	 * @param configurable
	 * @throws Exception
	 */
	public void store(Configurable configurable) throws Exception;

	/**
	 * Loads the configuration details from disk or from memory and populates the provided Configurable
	 *
	 * @param configurable
	 * @return
	 * @throws Exception
	 *             if there are no configuration details available
	 */
	public void populate(Configurable configurable) throws Exception;

}