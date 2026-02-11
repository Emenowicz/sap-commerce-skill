#!/bin/bash
# generate-extension.sh
# Scaffold a new SAP Commerce extension structure
# Usage: ./generate-extension.sh
# Prompts for extension name and dependencies, creates directory structure

set -e

echo "=== SAP Commerce Extension Generator ==="
echo ""

# Prompt for extension name
read -p "Enter extension name (lowercase, no spaces): " EXT_NAME

# Validate extension name
if [[ ! "$EXT_NAME" =~ ^[a-z][a-z0-9]*$ ]]; then
    echo "Error: Extension name must start with a letter and contain only lowercase letters and numbers"
    exit 1
fi

# Prompt for dependencies
read -p "Enter required extensions (comma-separated, e.g., commerceservices,acceleratorservices): " DEPENDENCIES

# Prompt for output directory
read -p "Enter output directory [./]: " OUTPUT_DIR
OUTPUT_DIR=${OUTPUT_DIR:-.}

EXT_PATH="$OUTPUT_DIR/$EXT_NAME"

# Check if directory exists
if [ -d "$EXT_PATH" ]; then
    echo "Error: Directory $EXT_PATH already exists"
    exit 1
fi

echo ""
echo "Creating extension: $EXT_NAME"
echo "Location: $EXT_PATH"
echo ""

# Create directory structure
mkdir -p "$EXT_PATH/resources/localization"
mkdir -p "$EXT_PATH/src/com/company/$EXT_NAME/constants"
mkdir -p "$EXT_PATH/src/com/company/$EXT_NAME/setup"
mkdir -p "$EXT_PATH/src/com/company/$EXT_NAME/daos/impl"
mkdir -p "$EXT_PATH/src/com/company/$EXT_NAME/services/impl"
mkdir -p "$EXT_PATH/src/com/company/$EXT_NAME/facades/impl"
mkdir -p "$EXT_PATH/testsrc"

# Generate extensioninfo.xml
cat > "$EXT_PATH/extensioninfo.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<extensioninfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:noNamespaceSchemaLocation="extensioninfo.xsd">

    <extension name="$EXT_NAME"
               classprefix="$(echo ${EXT_NAME^})"
               version="1.0.0">

EOF

# Add dependencies
if [ -n "$DEPENDENCIES" ]; then
    IFS=',' read -ra DEPS <<< "$DEPENDENCIES"
    for dep in "${DEPS[@]}"; do
        dep=$(echo "$dep" | xargs)  # trim whitespace
        echo "        <requires-extension name=\"$dep\"/>" >> "$EXT_PATH/extensioninfo.xml"
    done
fi

cat >> "$EXT_PATH/extensioninfo.xml" << EOF

        <coremodule generated="true"
                    manager="de.hybris.platform.jalo.extension.ExtensionManager"
                    packageroot="com.company.$EXT_NAME"/>

    </extension>
</extensioninfo>
EOF

# Generate items.xml
cat > "$EXT_PATH/resources/${EXT_NAME}-items.xml" << EOF
<?xml version="1.0" encoding="ISO-8859-1"?>
<items xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:noNamespaceSchemaLocation="items.xsd">

    <enumtypes>
        <!-- Define enumerations here -->
    </enumtypes>

    <itemtypes>
        <!-- Define item types here -->
    </itemtypes>

</items>
EOF

# Generate spring.xml
cat > "$EXT_PATH/resources/${EXT_NAME}-spring.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd">

    <context:component-scan base-package="com.company.$EXT_NAME"/>

    <!-- DAO Beans -->

    <!-- Service Beans -->

    <!-- Facade Beans -->

</beans>
EOF

# Generate localization properties
cat > "$EXT_PATH/resources/localization/${EXT_NAME}-locales_en.properties" << EOF
# ${EXT_NAME} localization - English
# Add localized strings here
EOF

# Generate Constants class
CONST_CLASS="$(echo ${EXT_NAME^})Constants"
cat > "$EXT_PATH/src/com/company/$EXT_NAME/constants/${CONST_CLASS}.java" << EOF
package com.company.$EXT_NAME.constants;

/**
 * Global constants for $EXT_NAME extension.
 */
public final class ${CONST_CLASS} {

    public static final String EXTENSIONNAME = "$EXT_NAME";

    private ${CONST_CLASS}() {
        // Private constructor
    }
}
EOF

# Generate SystemSetup class
SETUP_CLASS="$(echo ${EXT_NAME^})SystemSetup"
cat > "$EXT_PATH/src/com/company/$EXT_NAME/setup/${SETUP_CLASS}.java" << EOF
package com.company.$EXT_NAME.setup;

import com.company.$EXT_NAME.constants.${CONST_CLASS};

import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetup.Process;
import de.hybris.platform.core.initialization.SystemSetup.Type;
import de.hybris.platform.core.initialization.SystemSetupContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SystemSetup(extension = ${CONST_CLASS}.EXTENSIONNAME)
public class ${SETUP_CLASS} {

    private static final Logger LOG = LoggerFactory.getLogger(${SETUP_CLASS}.class);

    @SystemSetup(type = Type.ESSENTIAL, process = Process.ALL)
    public void createEssentialData() {
        LOG.info("Creating essential data for $EXT_NAME");
    }

    @SystemSetup(type = Type.PROJECT, process = Process.ALL)
    public void createProjectData(final SystemSetupContext context) {
        LOG.info("Creating project data for $EXT_NAME");
    }
}
EOF

echo "Extension created successfully!"
echo ""
echo "Next steps:"
echo "1. Add extension to localextensions.xml:"
echo "   <extension name=\"$EXT_NAME\"/>"
echo ""
echo "2. Build the platform:"
echo "   ant clean all"
echo ""
echo "3. Initialize or update the system:"
echo "   ant initialize  (new installation)"
echo "   ant updatesystem (existing installation)"
