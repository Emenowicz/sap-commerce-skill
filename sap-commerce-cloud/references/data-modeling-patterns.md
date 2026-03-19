# Data Modeling Patterns

## Table of Contents
- [Product Modeling](#product-modeling)
- [Category Hierarchies](#category-hierarchies)
- [User Structures B2C](#user-structures-b2c)
- [User Structures B2B](#user-structures-b2b)
- [Order Management](#order-management)
- [Pricing and Discounts](#pricing-and-discounts)
- [Content Management](#content-management)
- [Multi-Site and Multi-Catalog](#multi-site-and-multi-catalog)
- [Localization and Currency](#localization-and-currency)
- [ERP Integration](#erp-integration)
- [Search and Solr Indexing](#search-and-solr-indexing)

## Product Modeling

### Base Product Structure
`Product` is the core item type with attributes: code, name, description, catalogVersion.

### Variants
Use `VariantProduct` extending Product for size/color variations:
```
Product (base)
  └── VariantProduct (extends Product)
        └── ApparelSizeVariantProduct
        └── ApparelStyleVariantProduct
```

### Classification System
Use `ClassificationClass` and `ClassificationAttribute` for dynamic product features:
- ClassificationClass groups attributes (e.g., "Technical Specs")
- ClassificationAttribute defines feature (e.g., "Screen Size")
- ClassAttributeAssignment links attributes to products

### Product Features
Store structured data via `ProductFeature` for searchable attributes.

## Category Hierarchies

### Category Structure
`Category` uses `supercategories` relation for hierarchy:
```sql
SELECT {c.pk} FROM {Category AS c} WHERE {c.supercategories} IS NULL  -- Root categories
```

### Navigation Nodes
`CMSNavigationNode` creates storefront navigation separate from catalog categories. Link via `CMSNavigationEntry`.

### Catalog-Aware Categories
Categories belong to `CatalogVersion`. Use staged/online workflow for content approval.

## User Structures B2C

### Customer Model
`Customer` extends `User` with commerce attributes:
- contactEmail, defaultPaymentAddress, defaultShipmentAddress
- CustomerGroup for segmentation

### User Groups
`CustomerGroup` for loyalty tiers, permissions:
```impex
INSERT_UPDATE CustomerGroup;uid[unique=true];locname[lang=en]
;goldCustomers;Gold Members
```

## User Structures B2B

### B2B Organization
```
B2BUnit (company/division)
  └── B2BCustomer (employee)
  └── B2BCostCenter (budget allocation)
  └── B2BBudget (spending limits)
```

### Key B2B Types
- **B2BUnit**: Organization hierarchy (parent/children units)
- **B2BCustomer**: Extends Customer with unit, roles, approvers
- **B2BCostCenter**: Links orders to budgets
- **B2BBudget**: Spending limits with date ranges
- **B2BPermission**: Order approval thresholds

### Approval Workflow
Configure via `B2BApprovalProcess`, `B2BPermissionResult` for order approvals.

## Order Management

### Order Structure
```
Order
  └── OrderEntry (line items)
        └── Product reference
        └── quantity, basePrice, totalPrice
```

### Fulfillment
```
Consignment (shipment)
  └── ConsignmentEntry
        └── OrderEntry reference
        └── quantity shipped
```

### Order Lifecycle
AbstractOrder → Cart → Order → Consignment → Delivery

### Key Attributes
- Order: code, date, status, totalPrice, currency, deliveryAddress
- OrderEntry: entryNumber, quantity, basePrice, product

## Pricing and Discounts

### Price Rows
`PriceRow` defines prices per product/user/currency:
```impex
INSERT_UPDATE PriceRow;product(code)[unique=true];price;currency(isocode);unit(code)
;PROD001;99.99;USD;pieces
```

### Discount Rules
`DiscountRow` for percentage or absolute discounts. Link to UserGroup for targeted pricing.

### Promotions
Use `PromotionGroup`, `ProductPromotion`, `OrderPromotion` for:
- Buy X get Y free
- Percentage off orders over threshold
- Bundle discounts

## Content Management

### CMS Structure
```
ContentPage
  └── ContentSlot (position on page)
        └── AbstractCMSComponent (content)
```

### Key CMS Types
- **ContentPage**: URL, template, restrictions
- **ContentSlot**: Named position (header, footer, sidebar)
- **CMSParagraphComponent**: Rich text content
- **BannerComponent**: Image with link
- **ProductCarouselComponent**: Product listings

### Page Templates
`PageTemplate` defines available slots; `ContentSlotForTemplate` assigns defaults.

## Multi-Site and Multi-Catalog

### Site Structure
`CMSSite` represents a storefront with:
- defaultCatalog, contentCatalogs
- urlPatterns for routing
- defaultLanguage, defaultCurrency

### Catalog Versioning
```
Catalog
  └── CatalogVersion (Staged)  -- Work in progress
  └── CatalogVersion (Online)  -- Published
```

### Synchronization
Use `SyncJob` to push Staged → Online content.

## Localization and Currency

### Localized Attributes
Define in items.xml:
```xml
<attribute qualifier="name" type="localized:java.lang.String"/>
```

### Languages and Currencies
```impex
INSERT_UPDATE Language;isocode[unique=true];name[lang=en]
;en;English
;de;German

INSERT_UPDATE Currency;isocode[unique=true];symbol;conversion
;USD;$;1.0
;EUR;€;0.85
```

### Session Context
Set language/currency via `CommonI18NService.setCurrentLanguage()`.

## ERP Integration

### Integration Objects
Define via `IntegrationObject` for API exposure:
- IntegrationObjectItem maps to item types
- IntegrationObjectItemAttribute maps attributes

### DataHub Patterns (Legacy)
- Raw Item → Canonical Item → Target Item
- Composition for complex objects

### Inbound/Outbound
- **Inbound**: External system → SAP Commerce (ImpEx, Integration API)
- **Outbound**: SAP Commerce → External (Webhooks, Integration API)

### Best Practices
- Use idempotent operations (INSERT_UPDATE)
- Batch large imports
- Handle delta updates via modification timestamps

## Search and Solr Indexing

### Index Configuration
`SolrIndexedType` defines what to index:
```
SolrIndexedType
  └── SolrIndexedProperty (fields)
  └── SolrFacetSearchConfig (search settings)
```

### Indexed Properties
`IndexedProperty` configures searchable/sortable/facet fields:
- name, type, sortable, facet, localized

### Indexer Strategy
`IndexerStrategy` controls full vs. incremental indexing:
- FullIndexerStrategy: Complete rebuild
- UpdateIndexerStrategy: Delta updates

### Facets
Configure `SolrSearchQueryProperty` for:
- Category facets
- Price range facets
- Attribute facets (color, size)
