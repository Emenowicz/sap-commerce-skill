# Troubleshooting Guide

## Table of Contents
- [Build Errors](#build-errors)
- [Type System Issues](#type-system-issues)
- [Spring Configuration Problems](#spring-configuration-problems)
- [ImpEx Import Failures](#impex-import-failures)
- [FlexibleSearch Performance Issues](#flexiblesearch-performance-issues)
- [Database Connection Problems](#database-connection-problems)
- [OCC API Errors](#occ-api-errors)
- [Cloud-Specific Issues](#cloud-specific-issues)
- [Solr/Search Issues](#solrsearch-issues)
- [Common Exceptions](#common-exceptions)
- [Debugging Techniques](#debugging-techniques)
- [Log Analysis](#log-analysis)

## Build Errors

### OutOfMemoryError During Build
Add to `setantenv.sh` or `setantenv.bat`:
```bash
export ANT_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

### Missing Dependencies
Check `extensioninfo.xml` for required extensions:
```xml
<requires-extension name="commerceservices"/>
```
Verify extension exists in `localextensions.xml`.

### Compilation Errors After Type Changes
Run full rebuild:
```bash
ant clean all
```
If persists, delete `gensrc` and `classes` folders manually.

### Circular Extension Dependencies
Check extension load order. Use `ant extensionorder` to visualize dependencies. Refactor to break cycles.

## Type System Issues

### Deployment Errors
Check for duplicate attribute names across type hierarchy. Verify deployment table names are unique.

### Type Conflicts
Run typecode check:
```bash
ant typecodecheck
```
Ensure unique typecodes in `items.xml`:
```xml
<itemtype code="CustomProduct" extends="Product"
          jaloclass="com.company.jalo.CustomProduct"
          autocreate="true" generate="true">
    <deployment table="CustomProducts" typecode="10001"/>
</itemtype>
```

### Duplicate Typecodes
Each itemtype needs unique typecode (10000-32767 for custom types). Check all extensions for conflicts.

### Model Not Generated
Verify `generate="true"` on itemtype. Run `ant clean all` to regenerate models.

## Spring Configuration Problems

### Bean Not Found
Check bean ID matches exactly (case-sensitive). Verify Spring XML is loaded:
```xml
<import resource="classpath:/myextension-spring.xml"/>
```

### Circular Dependencies
Use `@Lazy` annotation or setter injection instead of constructor injection:
```java
@Autowired
public void setProductService(@Lazy ProductService productService) {
    this.productService = productService;
}
```

### Context Loading Failures
Enable debug logging:
```properties
log4j2.logger.spring.name = org.springframework
log4j2.logger.spring.level = DEBUG
```
Check for missing required beans or incorrect bean class paths.

## ImpEx Import Failures

### Syntax Errors
Common issues:
- Missing semicolons at line end
- Wrong column separator (use `;` not `,`)
- Unclosed quotes in values

### Validation Errors
Check type attribute exists. Verify mandatory attributes are provided:
```impex
INSERT_UPDATE Product;code[unique=true];name[lang=en];catalogVersion(catalog(id),version)
;PROD001;My Product;Default:Online
```

### Reference Resolution Failures
Use deferred import for circular references:
```impex
INSERT_UPDATE Product;code[unique=true];supercategories(code,catalogVersion(catalog(id),version))[mode=append]
```
Or split into multiple ImpEx files with proper order.

## FlexibleSearch Performance Issues

### Slow Queries
Create indexes for frequently queried attributes:
```xml
<attribute qualifier="customField" type="java.lang.String">
    <persistence type="property"/>
    <modifiers read="true" write="true" search="true"/>
</attribute>
```

### Missing Indexes
Add database index via ImpEx:
```impex
INSERT_UPDATE IndexDefinition;...
```
Or create directly in database for non-type attributes.

### N+1 Query Problem
Use JOIN in FlexibleSearch instead of lazy loading:
```sql
SELECT {p.pk}, {c.code} FROM {Product AS p
JOIN Category AS c ON {p.supercategories} = {c.pk}}
WHERE {p.code} = ?code
```

## Database Connection Problems

### Connection Pool Exhausted
Increase pool size in `local.properties`:
```properties
db.pool.maxActive=100
db.pool.maxIdle=50
```

### Connection Timeouts
Configure timeout settings:
```properties
db.pool.maxWait=30000
db.connectionTimeout=30000
```

### Deadlocks
Check transaction isolation level. Avoid long-running transactions. Use `@Transactional(timeout=30)`.

## OCC API Errors

### 401 Unauthorized
Verify OAuth client credentials. Check token endpoint:
```
POST /authorizationserver/oauth/token
```
Ensure client is configured in ImpEx:
```impex
INSERT_UPDATE OAuthClientDetails;clientId[unique=true];...
```

### 403 Forbidden
Check user permissions and OAuth scopes. Verify `oauthauthorizations` configuration.

### 500 Internal Server Error
Enable detailed error messages:
```properties
webservicescommons.resthandlerexceptionresolver.webservices.showStackTrace=true
```
Check server logs for stack trace.

## Cloud-Specific Issues

### Deployment Failures
Validate `manifest.json` syntax. Check extension names match exactly.

### Environment Configuration
Verify properties in CCV2 portal. Check environment-specific `local.properties` overlay.

### Build Timeout
Optimize build by excluding unnecessary extensions. Check `manifest.json` aspects configuration.

## Solr/Search Issues

### Indexing Not Running
Trigger full index:
```bash
ant solrindex -Dsolrindex.type=full
```
Or via HAC: Administration > Solr > Index

### No Search Results
Verify indexed properties configuration. Check Solr server connectivity:
```properties
solrserver.instances.default.url=http://localhost:8983/solr
```

### Facet Not Showing
Check facet configuration in Solr. Verify `FacetSearchConfig` in Backoffice.

## Common Exceptions

### ModelSavingException
Check mandatory attributes are set. Verify unique constraints aren't violated. Check interceptors for validation errors.

### UnknownIdentifierException
Item with given identifier not found. Verify code/pk exists:
```java
try {
    productService.getProductForCode(code);
} catch (UnknownIdentifierException e) {
    // Handle not found
}
```

### AmbiguousIdentifierException
Multiple items found for unique lookup. Check data integrity. Use catalog version qualifier.

### SystemException
Generic platform error. Check cause chain in logs. Often wraps database or transaction errors.

## Debugging Techniques

### HAC Console
Access at `/hac`. Use:
- Scripting console for Groovy/BeanShell
- FlexibleSearch console for query testing
- ImpEx import/export

### Logging Configuration
Add to `local.properties`:
```properties
log4j2.logger.myextension.name = com.company.myextension
log4j2.logger.myextension.level = DEBUG
```

### Remote Debugging
Add to `tomcat/bin/setenv.sh`:
```bash
export CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
```
Connect IDE debugger to port 8000.

## Log Analysis

### Log Locations
- Tomcat: `hybris/log/tomcat/console*.log`
- Hybris: `hybris/log/y*.log`
- Database: Check database server logs

### Useful Grep Patterns
```bash
# Find errors
grep -i "ERROR\|Exception" hybris/log/tomcat/console.log

# Find specific class issues
grep "com.company.myclass" hybris/log/y*.log

# Find slow queries
grep "took [0-9]\{4,\}ms" hybris/log/y*.log
```

### Log Levels
Set appropriate levels to avoid log bloat:
- ERROR: Production
- WARN: Staging
- INFO: Development
- DEBUG: Active debugging only
