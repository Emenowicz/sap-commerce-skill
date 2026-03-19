# Testing Guide

## Table of Contents
- [Overview](#overview)
- [Test Annotations](#test-annotations)
- [Unit Tests (JUnit)](#unit-tests-junit)
- [Integration Tests](#integration-tests)
- [Spock Tests (Groovy)](#spock-tests-groovy)
- [Service Layer Testing](#service-layer-testing)
- [DAO Testing with FlexibleSearch](#dao-testing-with-flexiblesearch)
- [OCC API Testing](#occ-api-testing)
- [ImpEx Testing](#impex-testing)
- [Running Tests](#running-tests)
- [CCv2 Test Configuration](#ccv2-test-configuration)
- [Test Best Practices](#test-best-practices)

## Overview

SAP Commerce uses JUnit as the base testing framework, with support for Groovy-based Spock tests. Tests are classified by annotation to control which tests run in which context (local build, CCv2 build, CI).

## Test Annotations

SAP Commerce uses annotations from `de.hybris.bootstrap.annotations` to classify tests:

| Annotation | Description | Requires DB |
|------------|-------------|-------------|
| `@UnitTest` | Pure unit tests — no Spring context, no DB | No |
| `@IntegrationTest` | Spring context loaded; may use in-memory or real DB | Yes |
| `@PerformanceTest` | Performance/load tests — excluded from regular runs | Yes |
| `@ManualTest` | Manual-only tests — never run in CI | - |
| `@DemoTest` | Demo/showcase tests | Yes |

```java
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.bootstrap.annotations.IntegrationTest;
import org.junit.Test;

@UnitTest
public class MyServiceUnitTest { ... }

@IntegrationTest
public class MyServiceIntegrationTest extends ServicelayerTest { ... }
```

## Unit Tests (JUnit)

Unit tests should have **no Spring context** and use mocking:

```java
import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@UnitTest
public class DefaultProductFacadeUnitTest {

    @InjectMocks
    private DefaultProductFacade productFacade;

    @Mock
    private ProductService productService;

    @Mock
    private Converter<ProductModel, ProductData> productConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getProductForCode_shouldReturnConvertedData() {
        // Given
        final String code = "TEST_PRODUCT";
        final ProductModel model = new ProductModel();
        model.setCode(code);
        final ProductData data = new ProductData();
        data.setCode(code);

        when(productService.getProductForCode(code)).thenReturn(model);
        when(productConverter.convert(model)).thenReturn(data);

        // When
        final ProductData result = productFacade.getProductForCode(code);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(code);
        verify(productService).getProductForCode(code);
        verify(productConverter).convert(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getProductForCode_withNullCode_shouldThrow() {
        productFacade.getProductForCode(null);
    }
}
```

## Integration Tests

Integration tests load the Spring context and have access to the full service layer and database:

```java
import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.servicelayer.ServicelayerTest;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ProductDAOIntegrationTest extends ServicelayerTest {

    @Resource
    private ProductDAO productDAO;

    @Resource
    private ModelService modelService;

    @Resource
    private CatalogVersionService catalogVersionService;

    @Before
    public void setUp() throws Exception {
        // Import test data via ImpEx
        importCsv("/myextension/test/testdata-products.impex", "utf-8");
    }

    @Test
    public void findByCode_shouldReturnProduct() {
        // Given
        final String code = "TEST-PRODUCT-001";

        // When
        final ProductModel product = productDAO.findByCode(code);

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getCode()).isEqualTo(code);
    }

    @Test
    public void findByCode_nonExistent_shouldReturnNull() {
        final ProductModel product = productDAO.findByCode("NON-EXISTENT");
        assertThat(product).isNull();
    }
}
```

### Test ImpEx Location
Place test ImpEx files in:
```
myextension/
└── resources/
    └── myextension/
        └── test/
            └── testdata-products.impex
```

Load in test:
```java
importCsv("/myextension/test/testdata-products.impex", "utf-8");
```

## Spock Tests (Groovy)

Spock is a Groovy-based BDD testing framework that produces very readable tests. SAP Commerce supports Spock tests in `testsrc/` or `web/testsrc/`.

### Enable Spock Compilation

```properties
# local.properties
compile.groovy.web.tests=true
<extname>.backoffice.compile.groovy=true
```

### Unit Test with Spock

```groovy
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.product.ProductService
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultProductFacadeSpec extends Specification {

    def productService = Mock(ProductService)
    def productConverter = Mock(Converter)
    def facade = new DefaultProductFacade(
        productService: productService,
        productConverter: productConverter
    )

    def "getProductForCode returns converted data for valid code"() {
        given:
        def model = new ProductModel(code: "P001")
        def data = new ProductData(code: "P001")

        productService.getProductForCode("P001") >> model
        productConverter.convert(model) >> data

        when:
        def result = facade.getProductForCode("P001")

        then:
        result.code == "P001"
        1 * productService.getProductForCode("P001")
    }

    @Unroll
    def "getProductForCode throws for invalid code: '#code'"() {
        when:
        facade.getProductForCode(code)

        then:
        thrown(IllegalArgumentException)

        where:
        code << [null, "", "  "]
    }
}
```

### Integration Test with Spock

```groovy
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.servicelayer.ServicelayerSpockSpecification

import javax.annotation.Resource

@IntegrationTest
class ProductDAOSpec extends ServicelayerSpockSpecification {

    @Resource
    ProductDAO productDAO

    def setup() {
        importCsv("/myextension/test/testdata-products.impex", "utf-8")
    }

    def "findByCode returns product for existing code"() {
        when:
        def product = productDAO.findByCode("TEST-PRODUCT-001")

        then:
        product != null
        product.code == "TEST-PRODUCT-001"
    }
}
```

## Service Layer Testing

### Testing with ModelService

```java
@IntegrationTest
public class ProductServiceIntegrationTest extends ServicelayerTest {

    @Resource
    private DefaultProductService productService;

    @Resource
    private ModelService modelService;

    @Test
    public void updateProductStock_shouldPersistChange() {
        // Given
        final ProductModel product = modelService.create(ProductModel.class);
        product.setCode("STOCK-TEST");
        product.setCatalogVersion(/* set catalog version */);
        modelService.save(product);

        // When
        productService.updateProductStock("STOCK-TEST", 50);

        // Then
        final ProductModel updated = productService.getProductForCode("STOCK-TEST");
        assertThat(updated.getStockLevel()).isEqualTo(50);
    }
}
```

### Testing Interceptors

```java
@UnitTest
public class ProductValidationInterceptorUnitTest {

    private ProductValidationInterceptor interceptor = new ProductValidationInterceptor();

    @Test(expected = InterceptorException.class)
    public void onValidate_withNullCode_shouldThrow() throws InterceptorException {
        ProductModel product = new ProductModel();
        product.setCode(null);
        interceptor.onValidate(product, mock(InterceptorContext.class));
    }

    @Test
    public void onValidate_withValidCode_shouldNotThrow() throws InterceptorException {
        ProductModel product = new ProductModel();
        product.setCode("VALID-CODE");
        interceptor.onValidate(product, mock(InterceptorContext.class));
        // No exception expected
    }
}
```

## DAO Testing with FlexibleSearch

```java
@IntegrationTest
public class ProductDAOIntegrationTest extends ServicelayerTransactionalTest {

    @Resource
    private FlexibleSearchService flexibleSearchService;

    @Resource
    private DefaultProductDAO productDAO;

    @Before
    public void setUp() throws Exception {
        importCsv("/myextension/test/testdata.impex", "utf-8");
    }

    @Test
    public void findActiveProducts_shouldReturnOnlyApproved() {
        final List<ProductModel> products = productDAO.findActiveProducts();

        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(p -> p.getApprovalStatus() == ArticleApprovalStatus.APPROVED);
    }
}
```

> **Tip:** Extend `ServicelayerTransactionalTest` instead of `ServicelayerTest` when you want each test to be rolled back after execution (avoids test data pollution).

## OCC API Testing

### Integration Test for OCC Controllers

```java
import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.oauth2.constants.OAuth2Constants;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class ProductControllerIntegrationTest extends OccApiTest {

    @Before
    public void setUp() throws Exception {
        importCsv("/myextension/test/testdata-occ.impex", "utf-8");
    }

    @Test
    public void getProduct_shouldReturn200WithProductData() throws Exception {
        given()
            .header("Authorization", "Bearer " + getToken())
        .when()
            .get("/occ/v2/electronics/products/300938")
        .then()
            .statusCode(200)
            .body("code", equalTo("300938"))
            .body("name", notNullValue());
    }
}
```

### Using REST Assured for OCC Testing

```java
import io.restassured.RestAssured;
import io.restassured.response.Response;

// Get OAuth token
Response tokenResponse = RestAssured.given()
    .contentType("application/x-www-form-urlencoded")
    .formParam("grant_type", "client_credentials")
    .formParam("client_id", "mobile_android")
    .formParam("client_secret", "secret")
    .post("/authorizationserver/oauth/token");

String token = tokenResponse.jsonPath().getString("access_token");

// Test OCC endpoint
RestAssured.given()
    .header("Authorization", "Bearer " + token)
    .when()
    .get("/occ/v2/electronics/products/300938")
    .then()
    .statusCode(200);
```

## ImpEx Testing

Test ImpEx scripts before importing:

```bash
# Via HAC
# Navigate to Platform > ImpEx Import > check "Enable Code Execution"
# Paste ImpEx and validate

# Via script
scripts/validate-impex.sh path/to/data.impex
```

### Test Data ImpEx Best Practices

```impex
# Use test-only codes to avoid conflicts
$testPrefix=TEST_

INSERT_UPDATE Product;code[unique=true];name[lang=en];catalogVersion(catalog(id),version)
;${testPrefix}PROD001;Test Product 1;testCatalog:Online
;${testPrefix}PROD002;Test Product 2;testCatalog:Online
```

## Running Tests

### Local Build

```bash
# Run all unit tests
ant unittests

# Run unit tests for a specific extension
ant unittests -Dtestclasses.extensions=myextension

# Run integration tests (requires running DB)
ant integrationtests

# Run integration tests for a specific extension
ant integrationtests -Dtestclasses.extensions=myextension

# Run specific test class
ant unittests -Dtestclasses.names=com.example.MyServiceTest

# Run Spock tests
ant alltests -Dtestclasses.extensions=myextension
```

### Via Ant with Annotations Filter

```bash
# Only run @UnitTest annotated tests
ant unittests -Dtestclasses.annotations=UnitTest

# Run both @UnitTest and @IntegrationTest
ant alltests -Dtestclasses.annotations=UnitTest,IntegrationTest
```

### Test Reports
Test results are written to:
```
hybris/log/testing/
```
HTML reports available after `ant alltests` in:
```
hybris/log/testing/unit/index.html
hybris/log/testing/integration/index.html
```

## CCv2 Test Configuration

Configure in `manifest.json`:

```json
{
  "tests": {
    "extensions": ["myextension", "myextension2"],
    "annotations": ["UnitTest", "IntegrationTest"],
    "packages": ["de.hybris.bootstrap.annotations"],
    "excludedGroups": ["ManualTest", "PerformanceTest"]
  }
}
```

> CCv2 runs tests automatically on each build. Failing tests block deployment. Ensure integration tests don't rely on external systems (use mocks or stubs).

## Test Best Practices

### 1. Isolate Tests with Transactions
Use `ServicelayerTransactionalTest` to roll back after each test:
```java
@IntegrationTest
public class MyDaoTest extends ServicelayerTransactionalTest { ... }
```

### 2. Use Test-Specific ImpEx Prefixes
Prefix test data codes to avoid collisions with other tests:
```impex
$testPrefix=MYEXT_TEST_
;${testPrefix}PROD001;Test Product;...
```

### 3. Mock External Dependencies in Unit Tests
```java
@Mock
private ExternalPaymentGateway paymentGateway;

@Before
public void setUp() {
    when(paymentGateway.charge(any())).thenReturn(AuthResult.success("AUTH-001"));
}
```

### 4. Never Use `@Autowired` in Tests — Use `@Resource`
SAP Commerce beans use alias patterns. `@Resource` resolves by name (following aliases correctly).

### 5. Test at the Right Layer
- **Business logic / calculations** → Unit test the Service
- **DB queries** → Integration test the DAO
- **End-to-end scenarios** → Integration test via Facade or OCC

### 6. Keep Integration Tests Fast
- Minimize ImpEx test data to exactly what's needed
- Use `ServicelayerTransactionalTest` for DB cleanup
- Avoid full catalog imports in unit/integration tests

### 7. Assert Meaningfully
```java
// Prefer assertj's fluent assertions over JUnit basic
assertThat(products)
    .hasSize(3)
    .extracting(ProductModel::getCode)
    .containsExactlyInAnyOrder("P001", "P002", "P003");
```
