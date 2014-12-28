/*******************************************************************************
 * Copyright (C) 2013 The University of Manchester
 *
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
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
