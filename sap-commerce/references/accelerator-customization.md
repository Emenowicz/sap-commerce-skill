# Accelerator Customization

## Table of Contents
- [B2C Accelerator Architecture](#b2c-accelerator-architecture)
- [B2B Accelerator Architecture](#b2b-accelerator-architecture)
- [Storefront Customization](#storefront-customization)
- [Checkout Flow Customization](#checkout-flow-customization)
- [CMS Component Development](#cms-component-development)
- [AddOn Structure](#addon-structure)
- [Theme Customization](#theme-customization)
- [Payment Gateway Integration](#payment-gateway-integration)
- [SmartEdit Integration](#smartedit-integration)

## B2C Accelerator Architecture

### Key Extensions
- `yacceleratorstorefront`: Storefront web application
- `yacceleratorfacades`: Facade implementations
- `yacceleratorcore`: Core services and DAOs
- `yacceleratorfulfilmentprocess`: Order fulfillment

### Structure
```
yacceleratorstorefront/
├── web/
│   ├── src/            # Controllers, filters
│   ├── webroot/
│   │   ├── WEB-INF/
│   │   │   ├── views/  # JSP pages
│   │   │   ├── tags/   # Tag libraries
│   │   │   └── config/ # Spring configs
│   │   └── _ui/        # Static resources
│   └── testsrc/
```

## B2B Accelerator Architecture

### Additional Features
- Account management dashboard
- Approval workflows
- Organization hierarchy
- Cost centers and budgets
- Quick order and reorder

### Key Extensions
- `b2bacceleratoraddon`: B2B-specific features
- `b2bapprovalprocess`: Order approval workflow
- `b2bpunchout`: PunchOut integration

## Storefront Customization

### Controller Extension
```java
@Controller
@RequestMapping("/cart")
public class CustomCartPageController extends CartPageController {

    @Override
    @RequestMapping(method = RequestMethod.GET)
    public String showCart(Model model) throws CommerceCartModificationException {
        // Custom logic
        return super.showCart(model);
    }

    @RequestMapping(value = "/express-checkout", method = RequestMethod.POST)
    public String expressCheckout(Model model) {
        // New functionality
        return "redirect:/checkout";
    }
}
```

### JSP Customization
```jsp
<%-- customtag.tag --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="product" required="true" type="de.hybris.platform.commercefacades.product.data.ProductData" %>

<div class="custom-product-card">
    <h3>${product.name}</h3>
    <span class="price">${product.price.formattedValue}</span>
</div>
```

## Checkout Flow Customization

### Spring Web Flow
```xml
<!-- checkout-flow.xml -->
<flow xmlns="http://www.springframework.org/schema/webflow">

    <var name="checkoutForm" class="com.example.forms.CheckoutForm"/>

    <view-state id="deliveryAddress" view="pages/checkout/deliveryAddress">
        <on-entry>
            <evaluate expression="checkoutFacade.getDeliveryAddresses()" result="flowScope.addresses"/>
        </on-entry>
        <transition on="next" to="deliveryMethod"/>
        <transition on="back" to="cart"/>
    </view-state>

    <view-state id="customStep" view="pages/checkout/customStep">
        <transition on="next" to="payment"/>
        <transition on="back" to="deliveryMethod"/>
    </view-state>

    <view-state id="payment" view="pages/checkout/payment">
        <transition on="placeOrder" to="orderConfirmation">
            <evaluate expression="checkoutFacade.placeOrder()" result="flowScope.order"/>
        </transition>
    </view-state>

    <end-state id="orderConfirmation" view="externalRedirect:/checkout/orderConfirmation"/>
</flow>
```

### Custom Checkout Step
```java
@Controller
@RequestMapping("/checkout/custom-step")
public class CustomCheckoutStepController extends AbstractCheckoutStepController {

    @Override
    @RequestMapping(method = RequestMethod.GET)
    public String enterStep(Model model, RedirectAttributes redirectAttributes) {
        // Load step data
        model.addAttribute("customData", customFacade.getCustomData());
        return "pages/checkout/customStep";
    }

    @Override
    @RequestMapping(method = RequestMethod.POST)
    public String doStep(@Valid CustomStepForm form, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return enterStep(model, redirectAttributes);
        }
        customFacade.saveCustomData(form);
        return getCheckoutStep().nextStep();
    }
}
```

## CMS Component Development

### Component Model (items.xml)
```xml
<itemtype code="CustomBannerComponent"
          extends="SimpleBannerComponent"
          autocreate="true" generate="true">
    <attributes>
        <attribute qualifier="subtitle" type="localized:java.lang.String">
            <persistence type="property"/>
        </attribute>
        <attribute qualifier="buttonText" type="localized:java.lang.String">
            <persistence type="property"/>
        </attribute>
    </attributes>
</itemtype>
```

### Component Controller
```java
@Controller
@RequestMapping("/view/CustomBannerComponent")
public class CustomBannerComponentController extends AbstractCMSComponentController<CustomBannerComponentModel> {

    @Override
    protected void fillModel(HttpServletRequest request, Model model,
                             CustomBannerComponentModel component) {
        model.addAttribute("subtitle", component.getSubtitle());
        model.addAttribute("buttonText", component.getButtonText());
        model.addAttribute("urlLink", component.getUrlLink());
    }
}
```

### Component JSP
```jsp
<%-- custombanner.jsp --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="custom-banner">
    <img src="${component.media.url}" alt="${component.media.altText}"/>
    <h2>${subtitle}</h2>
    <a href="${urlLink}" class="btn">${buttonText}</a>
</div>
```

## AddOn Structure

### Directory Layout
```
customaddon/
├── acceleratoraddon/
│   └── web/
│       ├── src/
│       ├── webroot/
│       │   ├── WEB-INF/
│       │   │   ├── views/     # Override JSPs
│       │   │   └── tags/      # Override tags
│       │   └── _ui/
│       │       └── addons/customaddon/
└── resources/
    └── customaddon/
        └── import/
```

### Installing AddOn
```bash
ant addoninstall -Daddonnames="customaddon" -DaddonStorefront.yacceleratorstorefront="yacceleratorstorefront"
```

### AddOn extensioninfo.xml
```xml
<extension name="customaddon" classprefix="Custom">
    <requires-extension name="addonsupport"/>

    <meta key="addon.storefronts" value="yacceleratorstorefront"/>
    <meta key="addon.required-addons" value=""/>
</extension>
```

## Theme Customization

### SASS Structure
```
_ui/responsive/common/
├── css/
├── images/
└── scss/
    ├── _variables.scss
    ├── _mixins.scss
    ├── components/
    │   ├── _buttons.scss
    │   └── _forms.scss
    └── main.scss
```

### Custom Variables
```scss
// _variables.scss
$primary-color: #0066cc;
$secondary-color: #333333;
$font-family-base: 'Open Sans', sans-serif;
$border-radius: 4px;
```

### Build Process
```bash
# Grunt
grunt less

# Or manual SASS
sass --watch _ui/responsive/common/scss:_ui/responsive/common/css
```

## Payment Gateway Integration

### Payment Facade
```java
public interface CustomPaymentFacade {
    PaymentInfoData createPaymentInfo(PaymentForm form);
    AuthorizationResult authorizePayment(String paymentId, BigDecimal amount);
    CaptureResult capturePayment(String authorizationId);
}
```

### Checkout Payment Step
```java
@RequestMapping(value = "/payment", method = RequestMethod.POST)
public String submitPayment(@Valid PaymentForm form, BindingResult result,
                            Model model, RedirectAttributes redirectAttributes) {

    if (result.hasErrors()) {
        return "pages/checkout/payment";
    }

    try {
        PaymentInfoData paymentInfo = paymentFacade.createPaymentInfo(form);
        checkoutFacade.setPaymentDetails(paymentInfo.getId());
        return "redirect:/checkout/review";
    } catch (PaymentException e) {
        model.addAttribute("paymentError", e.getMessage());
        return "pages/checkout/payment";
    }
}
```

## SmartEdit Integration

### Component Configuration
```xml
<!-- Enable SmartEdit for component -->
<bean id="customBannerComponentTypeConfiguration"
      class="de.hybris.platform.cmsfacades.types.service.impl.DefaultCMSComponentTypeStructure">
    <property name="typecode" value="CustomBannerComponent"/>
    <property name="attributes">
        <list>
            <bean class="de.hybris.platform.cmsfacades.types.service.impl.DefaultComponentTypeAttributeStructure">
                <property name="qualifier" value="subtitle"/>
                <property name="localized" value="true"/>
            </bean>
        </list>
    </property>
</bean>
```

### Editable Areas
```jsp
<%-- Mark editable in JSP --%>
<cms:pageSlot position="Section1" var="comp" element="div">
    <cms:component component="${comp}" element="div"/>
</cms:pageSlot>
```
