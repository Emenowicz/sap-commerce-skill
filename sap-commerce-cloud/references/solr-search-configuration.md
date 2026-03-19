# Solr Search Configuration

## Table of Contents
- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [SolrFacetSearchConfig Setup](#solrfacetsearchconfig-setup)
- [Indexed Properties](#indexed-properties)
- [Value Providers](#value-providers)
- [Free-Text Search](#free-text-search)
- [Boost Rules and Search Profiles](#boost-rules-and-search-profiles)
- [Indexer Strategies](#indexer-strategies)
- [ImpEx Configuration](#impex-configuration)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

## Overview

SAP Commerce uses Apache Solr for product search, faceted navigation, and category browsing. The search framework provides:
- Full-text search with relevance ranking
- Faceted filtering (category, price, brand, etc.)
- Sorting by multiple attributes
- Boost rules for merchandising
- Multi-language and multi-currency support

## Core Concepts

### Architecture

```
Product Data → Value Providers → Solr Index → Search Queries → Search Results
                                     ↑
                              Indexer CronJobs
```

### Key Models

| Model | Purpose |
|-------|---------|
| `SolrFacetSearchConfig` | Top-level configuration linking everything |
| `SolrIndexedType` | Maps a Commerce type (e.g., Product) to a Solr core |
| `SolrIndexedProperty` | Defines a searchable/sortable/facetable field |
| `SolrValueProvider` | Extracts field values from source models |
| `SolrSearchProfile` | Defines boost rules for search tuning |
| `SolrIndex` | Runtime index instance |

## SolrFacetSearchConfig Setup

The `SolrFacetSearchConfig` is the root configuration object that ties together:
- Which server to use
- Which types to index
- Which catalog versions to include
- Language and currency support

```impex
INSERT_UPDATE SolrFacetSearchConfig; name[unique=true]       ; indexNamePrefix; solrServerConfig(name); languages(isocode); currencies(isocode); catalogVersions(catalog(id),version)
                                   ; myStoreSearchConfig     ; mystore        ; Default              ; en,de             ; USD,EUR           ; myStoreProductCatalog:Online
```

## Indexed Properties

### Property Types

| Type | Use Case | Example |
|------|----------|---------|
| `string` | Exact match, facets | Brand name, category code |
| `text` | Full-text search | Product name, description |
| `int`, `long`, `float`, `double` | Numeric ranges | Price, weight |
| `date` | Date ranges | Created date |
| `boolean` | Binary filters | In stock |
| `sortabletext` | Text for sorting | Name (sortable) |

### Property Flags

| Flag | Effect |
|------|--------|
| `searchable` | Included in free-text search queries |
| `facet` | Appears as a filter facet |
| `sortable` | Can be used for result sorting |
| `localized` | Has per-language values |
| `currency` | Has per-currency values |
| `multiValue` | Stores multiple values per document |
| `ranged` | Displays as a range facet (e.g., price ranges) |

### ImpEx Property Definition

```impex
$searchConfig = myStoreSearchConfig
$indexedType = myStoreProductType

INSERT_UPDATE SolrIndexedProperty; solrIndexedType(identifier)[unique=true]; name[unique=true]; type(code); sortableType(code); localized[default=false]; currency[default=false]; multiValue[default=false]; facet[default=false]; facetType(code); facetSort(code); priority; fieldValueProvider; facetDisplayNameProvider
# Searchable text fields
                                 ; $indexedType ; name              ; text       ;                   ; true  ;       ;       ;       ;           ;       ; 100 ; springELValueProvider ;
                                 ; $indexedType ; description       ; text       ;                   ; true  ;       ;       ;       ;           ;       ; 50  ; springELValueProvider ;
                                 ; $indexedType ; code              ; string     ;                   ;       ;       ;       ;       ;           ;       ; 90  ;                      ;
# Facet fields
                                 ; $indexedType ; category          ; string     ;                   ;       ;       ; true  ; true  ; Refine    ; Alpha ; 80  ; categoryCodeValueProvider ; categoryFacetDisplayNameProvider
                                 ; $indexedType ; brand             ; string     ;                   ;       ;       ;       ; true  ; Refine    ; Alpha ; 70  ; springELValueProvider ;
                                 ; $indexedType ; price             ; double     ;                   ;       ; true  ;       ; true  ; MultiSelectOr ; Alpha ; 60 ; productPriceValueProvider ;
                                 ; $indexedType ; inStockFlag       ; boolean    ;                   ;       ;       ;       ; true  ; Refine    ;       ; 50  ; productInStockFlagValueProvider ;
# Sortable fields
                                 ; $indexedType ; name-sort         ; sortabletext ; text            ; true  ;       ;       ;       ;           ;       ; 40  ; springELValueProvider ;
                                 ; $indexedType ; price-sort        ; double     ;                   ;       ; true  ;       ;       ;           ;       ; 40  ; productPriceValueProvider ;
```

## Value Providers

Value providers extract data from models and convert it to Solr-indexable values.

### Built-in Value Providers

| Provider | Purpose |
|----------|---------|
| `springELValueProvider` | Uses Spring Expression Language (SpEL) |
| `productPriceValueProvider` | Extracts prices per currency |
| `categoryCodeValueProvider` | Extracts category codes (hierarchical) |
| `productInStockFlagValueProvider` | Checks stock availability |
| `productUrlValueProvider` | Generates product URL |
| `productImageValueProvider` | Extracts image URLs |
| `modelPropertyFieldValueProvider` | Reads a model property by name |

### SpEL Value Provider Configuration

```impex
INSERT_UPDATE SolrIndexedProperty; solrIndexedType(identifier)[unique=true]; name[unique=true]; fieldValueProvider       ; valueProviderParameter
                                 ; $indexedType ; name         ; springELValueProvider   ; getName()
                                 ; $indexedType ; description  ; springELValueProvider   ; getDescription()
                                 ; $indexedType ; manufacturer ; springELValueProvider   ; getManufacturerName()
```

### Custom Value Provider

Implement `FieldValueProvider` for custom logic:

```java
package com.example.search.providers;

import de.hybris.platform.solrfacetsearch.config.IndexConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedProperty;
import de.hybris.platform.solrfacetsearch.config.exceptions.FieldValueProviderException;
import de.hybris.platform.solrfacetsearch.provider.FieldValue;
import de.hybris.platform.solrfacetsearch.provider.FieldValueProvider;
import de.hybris.platform.solrfacetsearch.provider.impl.AbstractPropertyFieldValueProvider;

import de.hybris.platform.core.model.product.ProductModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomValueProvider extends AbstractPropertyFieldValueProvider implements FieldValueProvider {

    @Override
    public Collection<FieldValue> getFieldValues(
            final IndexConfig indexConfig,
            final IndexedProperty indexedProperty,
            final Object model) throws FieldValueProviderException {

        if (!(model instanceof ProductModel)) {
            throw new FieldValueProviderException("Model is not a ProductModel");
        }

        final ProductModel product = (ProductModel) model;
        final List<FieldValue> fieldValues = new ArrayList<>();

        // Custom logic to compute the field value
        final String computedValue = computeValue(product);

        if (computedValue != null) {
            addFieldValues(fieldValues, indexedProperty, computedValue);
        }

        return fieldValues;
    }

    private String computeValue(final ProductModel product) {
        // TODO: Implement custom value computation
        return product.getCode();
    }
}
```

**Spring registration**:
```xml
<bean id="customValueProvider" class="com.example.search.providers.CustomValueProvider"
      parent="abstractPropertyFieldValueProvider"/>
```

**ImpEx reference**:
```impex
INSERT_UPDATE SolrIndexedProperty; solrIndexedType(identifier)[unique=true]; name[unique=true]; fieldValueProvider
                                 ; $indexedType ; customField ; customValueProvider
```

## Free-Text Search

### Configuration

Free-text search uses `searchable=true` indexed properties. Configure query behavior:

```impex
# Free-text search configuration
INSERT_UPDATE SolrSearchQueryProperty; indexedProperty(name, solrIndexedType(identifier))[unique=true]; searchQueryTemplate(name, indexedType(identifier))[unique=true]; facet; ftsPhraseQuery; ftsPhraseQueryBoost; ftsQuery; ftsQueryBoost; ftsFuzzyQuery; ftsFuzzyQueryBoost; ftsWildcardQuery; ftsWildcardQueryType(code); includeInResponse
                                     ; name:$indexedType    ; DEFAULT:$indexedType ; ; true ; 100 ; true ; 50 ; true ; 25 ;      ;         ; true
                                     ; code:$indexedType    ; DEFAULT:$indexedType ; ; true ; 90  ; true ; 60 ;      ;    ; true ; POSTFIX ; true
                                     ; description:$indexedType ; DEFAULT:$indexedType ; ; ;  ; true ; 20 ; true ; 10 ;      ;     ; true
```

### Query Boost Hierarchy

```
Exact Match (phrase) → Standard Match → Fuzzy Match → Wildcard Match
   (highest boost)                                     (lowest boost)
```

## Boost Rules and Search Profiles

### Boost Rules

Promote or demote products in search results:

```impex
INSERT_UPDATE SolrBoostRule; solrIndexedProperty(name, solrIndexedType(identifier))[unique=true]; searchProfile(code)[unique=true]; boostType(code); boost; operator(code); propertyValue
                           ; brand:$indexedType         ; default ; MULTIPLICATIVE ; 2.0 ; EQUAL         ; PremiumBrand
                           ; inStockFlag:$indexedType   ; default ; MULTIPLICATIVE ; 1.5 ; EQUAL         ; true
```

### Search Profiles

Group boost rules into profiles for different contexts:

```impex
INSERT_UPDATE SolrSearchProfile; code[unique=true]; indexedType(identifier); categoryCode
                               ; default          ; $indexedType           ;
                               ; electronics      ; $indexedType           ; electronics
```

## Indexer Strategies

### Index Operation Types

| Operation | Use Case |
|-----------|----------|
| `FULL` | Rebuild entire index (during deployment or major changes) |
| `UPDATE` | Index only modified items since last run |
| `DELETE` | Remove items from index |

### CronJob-Based Indexing

```impex
# Full indexer CronJob
INSERT_UPDATE SolrIndexerCronJob; code[unique=true]              ; job(code)           ; facetSearchConfig(name); indexerOperation(code); sessionLanguage(isocode)
                                ; myStoreFullIndexerCronJob      ; solrIndexerJob       ; myStoreSearchConfig    ; full                 ; en

# Update indexer CronJob (incremental)
INSERT_UPDATE SolrIndexerCronJob; code[unique=true]              ; job(code)           ; facetSearchConfig(name); indexerOperation(code); sessionLanguage(isocode)
                                ; myStoreUpdateIndexerCronJob    ; solrIndexerJob       ; myStoreSearchConfig    ; update               ; en

# Schedule: full index nightly, update every 5 minutes
INSERT_UPDATE Trigger; cronJob(code)[unique=true]      ; cronExpression     ; active
                     ; myStoreFullIndexerCronJob        ; 0 0 3 * * ?        ; true
                     ; myStoreUpdateIndexerCronJob      ; 0 0/5 * * * ?      ; true
```

## ImpEx Configuration

### Complete Solr Setup

```impex
$productCatalog = myStoreProductCatalog
$catalogVersion = Online
$searchConfig = myStoreSearchConfig
$indexedType = myStoreProductType

# Server configuration
INSERT_UPDATE SolrServerConfig; name[unique=true]; mode(code); embeddedMaster
                              ; Default          ; embedded  ; true

# Endpoint configuration
INSERT_UPDATE SolrEndpointUrl; solrServerConfig(name)[unique=true]; url[unique=true]           ; master
                             ; Default                            ; http://localhost:8983/solr  ; true

# Search config
INSERT_UPDATE SolrFacetSearchConfig; name[unique=true]; indexNamePrefix; solrServerConfig(name); languages(isocode); currencies(isocode); catalogVersions(catalog(id),version)
                                   ; $searchConfig    ; mystoreindex   ; Default              ; en,de             ; USD,EUR           ; $productCatalog:$catalogVersion

# Indexed type
INSERT_UPDATE SolrIndexedType; identifier[unique=true]; type(code); solrFacetSearchConfig(name); defaultFieldValueProvider; variant
                             ; $indexedType           ; Product   ; $searchConfig              ; modelPropertyFieldValueProvider ; false

# Indexed properties
INSERT_UPDATE SolrIndexedProperty; solrIndexedType(identifier)[unique=true]; name[unique=true]; type(code); localized[default=false]; currency[default=false]; multiValue[default=false]; facet[default=false]; facetType(code); priority; fieldValueProvider; useForSpellchecking[default=false]; useForAutocomplete[default=false]
                                 ; $indexedType ; code       ; string ;       ;       ;       ;       ;        ; 90  ; springELValueProvider ;      ;
                                 ; $indexedType ; name       ; text   ; true  ;       ;       ;       ;        ; 100 ; springELValueProvider ; true ; true
                                 ; $indexedType ; price      ; double ;       ; true  ;       ; true  ; MultiSelectOr ; 60 ; productPriceValueProvider ; ;
                                 ; $indexedType ; category   ; string ;       ;       ; true  ; true  ; Refine ; 80  ; categoryCodeValueProvider ; ;
                                 ; $indexedType ; inStock    ; boolean;       ;       ;       ; true  ; Refine ; 50  ; productInStockFlagValueProvider ; ;
```

## Performance Tuning

### Indexing Performance

| Setting | Property | Default | Recommendation |
|---------|----------|---------|----------------|
| Batch size | `solr.indexer.batch.size` | 100 | 200-500 for large catalogs |
| Commit mode | `solr.indexer.commit.mode` | After batch | Use soft commit for speed |
| Thread count | `solr.indexer.thread.count` | 1 | 2-4 based on available cores |
| Query cache | `solr.query.cache.size` | 512 | Increase for high-traffic sites |

### local.properties Tuning

```properties
# Indexer performance
solr.indexer.batch.size=300
solr.indexer.thread.count=2

# Soft commit for faster near-real-time search
solr.indexer.commit.mode=AFTER_INDEX
solr.indexer.soft.commit=true

# Query performance
solr.query.cache.size=1024
solr.filter.cache.size=512
```

### Common Performance Issues

1. **Slow full indexing**: Increase batch size, add indexer threads, check value provider performance
2. **Stale search results**: Ensure update indexer CronJob runs frequently, check soft commit settings
3. **High memory on Solr**: Reduce cache sizes, optimize stored fields, review facet cardinality

## Troubleshooting

### Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `SolrServerException: Connection refused` | Solr not running or wrong URL | Verify Solr server config and endpoint URL |
| `FieldValueProviderException` | Value provider fails for a product | Check null handling in custom providers |
| `Index not found` | Index doesn't exist yet | Run a full index first |
| Empty search results | Wrong catalog version in config | Verify `catalogVersions` in SolrFacetSearchConfig |
| Facets not showing | Property not marked as `facet=true` | Update indexed property configuration |

### HAC Solr Administration

Navigate to **Platform > Solr** in HAC for:
- Viewing index status
- Running manual full/update index
- Testing search queries
- Viewing indexed documents
