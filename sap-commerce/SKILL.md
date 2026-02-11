---
name: sap-commerce
description: Provides comprehensive SAP Commerce Cloud (formerly Hybris) development guidance including type system modeling, service layer architecture, data management with ImpEx and FlexibleSearch, OCC API customization, and B2C/B2B accelerator patterns. Use when the user asks to "create SAP Commerce extensions", "define item types in items.xml", "write ImpEx scripts", "implement service layer components (facades/services/DAOs)", "customize OCC REST APIs", "work with FlexibleSearch queries", "customize B2C or B2B accelerators", "configure Spring beans", mentions "Hybris development" or "SAP Commerce Cloud platform", or asks about troubleshooting SAP Commerce issues.
---

# SAP Commerce Development

## Overview

SAP Commerce Cloud (formerly Hybris) is an enterprise e-commerce platform built on Java and Spring. This skill provides guidance for extension development, type system modeling, service layer implementation, data management, API customization, and accelerator patterns for both Cloud (CCv2) and On-Premise deployments.

## Core Architecture

### Type System
Data modeling via `items.xml` defines item types, attributes, relations, and enumerations. Types are compiled into Java classes during build. See [type-system.md](references/type-system.md) for syntax and patterns.

### Extensions
Modular architecture where functionality lives in extensions. Each extension has `extensioninfo.xml` for metadata, `items.xml` for types, and `*-spring.xml` for beans. See [extension-development.md](references/extension-development.md).

### Service Layer
Four-layer architecture: **Facade** (API for controllers) → **Service** (business logic) → **DAO** (data access) → **Model** (generated from types). See [service-layer-architecture.md](references/service-layer-architecture.md).

### Data Management
**ImpEx**: Scripting language for data import/export. See [impex-guide.md](references/impex-guide.md).
**FlexibleSearch**: SQL-like query language for items. See [flexiblesearch-reference.md](references/flexiblesearch-reference.md).

### OCC API
RESTful web services exposing commerce functionality. Controllers use `@WsDTOMapping` for DTO conversion. See [occ-api-development.md](references/occ-api-development.md).

### Accelerators
Pre-built storefronts: B2C (retail) and B2B (enterprise with approval workflows). See [accelerator-customization.md](references/accelerator-customization.md).

## Common Workflows

### Create a Custom Extension
1. Generate structure with `ant extgen` or use template from `assets/extension-structure/`
2. Configure `extensioninfo.xml` with dependencies
3. Define types in `items.xml`
4. Configure beans in `*-spring.xml`
5. Add to `localextensions.xml`
6. Build: `ant clean all`

Reference: [extension-development.md](references/extension-development.md) | Template: `assets/extension-structure/`

### Define Custom Item Types
1. Create or modify `*-items.xml` in extension
2. Define itemtype with attributes, relations
3. Build to generate model classes
4. Use in services/DAOs

Reference: [type-system.md](references/type-system.md) | Templates: `assets/item-type-definition/`

### Implement Service Layer
1. Create DTO class for data transfer
2. Create DAO interface and implementation with FlexibleSearch
3. Create Service interface and implementation with business logic
4. Create Facade interface and implementation for external API
5. Configure beans in `*-spring.xml`

Reference: [service-layer-architecture.md](references/service-layer-architecture.md) | Templates: `assets/service-layer/`

### Import Data with ImpEx
1. Create `.impex` file with header and data rows
2. Use INSERT_UPDATE for create/modify operations
3. Define macros for reusable values
4. Import via HAC or `ant importImpex`

Reference: [impex-guide.md](references/impex-guide.md) | Templates: `assets/impex-scripts/`

### Query with FlexibleSearch
```sql
SELECT {pk} FROM {Product} WHERE {code} = ?code
SELECT {p.pk} FROM {Product AS p JOIN Category AS c ON {p.supercategories} = {c.pk}} WHERE {c.code} = ?category
```

Reference: [flexiblesearch-reference.md](references/flexiblesearch-reference.md) | Examples: `assets/flexiblesearch-queries/`

### Customize OCC API
1. Create controller with `@Controller` and `@RequestMapping`
2. Create WsDTO with `@WsDTOMapping`
3. Create populator for model-to-DTO conversion
4. Register in `*-web-spring.xml`

Reference: [occ-api-development.md](references/occ-api-development.md) | Templates: `assets/occ-customization/`

### Extend Accelerator Checkout
1. Create custom checkout step implementing `CheckoutStep`
2. Configure in checkout flow XML
3. Create JSP for step UI
4. Register beans in `*-spring.xml`

Reference: [accelerator-customization.md](references/accelerator-customization.md) | Templates: `assets/checkout-customization/`

## Quick Reference

| Task | Reference | Assets | Script |
|------|-----------|--------|--------|
| Extension setup | [extension-development.md](references/extension-development.md) | `assets/extension-structure/` | `scripts/generate-extension.sh` |
| Type definitions | [type-system.md](references/type-system.md) | `assets/item-type-definition/` | - |
| Service layer | [service-layer-architecture.md](references/service-layer-architecture.md) | `assets/service-layer/` | - |
| Data import | [impex-guide.md](references/impex-guide.md) | `assets/impex-scripts/` | `scripts/validate-impex.sh` |
| Queries | [flexiblesearch-reference.md](references/flexiblesearch-reference.md) | `assets/flexiblesearch-queries/` | `scripts/query-items.sh` |
| API customization | [occ-api-development.md](references/occ-api-development.md) | `assets/occ-customization/` | - |
| Checkout/Storefront | [accelerator-customization.md](references/accelerator-customization.md) | `assets/checkout-customization/` | - |
| Spring config | [spring-configuration.md](references/spring-configuration.md) | - | - |
| Data patterns | [data-modeling-patterns.md](references/data-modeling-patterns.md) | - | - |
| Troubleshooting | [troubleshooting-guide.md](references/troubleshooting-guide.md) | - | - |

## Resources

### scripts/
Utility scripts for common tasks:
- `generate-extension.sh` - Scaffold new extension structure
- `validate-impex.sh` - Validate ImpEx syntax before import
- `query-items.sh` - Execute FlexibleSearch queries via HAC

### references/
Detailed guides for each topic area. Load as needed for in-depth information.

### assets/
Production-quality code templates ready to copy and customize. Organized by domain (service-layer, impex-scripts, etc.).
