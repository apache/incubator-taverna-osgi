/*******************************************************************************
 * Copyright (C) 2011 The University of Manchester
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
package uk.org.taverna.osgi.services.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A BundleActivator class that allows XML Transformers to register as an OSGi service.
 *
 * @author David Withers
 */
public class XMLTransformerActivator implements BundleActivator, ServiceFactory {

	private static final Logger logger = Logger.getLogger(XMLTransformerActivator.class.getName());

	private BundleContext context;

	private static final String FACTORY_INTERFACE = "javax.xml.transform.TransformerFactory";

	private static final String SERVICES_DIRECTORY = "/META-INF/services/";

	private static final String FACTORY_IMPLEMENTATIONS_FILE = SERVICES_DIRECTORY
			+ FACTORY_INTERFACE;

	private static final String FACTORY_DESCRIPTION = "An XML Transformer";

	private static final String FACTORY_NAMEKEY = "transformer.factoryname";

	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		ServiceReference serviceReference = registration.getReference();
		String transformerFactoryClassName = (String) serviceReference.getProperty(FACTORY_NAMEKEY);
		try {
			return createFactory(transformerFactoryClassName);
		} catch (TransformerFactoryConfigurationError fce) {
			logger.log(Level.WARNING, "Error while creating TransformerFactory", fce);
			return null;
		}
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		Bundle bundle = context.getBundle();
		try {
			registerFactories(getFactoryClassNames(bundle.getResource(FACTORY_IMPLEMENTATIONS_FILE)));
		} catch (IOException ioe) {
			throw new TransformerFactoryConfigurationError(ioe);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	private List<String> getFactoryClassNames(URL implementationsURL) throws IOException {
		List<String> classNames = new ArrayList<String>();
		if (implementationsURL != null) {
			InputStream is = implementationsURL.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) {
					continue; // blank line
				}
				int commentPosition = line.indexOf("#");
				if (commentPosition == 0) { // comment line
					continue;
				} else if (commentPosition < 0) { // no comment
					classNames.add(line);
				} else {
					classNames.add(line.substring(0, commentPosition).trim());
				}
			}
		}
		return classNames;
	}

	private void registerFactories(List<String> factoryClassNames)
			throws TransformerFactoryConfigurationError {
		for (int index = 0; factoryClassNames.size() > index; index++) {
			String factoryClassName = factoryClassNames.get(index);
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put(Constants.SERVICE_DESCRIPTION, FACTORY_DESCRIPTION);
			properties.put(Constants.SERVICE_PID, FACTORY_INTERFACE + "."
					+ context.getBundle().getBundleId() + "." + index);
			properties.put(FACTORY_NAMEKEY, factoryClassName);
			context.registerService(FACTORY_INTERFACE, this, properties);
		}
	}

	private Object createFactory(String factoryClassName)
			throws TransformerFactoryConfigurationError {
		try {
			return Class.forName(factoryClassName).newInstance();
		} catch (InstantiationException e) {
			throw new TransformerFactoryConfigurationError(e);
		} catch (IllegalAccessException e) {
			throw new TransformerFactoryConfigurationError(e);
		} catch (ClassNotFoundException e) {
			throw new TransformerFactoryConfigurationError(e);
		}
	}

}
