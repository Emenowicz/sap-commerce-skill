# ImpEx Guide

## Table of Contents
- [ImpEx Syntax](#impex-syntax)
- [Operations](#operations)
- [Header Declarations](#header-declarations)
- [Macros and Variables](#macros-and-variables)
- [Value Translators](#value-translators)
- [Default Values](#default-values)
- [Multi-Valued Attributes](#multi-valued-attributes)
- [Media Import](#media-import)
- [User and Permission Setup](#user-and-permission-setup)
- [Batch Processing](#batch-processing)
- [Error Handling](#error-handling)
- [Cloud Patterns](#cloud-patterns)

## ImpEx Syntax

### Basic Structure
```impex
# Comment line
$macro=value

INSERT_UPDATE Type;attribute1[unique=true];attribute2;attribute3
;value1;value2;value3
;value4;value5;value6
```

### Line Continuation
```impex
INSERT_UPDATE Product;code[unique=true];name[lang=en];\
                      description[lang=en];catalogVersion(catalog(id),version)
```

## Operations

### INSERT
Create new items only (fails if exists):
```impex
INSERT Product;code[unique=true];name
;PROD001;Product One
```

### UPDATE
Update existing items only (fails if not exists):
```impex
UPDATE Product;code[unique=true];name
;PROD001;Updated Name
```

### INSERT_UPDATE
Create or update (upsert):
```impex
INSERT_UPDATE Product;code[unique=true];name;catalogVersion(catalog(id),version)
;PROD001;My Product;Default:Online
```

### REMOVE
Delete items:
```impex
REMOVE Product;code[unique=true];catalogVersion(catalog(id),version)
;PROD001;Default:Online
```

## Header Declarations

### Basic Header
```impex
INSERT_UPDATE Product;code[unique=true];name
```

### Modifiers
- `[unique=true]`: Identifies unique key for matching
- `[lang=en]`: Localized attribute language
- `[default=value]`: Default if not provided
- `[mode=append]`: Append to collection instead of replace
- `[translator=class]`: Custom value translator

### Reference Attributes
```impex
;catalogVersion(catalog(id),version)  # Nested reference
;supercategories(code,catalogVersion(catalog(id),version))  # Complex reference
```

## Macros and Variables

### Define Macros
```impex
$catalog=Default
$version=Online
$catalogVersion=catalogVersion(catalog(id),version)[unique=true,default=$catalog:$version]

INSERT_UPDATE Product;code[unique=true];$catalogVersion;name
;PROD001;;My Product
```

### Common Macros
```impex
$productCatalog=electronicsProductCatalog
$productCV=catalogVersion(catalog(id[default=$productCatalog]),version[default='Online'])
$contentCV=catalogVersion(CatalogVersion.catalog(Catalog.id[default='electronicsContentCatalog']),CatalogVersion.version[default='Online'])
$lang=en
```

### Variable Reference
```impex
$baseProduct=PROD001

INSERT_UPDATE VariantProduct;code[unique=true];baseProduct(code)
;VAR001;$baseProduct
```

## Value Translators

### Date Translator
```impex
INSERT_UPDATE Product;code[unique=true];onlineDate[dateformat=dd.MM.yyyy]
;PROD001;01.01.2024
```

### Boolean Translator
```impex
INSERT_UPDATE Product;code[unique=true];approved[default=true]
;PROD001;
```

### Enum Translator
```impex
INSERT_UPDATE Product;code[unique=true];approvalStatus(code)
;PROD001;approved
```

### Custom Translator
```impex
;price[translator=de.hybris.platform.impex.jalo.translators.ItemPKTranslator]
```

## Default Values

### In Header
```impex
INSERT_UPDATE Product;code[unique=true];catalogVersion(catalog(id),version)[default=Default:Online]
;PROD001;
```

### Macro Default
```impex
$defaultCatalog=Default:Online
INSERT_UPDATE Product;code[unique=true];catalogVersion(catalog(id),version)[default=$defaultCatalog]
```

## Multi-Valued Attributes

### Collection (append)
```impex
INSERT_UPDATE Product;code[unique=true];supercategories(code)[mode=append]
;PROD001;category1,category2
```

### Collection (replace)
```impex
INSERT_UPDATE Product;code[unique=true];supercategories(code)
;PROD001;category1,category2
```

### Remove from Collection
```impex
UPDATE Product;code[unique=true];supercategories(code)[mode=remove]
;PROD001;oldCategory
```

## Media Import

### Media from File
```impex
$mediaContainer=@media[translator=de.hybris.platform.impex.jalo.media.MediaDataTranslator]

INSERT_UPDATE Media;code[unique=true];$mediaContainer;mime[default='image/jpeg'];catalogVersion(catalog(id),version)
;prod001_image;/images/prod001.jpg;;Default:Online
```

### Media from URL
```impex
INSERT_UPDATE Media;code[unique=true];@media[translator=de.hybris.platform.impex.jalo.media.MediaDataTranslator];mime
;logo;jar:com.example.setup&/images/logo.png;image/png
```

### Assign to Product
```impex
INSERT_UPDATE Product;code[unique=true];picture(code);thumbnail(code)
;PROD001;prod001_image;prod001_thumb
```

## User and Permission Setup

### Create Users
```impex
INSERT_UPDATE Customer;uid[unique=true];password[default=$defaultPassword];name;groups(uid)
;customer@test.com;;Test Customer;customergroup
```

### Create User Groups
```impex
INSERT_UPDATE CustomerGroup;uid[unique=true];locname[lang=en];groups(uid)
;goldcustomers;Gold Customers;customergroup
```

### Assign Permissions
```impex
INSERT_UPDATE UserRight;code[unique=true];itemtype(code);attribute;positive
;read_product_data;Product;;true
```

### B2B Users and Units
```impex
INSERT_UPDATE B2BUnit;uid[unique=true];name
;CustomUnit;Custom Business Unit

INSERT_UPDATE B2BCustomer;uid[unique=true];name;defaultB2BUnit(uid);groups(uid)
;b2b@company.com;B2B Customer;CustomUnit;b2bcustomergroup
```

## Batch Processing

### Large Imports
```impex
# Set batch size for performance
#% impex.setMaxThreads(4);
#% impex.enableCodeExecution(true);

INSERT_UPDATE Product;code[unique=true];name
;P001;Product 1
;P002;Product 2
# ... thousands more
```

### Disable Interceptors
```impex
#% impex.setValidationMode(ImpexValidationModeEnum.STRICT);
#% import.property.disable.interceptors=true
```

### Transaction Control
```impex
#% impex.startTransaction();
INSERT_UPDATE Product;code[unique=true];name
;P001;Product 1
#% impex.commitTransaction();
```

## Error Handling

### Validation Mode
```impex
#% impex.setValidationMode(ImpexValidationModeEnum.STRICT);  # Fail on errors
#% impex.setValidationMode(ImpexValidationModeEnum.RELAXED); # Skip errors
```

### Common Errors
- Missing required attributes
- Invalid reference (item not found)
- Unique constraint violation
- Type mismatch

### Deferred References
```impex
# For circular references, use mode=append or import in multiple passes
INSERT_UPDATE Category;code[unique=true];supercategories(code)[mode=append]
;subcat;parentcat
```

## Cloud Patterns

### Data Initialization (CCv2)
Structure in `hybris/bin/custom/extensionname/resources/impex/`:
```
impex/
├── essentialdata/
│   └── essentialdata.impex
└── projectdata/
    ├── 01-users.impex
    └── 02-products.impex
```

### Manifest.json Reference
```json
{
  "initialDataLoading": {
    "order": 1,
    "importPath": "/impex/essentialdata"
  }
}
```

### Environment-Specific Data
```impex
$environment=$config-environment

#% if: "$environment".equals("PROD");
INSERT_UPDATE CMSSite;uid[unique=true];active
;electronics;true
#% endif:
```
