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
package net.sf.taverna.t2.maven.plugins;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;

/**
 * Abstract Mojo for using the wagon.
 *
 * @author David Withers
 */
public abstract class AbstractWagonMojo extends AbstractMojo {

	@Component
	protected WagonManager wagonManager;

	/**
	 * Disconnect the wagon.
	 *
	 * @param wagon
	 *            the wagon to disconnect
	 * @param debug
	 */
	protected void disconnectWagon(Wagon wagon, Debug debug) {
		if (getLog().isDebugEnabled()) {
			wagon.removeTransferListener(debug);
			wagon.removeSessionListener(debug);
		}
		try {
			wagon.disconnect();
		} catch (ConnectionException e) {
			getLog().error("Error disconnecting wagon - ignored", e);
		}
	}


}
