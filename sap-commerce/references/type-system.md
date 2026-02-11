# Type System Reference

## Table of Contents
- [items.xml Structure](#itemsxml-structure)
- [Type Hierarchy and Inheritance](#type-hierarchy-and-inheritance)
- [Attribute Types and Modifiers](#attribute-types-and-modifiers)
- [Relations](#relations)
- [Enumerations](#enumerations)
- [Collection Types](#collection-types)
- [Deployment vs Runtime Types](#deployment-vs-runtime-types)
- [Build Process](#build-process)

## items.xml Structure

The `items.xml` file defines the data model. Located in `resources/<extension>-items.xml`:

```xml
<?xml version="1.0" encoding="ISO-8859-1"?>
<items xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:noNamespaceSchemaLocation="items.xsd">

    <atomictypes>
        <!-- Primitive type mappings -->
    </atomictypes>

    <collectiontypes>
        <!-- Collection definitions -->
    </collectiontypes>

    <enumtypes>
        <!-- Enumeration definitions -->
    </enumtypes>

    <maptypes>
        <!-- Map type definitions -->
    </maptypes>

    <relations>
        <!-- Relationship definitions -->
    </relations>

    <itemtypes>
        <!-- Item type definitions -->
    </itemtypes>
</items>
```

## Type Hierarchy and Inheritance

Define item types with inheritance using `extends`:

```xml
<itemtype code="CustomProduct" extends="Product"
          autocreate="true" generate="true"
          jaloclass="com.example.jalo.CustomProduct">
    <deployment table="CustomProducts" typecode="10001"/>
    <attributes>
        <!-- Custom attributes -->
    </attributes>
</itemtype>
```

Key attributes:
- `code`: Unique type identifier
- `extends`: Parent type (defaults to GenericItem)
- `autocreate`: Create type during initialization
- `generate`: Generate model class
- `jaloclass`: Deprecated Jalo layer class

> **WARNING: Jalo Layer Deprecated.** The `jaloclass` attribute is deprecated since SAP Commerce 6.0. Do **not** extend generated Jalo classes or place business logic in the Jalo layer. Use the ServiceLayer with `ModelService` instead. The `jaloclass` attribute exists only for backward compatibility and will be removed in a future release.

## Attribute Types and Modifiers

### Primitive Types
| Java Type | items.xml Type |
|-----------|----------------|
| String | java.lang.String |
| Integer | java.lang.Integer |
| Long | java.lang.Long |
| Double | java.lang.Double |
| Boolean | java.lang.Boolean |
| Date | java.util.Date |

### Attribute Definition
```xml
<attribute qualifier="customField" type="java.lang.String">
    <description>Custom field description</description>
    <modifiers read="true" write="true" optional="true" unique="false"
               initial="false" search="true"/>
    <persistence type="property"/>
    <defaultvalue>"default"</defaultvalue>
</attribute>
```

### Modifier Attributes
- `read/write`: Getter/setter generation
- `optional`: Allow null values
- `unique`: Enforce uniqueness
- `initial`: Set only on creation
- `search`: Index for FlexibleSearch
- `partof`: Cascade delete with parent

### Persistence Types
- `property`: Stored in database column
- `dynamic`: Computed via attributeHandler
- `jalo`: Deprecated, use dynamic

### Localized Attributes
```xml
<attribute qualifier="name" type="localized:java.lang.String">
    <persistence type="property"/>
</attribute>
```

## Relations

### One-to-Many
```xml
<relation code="Category2Product" localized="false">
    <sourceElement type="Category" qualifier="supercategories" cardinality="many"/>
    <targetElement type="Product" qualifier="products" cardinality="many" collectiontype="list"/>
</relation>
```

### Many-to-Many with Deployment
```xml
<relation code="Product2ProductTag" localized="false">
    <deployment table="Prod2ProdTag" typecode="10100"/>
    <sourceElement type="Product" qualifier="productTags" cardinality="many"/>
    <targetElement type="ProductTag" qualifier="products" cardinality="many"/>
</relation>
```

### Relation Properties
- `cardinality`: one or many
- `collectiontype`: list, set, collection
- `ordered`: Maintain insertion order
- `navigable`: Generate accessor (default true)

## Enumerations

### Static Enum (Fixed Values)
```xml
<enumtype code="OrderStatus" autocreate="true" generate="true" dynamic="false">
    <value code="CREATED"/>
    <value code="PROCESSING"/>
    <value code="COMPLETED"/>
    <value code="CANCELLED"/>
</enumtype>
```

### Dynamic Enum (Extensible at Runtime)
```xml
<enumtype code="ShippingCarrier" autocreate="true" generate="true" dynamic="true">
    <value code="DHL"/>
    <value code="UPS"/>
</enumtype>
```

Use in attributes:
```xml
<attribute qualifier="status" type="OrderStatus">
    <modifiers optional="false"/>
    <persistence type="property"/>
</attribute>
```

## Collection Types

### Collection Definition
```xml
<collectiontype code="ProductList" elementtype="Product" autocreate="true" type="list"/>
<collectiontype code="StringSet" elementtype="java.lang.String" autocreate="true" type="set"/>
```

### Map Definition
```xml
<maptype code="LocalizedString" argumenttype="Language" returntype="java.lang.String"
         autocreate="true" generate="false"/>
```

## Deployment vs Runtime Types

### Deployment Configuration
```xml
<deployment table="CustomProducts" typecode="10001"/>
```

- `table`: Database table name
- `typecode`: Unique identifier (10000-32767 for custom)

### Type Categories
- **Configured types**: Defined in items.xml, compiled at build
- **Runtime types**: Created dynamically (rare, via Type System API)

### Table Strategies
- Single table: All subtypes in parent table (default)
- Joined: Separate table per subtype (use deployment)

## Build Process

### Generate Models
```bash
ant clean all
```

### Model Generation
1. Platform reads all `*-items.xml`
2. Validates type definitions
3. Generates Java model classes in `gensrc`
4. Compiles to `classes`

### Check Typecodes
```bash
ant typecodecheck
```

### Update Running System
```bash
ant updatesystem
```
Or via HAC: Platform > Update

### Best Practices
- Use typecodes 10000+ for custom types
- Run `ant clean all` after items.xml changes
- Never reuse deleted typecodes
- Use `search="true"` for queryable attributes
- Prefer `optional="false"` for required fields
