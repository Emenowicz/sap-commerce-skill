# Extension Development

## Table of Contents
- [Extension Directory Structure](#extension-directory-structure)
- [extensioninfo.xml Configuration](#extensioninfoxml-configuration)
- [localextensions.xml Management](#localextensionsxml-management)
- [Extension Dependencies](#extension-dependencies)
- [Build Process](#build-process)
- [Cloud Deployment (CCv2)](#cloud-deployment-ccv2)
- [On-Premise Deployment](#on-premise-deployment)
- [Web Applications](#web-applications)
- [Resource Bundles](#resource-bundles)
- [System Setup Classes](#system-setup-classes)

## Extension Directory Structure

Standard extension layout:
```
customextension/
├── extensioninfo.xml          # Extension metadata
├── resources/
│   ├── customextension-items.xml    # Type definitions
│   ├── customextension-spring.xml   # Spring beans
│   └── localization/
│       └── customextension-locales_en.properties
├── src/
│   └── com/example/custom/
│       ├── constants/
│       │   └── CustomextensionConstants.java
│       ├── setup/
│       │   └── CustomextensionSystemSetup.java
│       ├── daos/
│       ├── services/
│       └── facades/
├── testsrc/                    # Unit tests
├── web/
│   └── webroot/
│       └── WEB-INF/
│           └── web.xml
├── hmc/                        # Backoffice configuration (legacy)
└── gensrc/                     # Generated sources (auto)
```

## extensioninfo.xml Configuration

Define extension metadata and dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensioninfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:noNamespaceSchemaLocation="extensioninfo.xsd">

    <extension name="customextension"
               classprefix="Custom"
               abstractextension="false"
               version="1.0.0">

        <!-- Required extensions -->
        <requires-extension name="commerceservices"/>
        <requires-extension name="acceleratorservices"/>

        <!-- Core extension settings -->
        <coremodule generated="true"
                    manager="de.hybris.platform.jalo.extension.ExtensionManager"/>

        <!-- Web module (optional) -->
        <webmodule webroot="/customextension"
                   jspcompile="true"/>

    </extension>
</extensioninfo>
```

### Key Attributes
- `name`: Unique extension identifier
- `classprefix`: Prefix for generated classes
- `requires-extension`: Declare dependencies
- `webmodule`: Configure web application

## localextensions.xml Management

Register extensions in `config/localextensions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hybrisconfig xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:noNamespaceSchemaLocation="resources/schemas/extensions.xsd">

    <extensions>
        <!-- Platform extensions -->
        <path dir="${HYBRIS_BIN_DIR}"/>

        <!-- Custom extensions -->
        <extension name="customextension"/>
        <extension dir="${HYBRIS_BIN_DIR}/custom/customextension"/>
    </extensions>
</hybrisconfig>
```

### Extension Paths
- Use `name` for extensions in standard locations
- Use `dir` for custom paths
- Order determines load sequence

## Extension Dependencies

### Dependency Declaration
In `extensioninfo.xml`:
```xml
<requires-extension name="commerceservices"/>
<requires-extension name="acceleratorfacades"/>
```

### Load Order
Platform resolves dependencies automatically. Check order:
```bash
ant extensionorder
```

### Circular Dependencies
Avoid by:
- Using interfaces in common extensions
- Splitting into smaller extensions
- Using Spring bean aliases

## Build Process

### Standard Commands
```bash
# Full clean build
ant clean all

# Build without cleaning
ant all

# Initialize database
ant initialize

# Update system
ant updatesystem

# Build specific extension
ant -Dextension=customextension
```

### Build Artifacts
- `gensrc/`: Generated Java sources
- `classes/`: Compiled classes
- `lib/`: Extension JARs

### Model Generation
After items.xml changes:
```bash
ant clean all
```

## Cloud Deployment (CCv2)

### manifest.json
```json
{
  "commerceSuiteVersion": "2211",
  "useConfig": {
    "properties": [
      {
        "location": "hybris/config/common.properties"
      },
      {
        "location": "hybris/config/accstorefront.properties",
        "persona": "development"
      }
    ],
    "solr": {
      "location": "hybris/config/solr"
    }
  },
  "extensions": [
    "commercewebservices",
    "yacceleratorstorefront",
    "customextension"
  ],
  "tests": {
    "extensions": [
      "customextension"
    ]
  },
  "aspects": [
    {
      "name": "backoffice",
      "properties": [
        {
          "key": "backoffice.fill.typefacade.cache.maxEntries",
          "value": "2000"
        }
      ],
      "webapps": [
        {"name": "backoffice", "contextPath": "/backoffice"}
      ]
    },
    {
      "name": "api",
      "properties": [
        {
          "key": "occ.rewrite.overlapping.paths.enabled",
          "value": "true"
        }
      ],
      "webapps": [
        {"name": "commercewebservices", "contextPath": "/occ"}
      ]
    },
    {
      "name": "storefront",
      "webapps": [
        {"name": "yacceleratorstorefront", "contextPath": ""}
      ]
    }
  ]
}
```

### Cloud Build
Push to repository → CCv2 builds automatically using manifest.json.

### Environment Configuration
Set properties via:
- CCv2 Portal environment variables
- `local.properties` overlay files

## On-Premise Deployment

### Traditional Build
```bash
cd ${HYBRIS_HOME}/bin/platform
. ./setantenv.sh
ant clean all
ant initialize
./hybrisserver.sh
```

### Server Configuration
Edit `config/local.properties`:
```properties
# Database
db.url=jdbc:mysql://localhost:3306/hybris
db.driver=com.mysql.jdbc.Driver
db.username=hybris
db.password=hybris

# Server
tomcat.http.port=9001
tomcat.ssl.port=9002
```

### Production Deployment
1. Build on CI server
2. Package as ZIP/TAR
3. Extract to target server
4. Configure local.properties
5. Initialize or update
6. Start server

## Web Applications

### Web Module Configuration
In `extensioninfo.xml`:
```xml
<webmodule webroot="/customextension" jspcompile="true"/>
```

### web.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="3.1">
    <display-name>Custom Extension</display-name>

    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

## Resource Bundles

### Localization Files
Create in `resources/localization/`:
```properties
# customextension-locales_en.properties
type.CustomProduct.name=Custom Product
type.CustomProduct.customField.name=Custom Field
```

### Adding Languages
```properties
# customextension-locales_de.properties
type.CustomProduct.name=Benutzerdefiniertes Produkt
```

### Access in Code
```java
Localization.getLocalizedString("type.CustomProduct.name");
```

## System Setup Classes

### SystemSetup Annotation
```java
@SystemSetup(extension = CustomextensionConstants.EXTENSIONNAME)
public class CustomextensionSystemSetup {

    @SystemSetup(type = Type.ESSENTIAL, process = Process.ALL)
    public void createEssentialData() {
        // Essential data for extension
    }

    @SystemSetup(type = Type.PROJECT, process = Process.ALL)
    public void createProjectData(final SystemSetupContext context) {
        // Sample/test data
        if (context.getBooleanParameter("createSampleData")) {
            // Create sample data
        }
    }
}
```

### Setup Types
- `ESSENTIAL`: Required data (types, permissions)
- `PROJECT`: Sample/demo data

### Process Types
- `ALL`: Run during initialize and update
- `INIT`: Only during initialize
- `UPDATE`: Only during update
