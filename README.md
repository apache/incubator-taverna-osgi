<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Apache Taverna OSGi plugin system

OSGi-based plugin system, including online updates. Written for
[Apache Taverna](http://taverna.incubator.apache.org/) (incubating),
probably usable for any OSGi-based command line/desktop product.


## License

* (c) 2007-2014 University of Manchester
* (c) 2014-2016 Apache Software Foundation

This product includes software developed at The [Apache Software
Foundation](http://www.apache.org/).

Licensed under the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), see the file
[LICENSE](LICENSE) for details.

The file [NOTICE](NOTICE) contain any additional attributions and
details about embedded third-party libraries and source code.


# Contribute

Please subscribe to and contact the
[dev@taverna](http://taverna.incubator.apache.org/community/lists#dev mailing list)
for any questions, suggestions and discussions about
Apache Taverna.

Bugs and feature plannings are tracked in the Jira
[Issue tracker](https://issues.apache.org/jira/browse/TAVERNA/component/12326811)
under the `TAVERNA` component _Taverna Common Activities_. Feel free
to add an issue!

To suggest changes to this source code, feel free to raise a
[GitHub pull request](https://github.com/apache/incubator-taverna-osgi/pulls).
Any contributions received are assumed to be covered by the [Apache License
2.0](https://www.apache.org/licenses/LICENSE-2.0). We might ask you
to sign a [Contributor License Agreement](https://www.apache.org/licenses/#clas)
before accepting a larger contribution.


## Disclaimer

Apache Taverna is an effort undergoing incubation at the
[Apache Software Foundation (ASF)](http://www.apache.org/),
sponsored by the [Apache Incubator PMC](http://incubator.apache.org/).

[Incubation](http://incubator.apache.org/incubation/Process_Description.html)
is required of all newly accepted projects until a further review
indicates that the infrastructure, communications, and decision making process
have stabilized in a manner consistent with other successful ASF projects.

While incubation status is not necessarily a reflection of the completeness
or stability of the code, it does indicate that the project has yet to be
fully endorsed by the ASF.



## Prerequisites

* Java 1.8 or newer (tested with OpenJDK 1.8)
* [Apache Maven](https://maven.apache.org/download.html) 3.2.5 or newer (older
  versions probably also work)


# Building

To build, use

    mvn clean install

This will build each module and run their tests.


## Building on Windows

If you are building on Windows, ensure you unpack this source code
to a folder with a [short path name](http://stackoverflow.com/questions/1880321/why-does-the-260-character-path-length-limit-exist-in-windows) 
lenght, e.g. `C:\src` - as 
Windows has a [limitation on the total path length](https://msdn.microsoft.com/en-us/library/aa365247%28VS.85%29.aspx#maxpath) 
which might otherwise
prevent this code from building successfully.


## Skipping tests

To skip the tests (these can be timeconsuming), use:

    mvn clean install -DskipTests


If you are modifying this source code independent of the
Apache Taverna project, you may not want to run the
[Rat Maven plugin](https://creadur.apache.org/rat/apache-rat-plugin/)
that enforces Apache headers in every source file - to disable it, try:

    mvn clean install -Drat.skip=true

# Modules

The Taverna OSGi modules are split into `-api` and `-impl`. `-api` contain
Java interfaces and abstract classes and minimal dependencies, while `-impl`
contain the corresponding implementation(s).

* [taverna-app-configuration-api](taverna-app-configuration-api/) - Apache Taverna Application Configuration API
* [taverna-app-configuration-impl](taverna-app-configuration-impl/) - Apache Taverna Application Configuration implementation
* [taverna-configuration-api](taverna-configuration-api/) - Apache Taverna Configuration API
* [taverna-configuration-impl](taverna-configuration-impl/) - Apache Taverna Configuration implementation
* [taverna-download-api](taverna-download-api/) - Apache Taverna Download API
* [taverna-download-impl](taverna-download-impl/) - Apache Taverna Download implementation
* [taverna-maven-plugin](taverna-maven-plugin/) - Apache Taverna Maven plugin for packaging and deploying Taverna plugins
* [taverna-osgi-schemas](taverna-osgi-schemas/) - Apache Taverna Taverna's OSGi XML schemas and JAXB bindings
* [taverna-plugin-api](taverna-plugin-api/) - Apache Taverna Plugin API
* [taverna-plugin-impl](taverna-plugin-impl/) - Apache Taverna Plugin implementation
* [taverna-update-api](taverna-update-api/) - Apache Taverna Update API
* [taverna-update-impl](taverna-update-impl/) - Apache Taverna Update implementation
* [taverna-osgi-launcher](taverna-osgi-launcher/) - Apache Taverna OSGi Framework Launcher
* [xml-parser-service](xml-parser-service/) - Apache Taverna XML Parser Service for OSGi
* [xml-transformer-service](xml-transformer-service/) - Apache Taverna XML Transformer Service for OSGi

See the [taverna-osgi javadoc](http://taverna.incubator.apache.org/javadoc/taverna-osgi/)
for details on each OSGi service. In brief:

* The [OsgiLauncher](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/osgilauncher/OsgiLauncher.html)
  starts the OSGi framework and installs the provided OSGi bundles. It is used by the
  [taverna-commandline-launcher](https://github.com/apache/incubator-taverna-commandline/blob/master/taverna-commandline-launcher/src/main/java/org/apache/taverna/commandline/TavernaCommandLine.java#L64)
  `main()` method.
* The [PluginManager](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/plugin/PluginManager.html) service
  from [taverna-plugin-api](taverna-plugin-api/)
  allow managing of
  plugins in an OSGi application, including online installation and updates of plugins. Each
  [Plugin](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/plugin/Plugin.html) provide a collection
  of OSGi bundles that are activated. The plugins and application profile are described in XML according to the
  [taverna-osgi-schemas](taverna-osgi-schemas/src/main/resources).
* The [UpdateManager](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/update/UpdateManager.html)
  can update the installed plugins by downloading from the configured plugin site URI.
* The [taverna-maven-plugin](taverna-maven-plugin/)   allows creating plugins for the Taverna `PluginManager`
  using Apache Maven, including deployment to plugin sites.
* The [DownloadManager](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/download/DownloadManager.html)
  service from [taverna-download-api](taverna-download-api/) provide convenience methods for downloading a `URI`
  to a `Path`, including hashsum checking using neighbouring `.sha1` or `.md5` URIs. Used by `PluginManager` and `UpdateManager`.
* The [ApplicationConfiguration](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/configuration/app/ApplicationConfiguration.html)
  service provide application installation details  like the startup directory and home directory for user configuration. The application should have a [conf/taverna.app.properties](https://github.com/apache/incubator-taverna-commandline/blob/master/taverna-commandline-product/src/main/etc/conf/taverna.app.properties)
  file in its startup folder, e.g. `taverna.app.name=taverna-cl-3.0.0` means `~/.taverna-cl-3.0.0/` will be the
  application's home directory on Unix.
* The [ConfigurationManager](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/configuration/ConfigurationManager.html)
  service from [taverna-configuration-api](taverna-configuration-api/) can configure any
  [Configurable](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/configuration/Configurable.html)
  like the [HTTPProxyConfiguration](http://taverna.incubator.apache.org/javadoc/taverna-osgi/org/apache/taverna/configuration/proxy/HttpProxyConfiguration.html)
  and the [workbench](https://github.com/apache/incubator-taverna-workbench/blob/master/taverna-configuration-api/src/main/java/org/apache/taverna/workbench/configuration/workbench/WorkbenchConfiguration.java)
  using preferences stored in the application home directory  
* The [xml-parser-service](xml-parser-service/)  and [xml-transformer-service](xml-transformer-service/)
  re-exposes xalan's and xerces's XML parser and XML transformer implementations within SOGi

## Spring services

The OSGi services should be
discoverable as [Spring](https://spring.io/) services,
e.g. by adding to
your `META-INF/spring/update-context-osgi.xml`:

```xml

    <beans:beans xmlns="http://www.springframework.org/schema/osgi" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    	xmlns:beans="http://www.springframework.org/schema/beans"
    	xsi:schemaLocation="
           http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/osgi http://www.springframework.org/schema/osgi/spring-osgi.xsd">

        <reference id="applicationConfiguration" interface="org.apache.taverna.configuration.app.ApplicationConfiguration" />
        <reference id="proxyConfiguration" interface="org.apache.taverna.configuration.proxy.HttpProxyConfiguration" />
        <reference id="configurationManager" interface="org.apache.taverna.configuration.ConfigurationManager" />
        <reference id="downloadManager" interface="org.apache.taverna.download.DownloadManager" />
        <reference id="pluginManager" interface="org.apache.taverna.plugin.PluginManager" />
        <reference id="updateManager" interface="org.apache.taverna.update.UpdateManager" />
    </beans:beans>
```


# Export restrictions

This distribution includes cryptographic software.
The country in which you currently reside may have restrictions
on the import, possession, use, and/or re-export to another country,
of encryption software. BEFORE using any encryption software,
please check your country's laws, regulations and policies
concerning the import, possession, or use, and re-export of
encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS),
has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1,
which includes information security software using or performing
cryptographic functions with asymmetric algorithms.
The form and manner of this Apache Software Foundation distribution makes
it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception
(see the BIS Export Administration Regulations, Section 740.13)
for both object code and source code.

The following provides more details on the included cryptographic software:

* [taverna-download-impl](taverna-download-impl) depend on the
  [Apache HttpComponents](https://hc.apache.org/) Client, which can
  initiate encrypted `https://` connections using
  [Java Secure Socket Extension](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
  (JSSE).
