# OCC API Development

## Table of Contents
- [OCC Architecture Overview](#occ-architecture-overview)
- [Controller Implementation](#controller-implementation)
- [DTO Mapping](#dto-mapping)
- [Request Response Handling](#request-response-handling)
- [Authentication and Authorization](#authentication-and-authorization)
- [API Versioning](#api-versioning)
- [Error Handling](#error-handling)
- [Extending Existing Endpoints](#extending-existing-endpoints)
- [Creating Custom Endpoints](#creating-custom-endpoints)
- [Swagger Documentation](#swagger-documentation)

## OCC Architecture Overview

Omni Commerce Connect (OCC) exposes REST APIs via `commercewebservices` extension.

**URL Structure:**
```
https://{host}/occ/v2/{baseSiteId}/{resource}
```

**Key Components:**
- Controllers: Handle HTTP requests
- WsDTOs: Data transfer objects for API
- Populators: Convert models to DTOs
- Validators: Input validation

## Controller Implementation

### Basic Controller
```java
@Controller
@RequestMapping("/{baseSiteId}/products")
@Tag(name = "Products")
public class ProductController {

    @Resource
    private ProductFacade productFacade;

    @RequestMapping(value = "/{productCode}", method = RequestMethod.GET)
    @ResponseBody
    @Operation(summary = "Get product details")
    public ProductWsDTO getProduct(
            @PathVariable String baseSiteId,
            @PathVariable String productCode,
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        ProductData productData = productFacade.getProductForCode(productCode);
        return dataMapper.map(productData, ProductWsDTO.class, fields);
    }
}
```

### Annotations
- `@Controller`: Spring MVC controller
- `@RequestMapping`: URL mapping
- `@ResponseBody`: Return JSON/XML
- `@Tag`, `@Operation`: OpenAPI 3 documentation

## DTO Mapping

### WsDTO Definition
```java
@Schema(name = "Product")
public class ProductWsDTO {

    @Schema(description = "Product code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "Product name")
    private String name;

    @Schema(description = "Price information")
    private PriceWsDTO price;

    // Getters and setters
}
```

### Field Mapping Levels
Configure in `*-web-spring.xml`:
```xml
<bean parent="fieldSetLevelMapping">
    <property name="dtoClass" value="com.example.dto.ProductWsDTO"/>
    <property name="levelMapping">
        <map>
            <entry key="BASIC" value="code,name"/>
            <entry key="DEFAULT" value="code,name,price,description"/>
            <entry key="FULL" value="code,name,price,description,images,categories"/>
        </map>
    </property>
</bean>
```

### DataMapper Usage
```java
@Resource
private DataMapper dataMapper;

ProductWsDTO dto = dataMapper.map(productData, ProductWsDTO.class, "FULL");
```

## Request Response Handling

### Path Variables
```java
@RequestMapping("/{baseSiteId}/users/{userId}/orders/{orderCode}")
public OrderWsDTO getOrder(
        @PathVariable String baseSiteId,
        @PathVariable String userId,
        @PathVariable String orderCode) {
    // ...
}
```

### Query Parameters
```java
@RequestMapping(value = "/search", method = RequestMethod.GET)
public ProductSearchPageWsDTO search(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") int currentPage,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(defaultValue = "relevance") String sort) {
    // ...
}
```

### Request Body
```java
@RequestMapping(method = RequestMethod.POST)
@ResponseStatus(HttpStatus.CREATED)
public CartWsDTO createCart(
        @RequestBody CartRequestWsDTO cartRequest) {
    // ...
}
```

## Authentication and Authorization

### OAuth2 Configuration
On `2211-jdk21`, OCC uses the Spring-based `authorizationserver`, `resourceserver`, and `oauth2commons` extensions. The password and implicit grants from the old `oauth2` extension are not supported.

Browser clients should use authorization code with PKCE. Confidential machine clients can use client credentials:
```
POST /authorizationserver/oauth/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <base64(client_id:client_secret)>

grant_type=client_credentials
```

### Secure Endpoints
```java
@Secured({"ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT"})
@RequestMapping(value = "/orders", method = RequestMethod.GET)
public OrderListWsDTO getOrders() {
    // Only authenticated users
}
```

### Anonymous Access
```java
@RequestMapping(value = "/products", method = RequestMethod.GET)
public ProductListWsDTO getProducts() {
    // No @Secured = public access
}
```

## API Versioning

### URL Versioning
OCC uses URL path versioning: `/occ/v2/`

### Maintaining Compatibility
- Keep v1 endpoints working
- Add new fields as optional
- Deprecate with `@Deprecated` annotation

### Version-Specific DTOs
```java
// v1
public class ProductWsDTO { ... }

// v2
public class ProductV2WsDTO extends ProductWsDTO {
    private List<VariantWsDTO> variants;
}
```

## Error Handling

### Exception Handler
```java
@ControllerAdvice
public class WebservicesExceptionHandler {

    @ExceptionHandler(UnknownIdentifierException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorListWsDTO handleNotFound(UnknownIdentifierException ex) {
        return createErrorResponse("notFound", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorListWsDTO handleValidation(ValidationException ex) {
        return createErrorResponse("validation", ex.getMessage());
    }
}
```

### Error Response Format
```json
{
  "errors": [
    {
      "type": "ValidationError",
      "message": "Product code is required",
      "reason": "missing"
    }
  ]
}
```

## Extending Existing Endpoints

### Subclass Controller
```java
@Controller
@RequestMapping("/{baseSiteId}/products")
public class CustomProductController extends ProductsController {

    @Override
    @RequestMapping(value = "/{productCode}", method = RequestMethod.GET)
    @ResponseBody
    public ProductWsDTO getProduct(
            @PathVariable String baseSiteId,
            @PathVariable String productCode,
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        // Custom logic before
        ProductWsDTO result = super.getProduct(baseSiteId, productCode, fields);
        // Custom logic after
        return result;
    }
}
```

### Add New Endpoint to Existing Controller
```java
@RequestMapping(value = "/{productCode}/availability", method = RequestMethod.GET)
@ResponseBody
public StockWsDTO getProductAvailability(
        @PathVariable String productCode) {
    // New endpoint
}
```

## Creating Custom Endpoints

### New Controller
```java
@Controller
@RequestMapping("/{baseSiteId}/customresource")
@Tag(name = "Custom Resource")
public class CustomResourceController {

    @Resource
    private CustomFacade customFacade;

    @Resource
    private DataMapper dataMapper;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @Operation(summary = "Get custom resources")
    public CustomResourceListWsDTO getResources(
            @PathVariable String baseSiteId,
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        List<CustomResourceData> data = customFacade.getResources();
        CustomResourceListWsDTO dto = new CustomResourceListWsDTO();
        dto.setResources(dataMapper.mapAsList(data, CustomResourceWsDTO.class, fields));
        return dto;
    }

    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    @ResponseBody
    public CustomResourceWsDTO getResource(
            @PathVariable String baseSiteId,
            @PathVariable String code,
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        CustomResourceData data = customFacade.getResource(code);
        return dataMapper.map(data, CustomResourceWsDTO.class, fields);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public CustomResourceWsDTO createResource(
            @PathVariable String baseSiteId,
            @RequestBody CustomResourceWsDTO resource) {

        CustomResourceData data = dataMapper.map(resource, CustomResourceData.class);
        CustomResourceData created = customFacade.createResource(data);
        return dataMapper.map(created, CustomResourceWsDTO.class, "FULL");
    }
}
```

### Spring Configuration
```xml
<bean id="customResourceController"
      class="com.example.controllers.CustomResourceController">
    <property name="customFacade" ref="customFacade"/>
    <property name="dataMapper" ref="dataMapper"/>
</bean>
```

## Swagger / OpenAPI Documentation

SAP Commerce `2211-jdk21` examples use **OpenAPI 3** annotations from `io.swagger.v3.oas.annotations`.

### Enable Swagger / OpenAPI UI
In `local.properties`:
```properties
commercewebservices.swagger.enabled=true
```

### Access Swagger UI
```
https://{host}/occ/v2/swagger-ui.html
```

### OpenAPI 3 Annotations
```java
@Tag(name = "Products", description = "Product operations")
@Operation(summary = "Get product", description = "Returns product details")
@Parameter(description = "Product code", required = true)
@ApiResponse(responseCode = "200", description = "Success")
@ApiResponse(responseCode = "404", description = "Product not found")
```

### Model Documentation
```java
@Schema(description = "Product representation")
public class ProductWsDTO {

    @Schema(description = "Unique product code", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    private String code;
}
```

## CORS Configuration (for Composable Storefront / Headless)

When Composable Storefront or any headless frontend calls OCC APIs from a different origin, configure CORS:

```properties
# local.properties or CCv2 api aspect properties
corsfilter.commercewebservices.allowedOriginPatterns=http://localhost:4200 https://yourstorefront.com
corsfilter.commercewebservices.allowedMethods=GET HEAD OPTIONS PATCH PUT POST DELETE
corsfilter.commercewebservices.allowedHeaders=origin content-type accept authorization cache-control if-none-match x-anonymous-consents x-profile-tag-debug x-consent-reference occ-personalization-id occ-personalization-time
corsfilter.commercewebservices.exposedHeaders=x-anonymous-consents
corsfilter.commercewebservices.allowCredentials=true
```
