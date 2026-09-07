# Service Layer Architecture

## Table of Contents
- [Architecture Layers Overview](#architecture-layers-overview)
- [Facade Pattern Implementation](#facade-pattern-implementation)
- [Service Layer Responsibilities](#service-layer-responsibilities)
- [DAO Implementation Patterns](#dao-implementation-patterns)
- [DTO Conversion Strategies](#dto-conversion-strategies)
- [Spring Dependency Injection](#spring-dependency-injection)
- [Transaction Management](#transaction-management)
- [Error Handling Patterns](#error-handling-patterns)

## Architecture Layers Overview

SAP Commerce implements a strict four-layer architecture:

```
Presentation Layer (Controllers, JSPs, REST Endpoints)
        ↓
Facade Layer (DTOs, Converters, External API)
        ↓
Service Layer (Business Logic, Validation, Transactions)
        ↓
DAO Layer (FlexibleSearch, Persistence)
        ↓
Model Layer (Generated from items.xml)
```

**Key Principles:**
- Each layer communicates only with adjacent layers
- Facades expose DTOs to presentation; never expose Models directly
- Services contain business logic and coordinate DAO operations
- DAOs handle all database interactions via FlexibleSearch

## Facade Pattern Implementation

Facades act as the API boundary between presentation and business layers.

**Interface:**
```java
public interface ProductFacade {
    ProductData getProductForCode(String code);
    List<ProductData> searchProducts(String query, int pageSize);
}
```

**Implementation:**
```java
public class DefaultProductFacade implements ProductFacade {
    private ProductService productService;
    private Converter<ProductModel, ProductData> productConverter;

    @Override
    public ProductData getProductForCode(String code) {
        ProductModel model = productService.getProductForCode(code);
        return productConverter.convert(model);
    }
}
```

**Guidelines:**
- Accept and return DTOs only, never Models
- Inject services and converters via Spring
- Handle null checks and convert exceptions
- Keep methods thin; delegate business logic to services

## Service Layer Responsibilities

Services encapsulate business logic, validation, and transaction boundaries.

**Interface:**
```java
public interface ProductService {
    ProductModel getProductForCode(String code);
    void updateProductStock(String code, String warehouseCode, int quantity);
}
```

**Responsibilities:**
1. Business Logic: Calculations, workflows, domain rules
2. Validation: Input validation before persistence
3. Transaction Coordination: Mark methods as transactional
4. Model Operations: Create, update, delete via ModelService
5. Cross-Domain Orchestration: Coordinate multiple DAOs

**Best Practices:**
- Inject `ModelService` for save/remove operations
- Use `@Transactional` annotation
- Throw service-level exceptions with meaningful messages
- Delegate data access to DAOs

## DAO Implementation Patterns

DAOs encapsulate all data access logic using FlexibleSearch.

**Interface:**
```java
public interface ProductDAO {
    ProductModel findByCodeAndCatalogVersion(String code, CatalogVersionModel catalogVersion);
    List<ProductModel> findByCategory(CategoryModel category);
}
```

**Implementation:**
```java
public class DefaultProductDAO implements ProductDAO {
    private FlexibleSearchService flexibleSearchService;

    @Override
    public ProductModel findByCodeAndCatalogVersion(
            String code, CatalogVersionModel catalogVersion) {
        String query = "SELECT {pk} FROM {Product} "
                + "WHERE {code} = ?code AND {catalogVersion} = ?catalogVersion";
        FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(query);
        fsQuery.addQueryParameter("code", code);
        fsQuery.addQueryParameter("catalogVersion", catalogVersion);
        SearchResult<ProductModel> result = flexibleSearchService.search(fsQuery);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }
}
```

**Query Patterns:**
- Use parameterized queries to prevent injection
- Prefer `{pk}` in SELECT for efficiency
- Use JOINs for relation traversal
- Configure result classes for type safety

## DTO Conversion Strategies

SAP Commerce uses the Converter/Populator pattern.

**Populator:** Fills specific fields in the target DTO
```java
public class ProductBasicPopulator implements Populator<ProductModel, ProductData> {
    @Override
    public void populate(ProductModel source, ProductData target) {
        target.setCode(source.getCode());
        target.setName(source.getName());
    }
}
```

**Converter Configuration:**
```xml
<bean id="productConverter" parent="abstractPopulatingConverter">
    <property name="targetClass" value="com.example.data.ProductData"/>
    <property name="populators">
        <list>
            <ref bean="productBasicPopulator"/>
            <ref bean="productPricePopulator"/>
        </list>
    </property>
</bean>
```

**Benefits:**
- Compose populators for different use cases
- Reuse populators across converters
- Test populators independently

## Spring Dependency Injection

Configure all components in `*-spring.xml` files.

```xml
<!-- DAO Layer -->
<alias name="defaultCustomProductDAO" alias="customProductDAO"/>
<bean id="defaultCustomProductDAO" class="com.example.dao.impl.DefaultProductDAO">
    <property name="flexibleSearchService" ref="flexibleSearchService"/>
</bean>

<!-- Service Layer -->
<alias name="defaultCustomProductService" alias="customProductService"/>
<bean id="defaultCustomProductService" class="com.example.service.impl.DefaultProductService">
    <property name="productDAO" ref="customProductDAO"/>
    <property name="platformProductService" ref="productService"/>
    <property name="modelService" ref="modelService"/>
</bean>

<!-- Facade Layer -->
<alias name="defaultCustomProductFacade" alias="customProductFacade"/>
<bean id="defaultCustomProductFacade" class="com.example.facade.impl.DefaultProductFacade">
    <property name="productService" ref="customProductService"/>
    <property name="productConverter" ref="customProductConverter"/>
</bean>
```

**Alias Pattern:** Define `alias` pointing to `default*` bean for easy overriding.

## Transaction Management

> **WARNING:** Never use `@Transactional` on DAO methods. Transactions must be managed at the Service layer only. Placing transaction boundaries on DAOs breaks the ability for services to coordinate multiple DAO operations atomically and can lead to partial commits.

Use declarative transactions via Spring annotations.

**Annotation-Based:**
```java
@Transactional
public void updateProductStock(String code, String warehouseCode, int quantity) {
    ProductModel product = platformProductService.getProductForCode(code);
    WarehouseModel warehouse = warehouseService.getWarehouseForCode(warehouseCode);
    StockLevelModel stockLevel = stockService.getStockLevel(product, warehouse);
    if (stockLevel == null) {
        StockLevelModel newStockLevel = modelService.create(StockLevelModel.class);
        newStockLevel.setProductCode(product.getCode());
        newStockLevel.setWarehouse(warehouse);
        newStockLevel.setAvailable(quantity);
        modelService.save(newStockLevel);
    } else {
        stockService.updateActualStockLevel(product, warehouse, quantity, "Product stock update");
    }
}
```

**Programmatic:**
```java
transactionTemplate.execute(status -> {
    products.forEach(modelService::save);
    return null;
});
```

**Guidelines:**
- Place `@Transactional` on service methods
- Use `readOnly=true` for query-only operations
- Configure rollback rules for specific exceptions
- Avoid long-running transactions

## Error Handling Patterns

Implement consistent exception handling across layers.

**Exception Hierarchy:**
```
ModelNotFoundException     (DAO layer)
    → ServiceException     (Service layer)
        → FacadeException  (Facade layer)
```

**DAO Layer:**
```java
if (result.getResult().isEmpty()) {
    throw new ModelNotFoundException("Product not found: " + code);
}
```

**Facade Layer:**
```java
try {
    ProductModel model = productService.getProductForCode(code);
    return productConverter.convert(model);
} catch (ModelNotFoundException e) {
    throw new UnknownIdentifierException("Product not found: " + code, e);
}
```

**Best Practices:**
- Create custom exceptions extending platform base exceptions
- Log exceptions at the layer where they occur
- Transform exceptions at layer boundaries
- Include context information in exception messages
