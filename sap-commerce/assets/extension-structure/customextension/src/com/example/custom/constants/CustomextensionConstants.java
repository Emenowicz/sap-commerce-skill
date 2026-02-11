/*
 * CustomextensionConstants.java
 * Constants class for the extension.
 * Contains extension name and other constant values.
 */
package com.example.custom.constants;

/**
 * Global constants for the customextension.
 */
public final class CustomextensionConstants {

    /** Extension name constant - matches extensioninfo.xml name */
    public static final String EXTENSIONNAME = "customextension";

    /** Default status code */
    public static final String DEFAULT_STATUS = "ACTIVE";

    /** Configuration keys */
    public static final String CONFIG_ENABLED = "customextension.enabled";
    public static final String CONFIG_MAX_ITEMS = "customextension.maxItems";

    private CustomextensionConstants() {
        // Private constructor to prevent instantiation
    }
}
