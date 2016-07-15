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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

/**
 * A utility abstract class that simplifies implementing a Configurable.
 * <br>
 * <p>A concrete version of this class needs to define the name,category,
 * UUID string and the set of default values.</p>
 *
 * @author Stuart Owen
 *
 */
public abstract class AbstractConfigurable implements Configurable {

	private Map<String,String> propertyMap = new HashMap<String, String>();

	private static Logger logger = Logger.getLogger(AbstractConfigurable.class);

	public static final String DELETED_VALUE_CODE = "~~DELETED~~";

	private ConfigurationManager configurationManager;

	public Set<String> getKeys() {
		return getInternalPropertyMap().keySet();
	}

	/**
	 * Constructs the AbstractConfigurable by either reading from a previously stored set of properties,
	 * or by using the default values which results in them being stored for subsequent usage.
	 */
	public AbstractConfigurable(ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
		try {
			configurationManager.populate(this);
		} catch (Exception e) {
			logger.error("There was an error reading the properties for the Configurable:"+getFilePrefix(),e);
		}
	}

	public synchronized String getProperty(String key) {
		String val = getInternalPropertyMap().get(key);
		if (val==null) val=getDefaultProperty(key);
		if (DELETED_VALUE_CODE.equals(val)) val=null;
		return val;
	}

	public String getDefaultProperty(String key) {
		return getDefaultPropertyMap().get(key);
	}

	protected void store() {
		try {
			configurationManager.store(this);
		} catch (Exception e) {
			logger.error("There was an error storing the new configuration for: "+this.getFilePrefix(),e);
		}
	}

	public void clear() {
		getInternalPropertyMap().clear();
	}

	public synchronized void setProperty(String key, String value) {
		Object oldValue = getInternalPropertyMap().get(key);
		if (value==null) {
			deleteProperty(key);
		}
		else {
			getInternalPropertyMap().put(key,value);
		}
		if (value==null || !value.equals(oldValue)) {
			store();
		}
	}

	/**
	 * Provides access to the internal map.
	 * <br>
	 * Note that this map will contain entries for deleted values that also have corresponding default values.
	 * For this reason using this map directly is discouraged, and  #getProperty(String)} should be used instead.
	 * @return
	 */
	public Map<String, String> getInternalPropertyMap() {
		return propertyMap;
	}


	public void restoreDefaults() {
		propertyMap.clear();
		propertyMap.putAll(getDefaultPropertyMap());
		store();
	}

	public void deleteProperty(String key) {
		if (getDefaultPropertyMap().containsKey(key)) {
			propertyMap.put(key, DELETED_VALUE_CODE);
		}
		else {
			propertyMap.remove(key);
		}
	}

	/**
	 * Returns an unmodifiable List<String> for the given key. Internally the value is stored as a single String, but converted to a list when calling this method.
	 * <br>
	 * The list is unmodifiable to prevent the mistake of trying <pre>getPropertyStringList(..).add("new element");</pre> which will not affect the stored
	 * list. For the property to be updated this{@link #setPropertyStringList(String, List)} must be used.
	 */
	public List<String> getPropertyStringList(String key) {
		String value = getProperty(key);
		if (value!=null) {
			return Collections.unmodifiableList(fromListText(value));
		}
		else {
			return null;
		}
	}

	private List<String> fromListText(String property) {
		List<String> result = new ArrayList<String>();
		if (property.length()>0) { //an empty string as assumed to be an empty list, rather than a list with 1 empty string in it!
			StringReader reader = new StringReader(property);
			try (CSVParser csvReader = new CSVParser(reader,CSVFormat.DEFAULT)){
				
				for (CSVRecord v : csvReader.getRecords()) {
					Iterator<String> itr = v.iterator();
					while(itr.hasNext())result.add(itr.next());
					
				}
			} catch (IOException e) {
				logger.error("Exception occurred parsing CSV properties:"+property,e);
			}
			
		}
		return result;
	}

	/**
	 * Set a value that is known to be a list. The value can be retrieved using this{@link #getPropertyStringList(String)}
	 * <br>
	 * Within the file, the value is stored as a single Comma Separated Value
	 */
	public void setPropertyStringList(String key, List<String> value) {
		setProperty(key, toListText(value));
	}

	private String toListText(List<String> values) {
		StringWriter writer = new StringWriter();
		try(CSVPrinter csvWriter = new CSVPrinter(writer,CSVFormat.DEFAULT)) {
			csvWriter.printRecord(values);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.getBuffer().toString().trim();
	}

}
