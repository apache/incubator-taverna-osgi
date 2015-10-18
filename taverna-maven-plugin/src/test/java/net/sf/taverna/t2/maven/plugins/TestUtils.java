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

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Test;

public class TestUtils {

	@Test
	public void getJavaPackages() throws Exception {
		Log log = new SilentLog();
		Set<String> packages = Utils.getJavaPackages(log);
		assertTrue(packages.contains("java.util"));
		assertTrue(packages.contains("org.xml.sax.helpers"));
		for (String pkg: packages) { 
			assertFalse(pkg.isEmpty());
			assertFalse(pkg.contains(" "));
			assertFalse(pkg.contains("#"));
			assertTrue(pkg.contains("."));
		}
	}
	
}
