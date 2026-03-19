# Spring Configuration

## Table of Contents
- [Context Hierarchy](#context-hierarchy)
- [Bean Definition Patterns](#bean-definition-patterns)
- [Autowiring Strategies](#autowiring-strategies)
- [Configuration File Organization](#configuration-file-organization)
- [Property Files](#property-files)
- [AOP Configuration](#aop-configuration)
- [Event System](#event-system)
- [Interceptors and Validators](#interceptors-and-validators)
- [Bean Overriding](#bean-overriding)

## Context Hierarchy

SAP Commerce uses multiple Spring contexts:

```
Global Context (platform level)
    └── Tenant Context (per tenant)
        └── Web Context (per web app)
```

### Context Loading Order
1. `*-spring.xml` in extensions (by dependency order)
2. `*-web-spring.xml` for web modules
3. `local` properties override beans

## Bean Definition Patterns

### XML Configuration
```xml
<bean id="productService" class="com.example.services.impl.DefaultProductService">
    <property name="productDAO" ref="productDAO"/>
    <property name="modelService" ref="modelService"/>
</bean>
```

### Alias Pattern
```xml
<alias name="defaultProductService" alias="productService"/>
<bean id="defaultProductService" class="com.example.services.impl.DefaultProductService">
    <property name="productDAO" ref="productDAO"/>
</bean>
```

### Annotation-Based
```java
@Service("productService")
public class DefaultProductService implements ProductService {

    @Resource
    private ProductDAO productDAO;
}
```

### Component Scan
```xml
<context:component-scan base-package="com.example.services"/>
```

## Autowiring Strategies

> **Best Practice:** Prefer `@Resource` (JSR-250, name-based) over `@Autowired` (Spring, type-based) in SAP Commerce. The platform relies heavily on bean aliases (e.g., `alias="productService"` pointing to `defaultProductService`). `@Resource` resolves by bean name, which correctly follows alias chains. `@Autowired` resolves by type, which can cause ambiguity when multiple beans of the same type exist or when alias overrides are in play.

### @Resource (by name) — Preferred
```java
@Resource(name = "productDAO")
private ProductDAO productDAO;
```

### @Autowired (by type)
```java
@Autowired
private ProductDAO productDAO;

// With qualifier
@Autowired
@Qualifier("defaultProductDAO")
private ProductDAO productDAO;
```

### Constructor Injection
```java
@Service
public class DefaultProductService {

    private final ProductDAO productDAO;

    @Autowired
    public DefaultProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }
}
```

### Setter Injection
```java
private ProductDAO productDAO;

@Autowired
public void setProductDAO(ProductDAO productDAO) {
    this.productDAO = productDAO;
}
```

## Configuration File Organization

### Naming Convention
```
resources/
├── customextension-spring.xml      # Core beans
├── customextension-facades-spring.xml  # Facade beans
├── customextension-services-spring.xml # Service beans
└── customextension-daos-spring.xml     # DAO beans
```

### Import Statements
```xml
<import resource="customextension-daos-spring.xml"/>
<import resource="customextension-services-spring.xml"/>
<import resource="customextension-facades-spring.xml"/>
```

### Web Module Config
```
web/webroot/WEB-INF/
├── customextension-web-spring.xml  # Web beans
└── config/
    └── customextension-spring-mvc.xml  # MVC config
```

## Property Files

### project.properties
Extension default properties:
```properties
# customextension/project.properties
customextension.feature.enabled=true
customextension.cache.ttl=3600
```

### local.properties
Environment-specific overrides:
```properties
# config/local.properties
customextension.feature.enabled=false
db.url=jdbc:mysql://localhost:3306/hybris
```

### Accessing Properties
```java
@Value("${customextension.feature.enabled:false}")
private boolean featureEnabled;

// Or via ConfigurationService
@Resource
private ConfigurationService configurationService;

boolean enabled = configurationService.getConfiguration()
    .getBoolean("customextension.feature.enabled", false);
```

## AOP Configuration

### Interceptors via XML
```xml
<bean id="loggingInterceptor" class="com.example.interceptors.LoggingInterceptor"/>

<aop:config>
    <aop:pointcut id="servicePointcut"
                  expression="execution(* com.example.services.*.*(..))"/>
    <aop:advisor advice-ref="loggingInterceptor" pointcut-ref="servicePointcut"/>
</aop:config>
```

### Annotation-Based AOP
```java
@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.example.services.*.*(..))")
    public void logMethodEntry(JoinPoint joinPoint) {
        LOG.debug("Entering: " + joinPoint.getSignature());
    }

    @Around("@annotation(Timed)")
    public Object timeMethod(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        LOG.info("Method took: " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }
}
```

## Event System

### Publishing Events
```java
@Resource
private EventService eventService;

public void createOrder(OrderModel order) {
    modelService.save(order);
    eventService.publishEvent(new OrderCreatedEvent(order));
}
```

### Event Listener
```java
public class OrderCreatedEventListener extends AbstractEventListener<OrderCreatedEvent> {

    @Override
    protected void onEvent(OrderCreatedEvent event) {
        OrderModel order = event.getOrder();
        // Handle event
    }
}
```

### Register Listener
```xml
<bean id="orderCreatedEventListener"
      class="com.example.listeners.OrderCreatedEventListener"
      parent="abstractEventListener">
    <property name="modelService" ref="modelService"/>
</bean>
```

### Cluster-Aware Events
```java
@Resource
private ClusterEventService clusterEventService;

// Publish to all nodes
clusterEventService.publishEvent(new ClusterAwareEvent());
```

## Interceptors and Validators

### Model Interceptors
```java
public class ProductValidationInterceptor implements ValidateInterceptor<ProductModel> {

    @Override
    public void onValidate(ProductModel product, InterceptorContext ctx)
            throws InterceptorException {
        if (product.getCode() == null || product.getCode().isEmpty()) {
            throw new InterceptorException("Product code is required");
        }
    }
}
```

### Register Interceptor
```xml
<bean id="productValidationInterceptor"
      class="com.example.interceptors.ProductValidationInterceptor"/>

<bean id="productValidationInterceptorMapping"
      class="de.hybris.platform.servicelayer.interceptor.impl.InterceptorMapping">
    <property name="interceptor" ref="productValidationInterceptor"/>
    <property name="typeCode" value="Product"/>
</bean>
```

### Interceptor Types
- `PrepareInterceptor`: Before model modification
- `ValidateInterceptor`: Before save (validation)
- `LoadInterceptor`: After load from database
- `InitDefaultsInterceptor`: When creating new model
- `RemoveInterceptor`: Before deletion

## Bean Overriding

### Via Alias
```xml
<!-- Original -->
<alias name="defaultProductService" alias="productService"/>

<!-- Override in custom extension -->
<alias name="customProductService" alias="productService"/>
<bean id="customProductService"
      class="com.example.services.impl.CustomProductService"
      parent="defaultProductService">
    <!-- Additional config -->
</bean>
```

### Via Parent Bean
```xml
<bean id="customProductFacade"
      class="com.example.facades.impl.CustomProductFacade"
      parent="defaultProductFacade">
    <property name="customService" ref="customService"/>
</bean>
```

### Bean Inheritance
```xml
<!-- Abstract parent -->
<bean id="abstractService" abstract="true">
    <property name="modelService" ref="modelService"/>
    <property name="sessionService" ref="sessionService"/>
</bean>

<!-- Concrete child -->
<bean id="productService" parent="abstractService"
      class="com.example.services.impl.DefaultProductService">
    <property name="productDAO" ref="productDAO"/>
</bean>
```

### Conditional Beans
```xml
<bean id="featureService"
      class="com.example.services.impl.FeatureServiceImpl">
    <property name="enabled" value="${feature.enabled:false}"/>
</bean>
```
