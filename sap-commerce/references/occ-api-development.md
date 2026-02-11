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
@Api(tags = "Products")
public class ProductController {

    @Resource
    private ProductFacade productFacade;

    @RequestMapping(value = "/{productCode}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get product details")
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
- `@Api`, `@ApiOperation`: Swagger docs

## DTO Mapping

### WsDTO Definition
```java
@ApiModel(value = "Product")
public class ProductWsDTO {

    @ApiModelProperty(value = "Product code", required = true)
    private String code;

    @ApiModelProperty(value = "Product name")
    private String name;

    @ApiModelProperty(value = "Price information")
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
OCC uses OAuth2 for authentication.

**Token Endpoint:**
```
POST /authorizationserver/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=mobile_android&client_secret=secret&username=user&password=pass
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
@Api(tags = "Custom Resource")
public class CustomResourceController {

    @Resource
    private CustomFacade customFacade;

    @Resource
    private DataMapper dataMapper;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Get custom resources")
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

## Swagger Documentation

### Enable Swagger
In `local.properties`:
```properties
commercewebservices.swagger.enabled=true
```

### Access Swagger UI
```
https://{host}/occ/v2/swagger-ui.html
```

### Annotations
```java
@Api(tags = "Products", description = "Product operations")
@ApiOperation(value = "Get product", notes = "Returns product details")
@ApiParam(value = "Product code", required = true)
@ApiResponse(code = 200, message = "Success")
@ApiResponse(code = 404, message = "Product not found")
```

### Model Documentation
```java
@ApiModel(value = "Product", description = "Product representation")
public class ProductWsDTO {

    @ApiModelProperty(value = "Unique product code", required = true, example = "12345")
    private String code;
}
```
