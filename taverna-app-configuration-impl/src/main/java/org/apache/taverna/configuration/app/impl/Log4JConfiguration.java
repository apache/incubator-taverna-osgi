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
package org.apache.taverna.configuration.app.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.taverna.configuration.app.ApplicationConfiguration;

public class Log4JConfiguration {
	public final static String LOG4J_PROPERTIES = "log4j.properties";

	private static boolean log4jConfigured = false;

	private ApplicationConfiguration applicationConfiguration;

	private Properties properties ;

	public Log4JConfiguration() {
//		prepareLog4J();
	}

	public void prepareLog4J() {
		if (!log4jConfigured) {
			Properties log4jProperties = getLogProperties();
			if (log4jProperties != null && ! log4jProperties.isEmpty()) {
				LogManager.resetConfiguration();
				PropertyConfigurator.configure(log4jProperties);
			}

			String logFilePath = applicationConfiguration.getLogFile().getAbsolutePath();
			PatternLayout layout = new PatternLayout("%-5p %d{ISO8601} (%c:%L) - %m%n");

			// Add file appender
			RollingFileAppender appender;
			try {
				appender = new RollingFileAppender(layout, logFilePath);
				appender.setMaxFileSize("1MB");
				appender.setEncoding("UTF-8");
				appender.setMaxBackupIndex(4);
				// Let root logger decide level
				appender.setThreshold(Level.ALL);
				LogManager.getRootLogger().addAppender(appender);
			} catch (IOException e) {
				System.err.println("Could not log to " + logFilePath);
			}

			log4jConfigured = true;
		}
	}

	/**
	 * Initialises and provides access to the list of Properties.
	 * @return
	 */
	public Properties getLogProperties() {
		if (properties == null) {
			InputStream is = getLogPropertiesInputStream();
			if (is != null) {
				try {
					properties = new Properties();
					properties.load(is);
//					properties.putAll(System.getProperties());
				}  catch (IOException e) {
					errorLog("An error occurred trying to load the " + LOG4J_PROPERTIES + " file",e);
				}
			}
		}
		return properties;
	}

	/**
	 * Return an input stream to the configuration file, or null if it can't be found
	 * @return
	 */
	private InputStream getLogPropertiesInputStream() {
		InputStream result = null;
		File propertiesFile = getLogPropertiesFile();
		if (propertiesFile!=null) {
			try {
				result=new FileInputStream(propertiesFile);
			} catch (FileNotFoundException e) {
				errorLog("Unable to find "+LOG4J_PROPERTIES,e);
			}
		}
		else {
			errorLog("Unable to determine file for "+LOG4J_PROPERTIES,null);
		}
		return result;
	}

	/**
	 * Returns a File object to the configuration file or null if it cannot be found.
	 *
	 * @return
	 */
	private File getLogPropertiesFile() {
		File home = applicationConfiguration.getApplicationHomeDir();
		File startup = applicationConfiguration.getStartupDir();
		File result=null;
		if (home!=null) {
			File file = new File(new File(home, ApplicationConfiguration.CONF_DIR), LOG4J_PROPERTIES);
			if (file.exists()) {
				result=file;
			}
		}
		if (result==null && startup!=null) {
			File file = new File(new File(startup, ApplicationConfiguration.CONF_DIR), LOG4J_PROPERTIES);
			if (file.exists()) {
				result=file;
			}
		}
		return result;
	}

	private void errorLog(String message, Throwable exception) {
		System.out.println(message);
		if (exception!=null) {
			exception.printStackTrace();
		}

	}

	/**
	 * Sets the applicationConfiguration.
	 *
	 * @param applicationConfiguration the new value of applicationConfiguration
	 */
	public void setApplicationConfiguration(ApplicationConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}

}
