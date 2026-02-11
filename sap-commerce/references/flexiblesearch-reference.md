# FlexibleSearch Reference

## Table of Contents
- [Query Syntax](#query-syntax)
- [Type References](#type-references)
- [Joins](#joins)
- [Subqueries](#subqueries)
- [Localized Attributes](#localized-attributes)
- [Ordering and Pagination](#ordering-and-pagination)
- [Query Parameters](#query-parameters)
- [Performance Considerations](#performance-considerations)
- [Common Query Patterns](#common-query-patterns)
- [Integration with DAOs](#integration-with-daos)

## Query Syntax

### Basic Structure
```sql
SELECT {fields} FROM {Type} WHERE {conditions} ORDER BY {field} ASC|DESC
```

### Simple Query
```sql
SELECT {pk} FROM {Product} WHERE {code} = 'PROD001'
```

### Multiple Fields
```sql
SELECT {pk}, {code}, {name} FROM {Product}
```

### All Fields (avoid in production)
```sql
SELECT * FROM {Product}
```

## Type References

### Curly Brace Syntax
- `{TypeName}`: Reference type
- `{attribute}`: Reference attribute
- `{pk}`: Primary key
- `{p.attribute}`: Aliased attribute

### Type Alias
```sql
SELECT {p.pk} FROM {Product AS p} WHERE {p.code} = ?code
```

### Subtypes
```sql
SELECT {pk} FROM {Product!} WHERE {code} LIKE 'VAR%'
-- ! excludes subtypes (VariantProduct)
```

## Joins

### Implicit Join (via relation)
```sql
SELECT {p.pk} FROM {Product AS p}
WHERE {p.supercategories} IN (
    SELECT {pk} FROM {Category} WHERE {code} = 'electronics'
)
```

### Explicit JOIN
```sql
SELECT {p.pk}, {c.code}
FROM {Product AS p
      JOIN Category AS c ON {p.supercategories} = {c.pk}}
WHERE {c.code} = ?categoryCode
```

### LEFT JOIN
```sql
SELECT {p.pk}, {s.available}
FROM {Product AS p
      LEFT JOIN StockLevel AS s ON {p.pk} = {s.product}}
WHERE {p.code} = ?code
```

### Multiple JOINs
```sql
SELECT {p.pk}
FROM {Product AS p
      JOIN CatalogVersion AS cv ON {p.catalogVersion} = {cv.pk}
      JOIN Catalog AS c ON {cv.catalog} = {c.pk}}
WHERE {c.id} = ?catalogId AND {cv.version} = ?version
```

## Subqueries

### IN Subquery
```sql
SELECT {pk} FROM {Product}
WHERE {pk} IN (
    SELECT {product} FROM {StockLevel} WHERE {available} > 0
)
```

### EXISTS Subquery
```sql
SELECT {p.pk} FROM {Product AS p}
WHERE EXISTS (
    SELECT 1 FROM {PriceRow AS pr} WHERE {pr.product} = {p.pk}
)
```

### NOT EXISTS
```sql
SELECT {p.pk} FROM {Product AS p}
WHERE NOT EXISTS (
    SELECT 1 FROM {OrderEntry AS oe} WHERE {oe.product} = {p.pk}
)
```

## Localized Attributes

### Current Session Language
```sql
SELECT {pk}, {name} FROM {Product}
-- Returns name in session language
```

### Specific Language
```sql
SELECT {pk}, {name[en]}, {name[de]} FROM {Product}
```

### Localized in WHERE
```sql
SELECT {pk} FROM {Product} WHERE {name[en]} LIKE '%shirt%'
```

## Ordering and Pagination

### ORDER BY
```sql
SELECT {pk} FROM {Product}
ORDER BY {name} ASC, {code} DESC
```

### Pagination via API
```java
FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
query.setStart(20);   // Skip first 20 results (offset)
query.setCount(10);   // Return 10 results (limit)
```

### LIMIT (non-standard)
```sql
SELECT {pk} FROM {Product} ORDER BY {creationtime} DESC
-- Use setCount() instead of SQL LIMIT
```

## Query Parameters

### Named Parameters
```java
String query = "SELECT {pk} FROM {Product} WHERE {code} = ?code";
FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(query);
fsQuery.addQueryParameter("code", productCode);
```

### Multiple Parameters
```java
String query = "SELECT {pk} FROM {Product} WHERE {code} = ?code AND {name} LIKE ?name";
fsQuery.addQueryParameter("code", code);
fsQuery.addQueryParameter("name", "%" + name + "%");
```

### Collection Parameters
```java
String query = "SELECT {pk} FROM {Product} WHERE {code} IN (?codes)";
fsQuery.addQueryParameter("codes", Arrays.asList("P001", "P002", "P003"));
```

### Map Parameters
```java
Map<String, Object> params = new HashMap<>();
params.put("code", productCode);
params.put("status", "ACTIVE");
fsQuery.addQueryParameters(params);
```

## Performance Considerations

### Use {pk} in SELECT
```sql
-- Good: Only fetch primary keys
SELECT {pk} FROM {Product} WHERE {code} LIKE ?pattern

-- Avoid: Fetches all columns
SELECT * FROM {Product} WHERE {code} LIKE ?pattern
```

### Index Usage
Ensure indexed attributes are used in WHERE:
```xml
<attribute qualifier="code" type="java.lang.String">
    <modifiers search="true"/>  <!-- Creates index -->
</attribute>
```

### Avoid N+1 Queries
```sql
-- Bad: Lazy loading categories in loop
SELECT {pk} FROM {Product}

-- Good: JOIN to fetch related data
SELECT {p.pk}, {c.code}
FROM {Product AS p JOIN Category AS c ON {p.supercategories} = {c.pk}}
```

### Query Caching
```java
fsQuery.setCacheable(true);  // Enable query cache
fsQuery.setResultClassList(Collections.singletonList(ProductModel.class));
```

### Limit Result Sets
```java
fsQuery.setCount(100);  // Always limit for large tables
```

## Common Query Patterns

### Products by Category
```sql
SELECT {p.pk} FROM {Product AS p
      JOIN CategoryProductRelation AS cpr ON {p.pk} = {cpr.target}
      JOIN Category AS c ON {cpr.source} = {c.pk}}
WHERE {c.code} = ?categoryCode
```

### Products by Price Range
```sql
SELECT DISTINCT {p.pk} FROM {Product AS p
      JOIN PriceRow AS pr ON {p.pk} = {pr.product}}
WHERE {pr.price} BETWEEN ?minPrice AND ?maxPrice
      AND {pr.currency} = ?currency
```

### Orders by User
```sql
SELECT {pk} FROM {Order}
WHERE {user} = ?user
ORDER BY {creationtime} DESC
```

### Cart Items
```sql
SELECT {ce.pk} FROM {CartEntry AS ce
      JOIN Cart AS c ON {ce.order} = {c.pk}}
WHERE {c.user} = ?user AND {c.code} = ?cartCode
```

### Low Stock Products
```sql
SELECT {p.pk} FROM {Product AS p
      JOIN StockLevel AS sl ON {p.pk} = {sl.product}}
WHERE {sl.available} < ?threshold AND {sl.available} > 0
```

### Recent Orders
```sql
SELECT {pk} FROM {Order}
WHERE {creationtime} >= ?startDate
ORDER BY {creationtime} DESC
```

### Search by Multiple Criteria
```sql
SELECT {pk} FROM {Product}
WHERE ({code} LIKE ?search OR LOWER({name}) LIKE LOWER(?search))
      AND {approvalStatus} = ?status
      AND {catalogVersion} = ?catalogVersion
```

## Integration with DAOs

### FlexibleSearchService Usage
```java
@Resource
private FlexibleSearchService flexibleSearchService;

public List<ProductModel> findProducts(String query) {
    String fsQuery = "SELECT {pk} FROM {Product} WHERE {name} LIKE ?query";

    FlexibleSearchQuery searchQuery = new FlexibleSearchQuery(fsQuery);
    searchQuery.addQueryParameter("query", "%" + query + "%");
    searchQuery.setResultClassList(Collections.singletonList(ProductModel.class));

    SearchResult<ProductModel> result = flexibleSearchService.search(searchQuery);
    return result.getResult();
}
```

### SearchResult Methods
```java
SearchResult<ProductModel> result = flexibleSearchService.search(query);

List<ProductModel> items = result.getResult();      // Results
int total = result.getTotalCount();                  // Total matching (may require count query)
int returned = result.getCount();                    // Returned count
```

### Count Query
```java
String countQuery = "SELECT COUNT({pk}) FROM {Product} WHERE {code} LIKE ?pattern";
FlexibleSearchQuery query = new FlexibleSearchQuery(countQuery);
query.addQueryParameter("pattern", "%test%");
query.setResultClassList(Collections.singletonList(Integer.class));

SearchResult<Integer> result = flexibleSearchService.search(query);
int count = result.getResult().get(0);
```
