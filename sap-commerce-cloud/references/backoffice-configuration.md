# Backoffice Configuration

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Widget Configuration](#widget-configuration)
- [Custom Editors and Renderers](#custom-editors-and-renderers)
- [Search Configuration](#search-configuration)
- [List View Customization](#list-view-customization)
- [Wizards](#wizards)
- [Deep Links](#deep-links)
- [Custom Backoffice Extensions](#custom-backoffice-extensions)

## Overview

Backoffice is SAP Commerce's administration UI, built on the cockpitng framework (ZK-based). Customization points include:
- Widget configuration (layout, properties, visibility)
- Custom editors for type attributes
- Search and list view customization
- Wizards for guided workflows
- Custom extensions with new widgets

## Architecture

### Component Stack

```
Browser → ZK Framework → cockpitng Widgets → SAP Commerce Services → Database
```

### Key Configuration Files

| File | Purpose | Location |
|------|---------|----------|
| `backoffice-config.xml` | Widget and layout configuration | `resources/backoffice/` |
| `cockpit-config.xml` | Legacy widget configuration | `resources/cockpitng/` |
| `widgets.xml` | Widget definitions | `resources/widgets/` |
| `labels_en.properties` | UI labels | `resources/backoffice/labels/` |

### Configuration Merge Order

Configurations merge by priority: platform defaults → extension configs → custom configs. Later configurations override earlier ones for the same widget/context.

## Widget Configuration

### backoffice-config.xml Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://www.hybris.com/cockpit/config"
        xmlns:y="http://www.hybris.com/cockpit/config/hybris">

    <!-- Configure the editor area for a specific type -->
    <context type="Product" component="editor-area">
        <editorArea:editorArea xmlns:editorArea="http://www.hybris.com/cockpitng/component/editorArea">
            <editorArea:tab name="hmc.properties" position="0">
                <editorArea:section name="hmc.essential">
                    <editorArea:attribute qualifier="code" readonly="true"/>
                    <editorArea:attribute qualifier="name"/>
                    <editorArea:attribute qualifier="description" editor="com.hybris.cockpitng.editor.localized"/>
                    <editorArea:attribute qualifier="ean"/>
                    <editorArea:attribute qualifier="approvalStatus"/>
                    <editorArea:attribute qualifier="supercategories"/>
                </editorArea:section>
                <editorArea:section name="hmc.pricing">
                    <editorArea:attribute qualifier="europe1Prices"/>
                    <editorArea:attribute qualifier="europe1Discounts"/>
                    <editorArea:attribute qualifier="europe1Taxes"/>
                </editorArea:section>
            </editorArea:tab>
            <editorArea:tab name="hmc.media" position="1">
                <editorArea:section name="hmc.media.images">
                    <editorArea:attribute qualifier="picture"/>
                    <editorArea:attribute qualifier="thumbnail"/>
                    <editorArea:attribute qualifier="galleryImages"/>
                </editorArea:section>
            </editorArea:tab>
        </editorArea:editorArea>
    </context>

</config>
```

### Hiding/Showing Attributes

```xml
<!-- Hide an attribute in the editor -->
<context type="Product" component="editor-area">
    <editorArea:editorArea xmlns:editorArea="http://www.hybris.com/cockpitng/component/editorArea">
        <editorArea:tab name="hmc.properties">
            <editorArea:section name="hmc.essential">
                <editorArea:attribute qualifier="internalNotes" visible="false"/>
            </editorArea:section>
        </editorArea:tab>
    </editorArea:editorArea>
</context>
```

### Making Attributes Read-Only

```xml
<editorArea:attribute qualifier="code" readonly="true"/>
<editorArea:attribute qualifier="creationtime" readonly="true"/>
```

## Custom Editors and Renderers

### Registering a Custom Editor

```xml
<!-- In backoffice-config.xml -->
<context type="Product" component="editor-area">
    <editorArea:editorArea xmlns:editorArea="http://www.hybris.com/cockpitng/component/editorArea">
        <editorArea:tab name="hmc.properties">
            <editorArea:section name="hmc.custom">
                <editorArea:attribute qualifier="customRating"
                                      editor="com.example.backoffice.editors.StarRatingEditor"/>
            </editorArea:section>
        </editorArea:tab>
    </editorArea:editorArea>
</context>
```

### Custom Editor Implementation

```java
package com.example.backoffice.editors;

import com.hybris.cockpitng.editors.CockpitEditorRenderer;
import com.hybris.cockpitng.editors.EditorContext;
import com.hybris.cockpitng.editors.EditorListener;

import org.zkoss.zul.Intbox;

public class StarRatingEditor implements CockpitEditorRenderer<Integer> {

    @Override
    public void render(final org.zkoss.zk.ui.Component parent,
                       final EditorContext<Integer> context,
                       final EditorListener<Integer> listener) {

        final Intbox intbox = new Intbox();
        intbox.setConstraint("min 0 max 5: Rating must be between 0 and 5");

        if (context.getInitialValue() != null) {
            intbox.setValue(context.getInitialValue());
        }

        intbox.addEventListener("onChange", event -> {
            listener.onValueChanged(intbox.getValue());
        });

        intbox.setParent(parent);
    }
}
```

### Register Editor in Spring

```xml
<bean id="starRatingEditor"
      class="com.example.backoffice.editors.StarRatingEditor"
      scope="prototype"/>
```

## Search Configuration

### Simple Search (Quick Search Bar)

```xml
<context type="Product" component="simple-search">
    <simpleSearch:simpleSearch xmlns:simpleSearch="http://www.hybris.com/cockpitng/config/simpleSearch">
        <simpleSearch:field name="code" selected="true"/>
        <simpleSearch:field name="name" selected="true"/>
        <simpleSearch:field name="ean"/>
        <simpleSearch:field name="catalogVersion" operator="equals"/>
    </simpleSearch:simpleSearch>
</context>
```

### Advanced Search

```xml
<context type="Product" component="advanced-search">
    <simpleSearch:simpleSearch xmlns:simpleSearch="http://www.hybris.com/cockpitng/config/simpleSearch">
        <simpleSearch:field name="code" operator="contains" selected="true"/>
        <simpleSearch:field name="name" operator="contains" selected="true"/>
        <simpleSearch:field name="approvalStatus" operator="equals"/>
        <simpleSearch:field name="supercategories" operator="equals"/>
        <simpleSearch:field name="catalogVersion" operator="equals" selected="true"/>
        <simpleSearch:field name="creationtime" operator="greater"/>
        <simpleSearch:field name="modifiedtime" operator="greater"/>
    </simpleSearch:simpleSearch>
</context>
```

### Sort Configuration

```xml
<context type="Product" component="simple-search">
    <simpleSearch:simpleSearch xmlns:simpleSearch="http://www.hybris.com/cockpitng/config/simpleSearch">
        <simpleSearch:sort name="name" ascending="true" default="true"/>
        <simpleSearch:sort name="code" ascending="true"/>
        <simpleSearch:sort name="creationtime" ascending="false"/>
    </simpleSearch:simpleSearch>
</context>
```

## List View Customization

### Column Configuration

```xml
<context type="Product" component="listview">
    <listview:listview xmlns:listview="http://www.hybris.com/cockpitng/component/listView">
        <listview:column qualifier="code" width="150" sortable="true"/>
        <listview:column qualifier="name" width="auto" sortable="true" spring-bean="localizedListCellRenderer"/>
        <listview:column qualifier="approvalStatus" width="120" sortable="true"/>
        <listview:column qualifier="catalogVersion" width="200"/>
        <listview:column qualifier="creationtime" width="150" sortable="true"/>
    </listview:listview>
</context>
```

### Custom Cell Renderer

For specialized display in list columns:

```java
package com.example.backoffice.renderers;

import com.hybris.cockpitng.core.config.impl.jaxb.listview.ListColumn;
import com.hybris.cockpitng.dataaccess.facades.type.DataType;
import com.hybris.cockpitng.engine.WidgetInstanceManager;
import com.hybris.cockpitng.widgets.common.listview.renderer.AbstractListCellRenderer;

import org.zkoss.zul.Listcell;

public class StatusBadgeRenderer extends AbstractListCellRenderer {

    @Override
    public void render(final Listcell listcell,
                       final WidgetInstanceManager widgetInstanceManager,
                       final Object item,
                       final DataType dataType,
                       final ListColumn column) {

        final String status = getPropertyValue(item, column.getQualifier());
        listcell.setLabel(status);

        // Color-code the status
        if ("APPROVED".equals(status)) {
            listcell.setStyle("color: green;");
        } else if ("UNAPPROVED".equals(status)) {
            listcell.setStyle("color: red;");
        }
    }
}
```

## Wizards

### Wizard Configuration

```xml
<context type="Product" component="wizard">
    <simpleWizard:simpleWizard xmlns:simpleWizard="http://www.hybris.com/cockpitng/config/simpleWizard">
        <simpleWizard:step id="basic" label="hmc.wizard.product.basic">
            <simpleWizard:property qualifier="code" required="true"/>
            <simpleWizard:property qualifier="name" required="true"/>
            <simpleWizard:property qualifier="catalogVersion" required="true"/>
        </simpleWizard:step>
        <simpleWizard:step id="details" label="hmc.wizard.product.details">
            <simpleWizard:property qualifier="description"/>
            <simpleWizard:property qualifier="ean"/>
            <simpleWizard:property qualifier="approvalStatus"/>
        </simpleWizard:step>
        <simpleWizard:step id="categories" label="hmc.wizard.product.categories">
            <simpleWizard:property qualifier="supercategories"/>
        </simpleWizard:step>
    </simpleWizard:simpleWizard>
</context>
```

### Custom Wizard Handler

```java
package com.example.backoffice.wizards;

import com.hybris.cockpitng.config.jaxb.wizard.CustomType;
import com.hybris.cockpitng.widgets.configurableflow.FlowActionHandler;
import com.hybris.cockpitng.widgets.configurableflow.FlowActionHandlerAdapter;

import java.util.Map;

public class ProductCreationWizardHandler implements FlowActionHandler {

    @Override
    public void perform(final CustomType customType,
                        final FlowActionHandlerAdapter adapter,
                        final Map<String, String> params) {
        // Custom logic before/after product creation
        final Object product = adapter.getWidgetInstanceManager()
                .getModel().get("newProduct");

        // Perform additional setup, validation, etc.

        adapter.done();
    }
}
```

## Deep Links

Navigate directly to specific items in Backoffice:

### URL Format

```
https://<host>:9002/backoffice/#/cxtype/<TypeCode>/<PK>
```

### Examples

```
# Open a specific product
https://localhost:9002/backoffice/#/cxtype/Product/8796093054977

# Open a specific order
https://localhost:9002/backoffice/#/cxtype/Order/8796093087745

# Open a category
https://localhost:9002/backoffice/#/cxtype/Category/8796093022209
```

### Programmatic Deep Link Generation

```java
public String generateBackofficeLink(final ItemModel item) {
    return String.format("/backoffice/#/cxtype/%s/%s",
            item.getItemtype(), item.getPk().getLongValue());
}
```

## Custom Backoffice Extensions

### Extension Structure

```
mybackoffice/
├── backoffice/
│   ├── resources/
│   │   ├── widgets/
│   │   │   └── mywidget/
│   │   │       ├── definition.xml
│   │   │       ├── mywidget.zul
│   │   │       └── mywidget.js
│   │   ├── labels/
│   │   │   └── labels_en.properties
│   │   └── mybackoffice-backoffice-config.xml
│   └── src/
│       └── com/example/backoffice/widgets/
│           └── MyWidgetController.java
├── resources/
│   └── mybackoffice-backoffice-spring.xml
└── extensioninfo.xml
```

### Widget Definition

```xml
<!-- resources/widgets/mywidget/definition.xml -->
<widget-definition
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:noNamespaceSchemaLocation="http://www.hybris.com/schema/cockpitng/widget-definition.xsd"
    id="com.example.backoffice.mywidget"
    name="My Custom Widget">

    <controller class="com.example.backoffice.widgets.MyWidgetController"/>
    <view src="mywidget.zul"/>

    <setting key="title" type="String" default-value="My Widget"/>

    <socketEvent name="currentObject" direction="input"/>
    <socketEvent name="objectUpdated" direction="output"/>
</widget-definition>
```

### Widget Controller

```java
package com.example.backoffice.widgets;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;

import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;

public class MyWidgetController extends DefaultWidgetController {

    @Wire
    private Label titleLabel;

    @SocketEvent(socketId = "currentObject")
    public void onCurrentObjectChanged(final Object item) {
        if (item != null) {
            titleLabel.setValue("Selected: " + item.toString());
        }
    }
}
```

### Required Dependencies in extensioninfo.xml

```xml
<extension name="mybackoffice">
    <requires-extension name="backoffice"/>
    <requires-extension name="platformbackoffice"/>
</extension>
```
