# Caching Guide

## Table of Contents
- [Overview](#overview)
- [Platform Cache Regions](#platform-cache-regions)
- [Region Cache Configuration](#region-cache-configuration)
- [Spring Cache Integration](#spring-cache-integration)
- [Cache Invalidation](#cache-invalidation)
- [Cluster-Aware Caching](#cluster-aware-caching)
- [Monitoring](#monitoring)
- [Common Pitfalls](#common-pitfalls)

## Overview

SAP Commerce provides a multi-layered caching system to improve performance:

- **Platform caches**: Built-in caches for type system, entity, and query results
- **Spring caches**: Application-level caching via `@Cacheable`/`@CacheEvict`
- **HTTP caches**: Response-level caching for OCC APIs

Proper cache configuration can reduce database queries by 80-90% on read-heavy operations.

## Platform Cache Regions

### Built-in Regions

| Region | Purpose | Caches |
|--------|---------|--------|
| `typesystemCacheRegion` | Type system metadata | Item types, attribute descriptors |
| `entityCacheRegion` | Model instances | Items loaded by PK |
| `queryCacheRegion` | FlexibleSearch results | Query result sets |
| `localizedEntityCacheRegion` | Localized attributes | Per-language item data |
| `modelBeanCacheRegion` | Service layer model cache | ModelService internal cache |

### Cache Hierarchy

```
Request → Model Service → Entity Cache → Database
                       → Query Cache → Database
```

When an item is requested:
1. Check entity cache by PK → return if cached
2. Check query cache by query hash → return if cached
3. Execute database query, populate caches

## Region Cache Configuration

### local.properties Settings

```properties
# Entity cache (item instances)
cache.region.entity.size=50000
cache.region.entity.evictionpolicy=LRU
cache.region.entity.statsEnabled=true

# Query cache (FlexibleSearch results)
cache.region.query.size=10000
cache.region.query.evictionpolicy=LRU
cache.region.query.statsEnabled=true

# Type system cache (metadata)
cache.region.typesystem.size=10000
cache.region.typesystem.evictionpolicy=LRU

# Localized entity cache
cache.region.localizedentity.size=30000
cache.region.localizedentity.evictionpolicy=LRU
```

### Sizing Guidelines

| Catalog Size | Entity Cache | Query Cache | Notes |
|-------------|-------------|-------------|-------|
| Small (<10K products) | 20,000 | 5,000 | Default settings usually sufficient |
| Medium (10K-100K) | 50,000-100,000 | 10,000-20,000 | Monitor hit ratios |
| Large (100K+) | 200,000+ | 50,000+ | May need cache tiering |

### EHCache Configuration

For advanced configuration, customize `ehcache.xml`:

```xml
<cache name="entityCacheRegion"
       maxEntriesLocalHeap="100000"
       eternal="false"
       timeToLiveSeconds="3600"
       timeToIdleSeconds="1800"
       memoryStoreEvictionPolicy="LRU">
</cache>

<cache name="queryCacheRegion"
       maxEntriesLocalHeap="20000"
       eternal="false"
       timeToLiveSeconds="1800"
       timeToIdleSeconds="900"
       memoryStoreEvictionPolicy="LRU">
</cache>
```

## Spring Cache Integration

### @Cacheable

Cache method return values:

```java
import org.springframework.cache.annotation.Cacheable;

@Service("productRecommendationService")
public class DefaultProductRecommendationService implements ProductRecommendationService {

    @Cacheable(value = "productRecommendations", key = "#productCode + '_' + #userId")
    @Override
    public List<ProductData> getRecommendations(final String productCode, final String userId) {
        // Expensive computation or external API call
        return computeRecommendations(productCode, userId);
    }
}
```

### @CacheEvict

Remove entries when data changes:

```java
import org.springframework.cache.annotation.CacheEvict;

@CacheEvict(value = "productRecommendations", allEntries = true)
public void updateProductData(final String productCode) {
    // Evicts all entries; prefix-based eviction requires a custom CacheManager
}

// Evict all entries in the cache
@CacheEvict(value = "productRecommendations", allEntries = true)
public void clearRecommendationCache() {
    // Triggered on catalog sync or major data update
}
```

### Spring Cache Manager Configuration

```xml
<bean id="cacheManager" class="org.springframework.cache.ehcache.EhCacheCacheManager">
    <property name="cacheManager" ref="ehCacheManager"/>
</bean>

<bean id="ehCacheManager" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
    <property name="configLocation" value="classpath:/ehcache-custom.xml"/>
    <property name="shared" value="true"/>
</bean>
```

## Cache Invalidation

### Automatic Invalidation

The platform automatically invalidates entity cache entries when:
- `ModelService.save()` is called
- `ModelService.remove()` is called
- ImpEx imports modify items
- Catalog sync updates items

### Manual Invalidation via HAC

Navigate to **Platform > Cache** in HAC:
- View cache statistics (hit/miss ratios)
- Clear specific cache regions
- Clear all caches

### Programmatic Cache Clearing

```java
import de.hybris.platform.regioncache.CacheController;

@Resource
private CacheController cacheController;

public void clearCaches() {
    cacheController.clearCache();
}
```

### FlexibleSearch Cache Control

Disable query caching for specific queries:

```java
final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {pk} FROM {Product}");
query.setDisableCaching(true); // Skip query cache for real-time results
```

## Cluster-Aware Caching

In clustered environments (CCv2), cache invalidation must propagate across nodes.

### Invalidation Broadcasting

SAP Commerce uses cluster-aware cache invalidation:

```properties
# Enable cluster cache invalidation
cluster.cache.invalidation.enabled=true

# Invalidation broadcast mode
cluster.cache.invalidation.mode=OPTIMISTIC
```

### Best Practices for Clusters

1. **Use platform caches**: They handle cluster invalidation automatically
2. **Avoid local-only caches**: Custom `ConcurrentHashMap` caches won't sync across nodes
3. **Use Spring Cache with EHCache**: Configure EHCache with replication for cluster support
4. **Monitor per-node hit ratios**: Uneven ratios indicate invalidation issues

## Monitoring

### HAC Cache Statistics

Navigate to **Platform > Cache** to view:
- Cache size (used entries / max entries)
- Hit ratio (higher is better; aim for >90%)
- Miss count and eviction count
- Per-region statistics

### Key Metrics

| Metric | Healthy | Warning | Action |
|--------|---------|---------|--------|
| Hit ratio | >90% | 70-90% | Increase cache size |
| Eviction rate | Low | High | Cache too small |
| Memory usage | <80% JVM heap | >80% | Reduce cache sizes |

### Diagnostic Properties

```properties
# Enable cache statistics
cache.region.entity.statsEnabled=true
cache.region.query.statsEnabled=true

# Log slow cache operations
cache.log.slow.operations=true
cache.log.slow.threshold.ms=100
```

### FlexibleSearch for Cache Analysis

```sql
-- Check items that are frequently accessed (and should be cached)
SELECT {code}, {itemtype}, COUNT(*) as accessCount
FROM {Product}
-- Use this with SQL profiling tools to identify hot items
```

## Common Pitfalls

### 1. Stale Data After Import

**Problem**: ImpEx imports don't invalidate entity cache in all scenarios.

**Solution**: Clear caches after large imports:
```properties
# In impex header
#% impex.enableCodeExecution(true);
#% afterEach: de.hybris.platform.core.Registry.getCurrentTenant().getCache().clear();
```

### 2. Memory Pressure from Over-Caching

**Problem**: Cache sizes too large for available JVM heap, causing GC pauses.

**Solution**: Size caches proportionally to available memory:
```
Total cache memory ≤ 40% of JVM heap
Entity cache: ~60% of cache budget
Query cache: ~25% of cache budget
Other caches: ~15% of cache budget
```

### 3. Cache Contention on Writes

**Problem**: High write throughput causes frequent cache invalidation, reducing hit ratios.

**Solution**: For write-heavy scenarios:
- Reduce entity cache TTL to avoid serving stale data
- Disable query caching for frequently-changing types
- Use `setDisableCaching(true)` on write-path queries

### 4. Missing Cache Warm-Up

**Problem**: After restart, empty caches cause a surge of database queries.

**Solution**: Implement cache warm-up during system startup:
```java
@SystemSetup(type = Type.PROJECT, process = Process.ALL)
public void warmUpCaches(final SystemSetupContext context) {
    // Pre-load frequently accessed items
    catalogService.getAllCatalogs();
    categoryService.getRootCategories();
}
```

### 5. Inconsistent Cache in Development

**Problem**: Local development caches stale data from previous builds.

**Solution**: Clear all caches after `ant clean all`:
```properties
# Auto-clear caches on startup in development
cache.clear.on.startup=true
```
