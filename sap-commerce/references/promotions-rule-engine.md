# Promotions & Rule Engine

## Table of Contents
- [Overview](#overview)
- [Rule Engine Architecture](#rule-engine-architecture)
- [Promotion Types](#promotion-types)
- [Conditions and Actions](#conditions-and-actions)
- [Coupon Management](#coupon-management)
- [Promotion Groups and Priority](#promotion-groups-and-priority)
- [Custom Promotion Actions](#custom-promotion-actions)
- [ImpEx Configuration](#impex-configuration)
- [Testing Promotions](#testing-promotions)

## Overview

SAP Commerce uses a Drools-based rule engine for promotions. Promotion rules define conditions (cart total, product in cart, customer group) and actions (percentage discount, fixed discount, free gift) that are compiled into Drools rules at publish time.

Key components:
- **Rule Engine**: Drools-based evaluation engine
- **Promotion Rules**: Business rules defined in Backoffice or ImpEx
- **Conditions**: Criteria that must be met (e.g., cart total > $100)
- **Actions**: Discounts or benefits applied when conditions are met
- **Source Rules**: High-level rule definitions compiled to Drools
- **Rule Groups**: Exclusive/non-exclusive grouping for conflict resolution

## Rule Engine Architecture

```
Source Rule (Backoffice/ImpEx)
    → Compiler → Drools Rule (KIE module)
    → Rule Engine evaluates at runtime
    → Actions applied to cart/order
```

### Key Models

| Model | Purpose |
|-------|---------|
| `PromotionSourceRule` | The editable promotion rule |
| `DroolsRule` | Compiled Drools rule (auto-generated) |
| `PromotionGroup` | Groups rules for evaluation order |
| `RuleConditionDefinition` | Defines available condition types |
| `RuleActionDefinition` | Defines available action types |
| `AbstractRuleBasedPromotionAction` | Applied promotion result |

### Rule Lifecycle

1. **Create**: Define source rule with conditions and actions
2. **Publish**: Compile to Drools rules and deploy to KIE module
3. **Evaluate**: Rule engine evaluates against cart/order at runtime
4. **Apply**: Matching actions create discounts on the cart

## Promotion Types

### Product Promotions
Applied to individual products or product groups:
- Percentage discount on a product
- Fixed price for a product
- Buy X get Y free
- Bundle discounts

### Order (Cart) Promotions
Applied to the entire cart:
- Percentage off order total
- Fixed discount on order
- Free shipping above threshold
- Tiered discounts (spend more, save more)

### Customer-Specific Promotions
Targeted to customer segments:
- Customer group-based discounts
- First-order discounts
- Loyalty tier benefits

## Conditions and Actions

### Built-in Conditions

| Condition | Description |
|-----------|-------------|
| `y_qualifying_products` | Specific products in cart |
| `y_qualifying_categories` | Products from specific categories |
| `y_cart_total` | Cart total threshold |
| `y_qualifying_group_types` | Customer group membership |
| `y_order_threshold_perfect_partner` | Threshold for partner products |
| `y_qualifying_coupons` | Coupon code applied |

### Built-in Actions

| Action | Description |
|--------|-------------|
| `y_order_percentage_discount` | Percentage off order total |
| `y_order_fixed_discount` | Fixed amount off order |
| `y_order_entry_percentage_discount` | Percentage off specific items |
| `y_order_entry_fixed_discount` | Fixed amount off specific items |
| `y_order_entry_fixed_price` | Set items to a fixed price |
| `y_free_gift` | Add free product to cart |
| `y_partner_order_entry_percentage_discount` | Discount on partner products |
| `y_change_delivery_mode` | Apply free/discounted shipping |

### Condition/Action JSON Structure

Conditions and actions are stored as JSON in the source rule:

```json
{
  "definitionId": "y_cart_total",
  "parameters": {
    "value": {
      "type": "Map(ItemType(Currency),java.math.BigDecimal)",
      "value": { "USD": 100.00, "EUR": 85.00 }
    },
    "operator": {
      "type": "Enum(de.hybris.platform.ruledefinitions.AmountOperator)",
      "value": "GREATER_THAN_OR_EQUAL"
    }
  }
}
```

## Coupon Management

### Single-Code Coupons

One code shared by all customers:

```impex
INSERT_UPDATE SingleCodeCoupon; couponId[unique=true]; name[lang=en]         ; active; maxRedemptionsPerCustomer; maxTotalRedemptions
                              ; SUMMER20             ; Summer 20% Off Coupon ; true  ; 1                       ; 10000
```

### Multi-Code Coupons

Generated unique codes per customer/campaign:

```impex
INSERT_UPDATE MultiCodeCoupon; couponId[unique=true]; name[lang=en]        ; active; codeGenerationConfiguration(name)
                             ; LOYALTY2024          ; Loyalty Reward Codes ; true  ; default-configuration
```

### Coupon Generation

```impex
# Generate codes for a multi-code coupon
INSERT_UPDATE CouponGenerationConfiguration; name[unique=true]      ; prefix; separator; codeLength; couponCharset(code)
                                           ; default-configuration  ; PROMO ; -        ; 8         ; ALPHANUMERIC
```

### Linking Coupons to Promotions

Use the `y_qualifying_coupons` condition in the promotion rule:

```json
{
  "definitionId": "y_qualifying_coupons",
  "parameters": {
    "coupons": {
      "type": "List(ItemType(AbstractCoupon))",
      "value": ["SUMMER20"]
    }
  }
}
```

## Promotion Groups and Priority

### Promotion Groups

Rules within the same group interact based on exclusivity settings:

```impex
INSERT_UPDATE PromotionGroup; identifier[unique=true]
                            ; myStorePromoGroup
                            ; defaultPromotionGroup
```

### Rule Priority

Higher priority rules are evaluated first. When using exclusive groups, the first matching rule wins:

```impex
INSERT_UPDATE PromotionSourceRule; code[unique=true]; priority; promotionGroup(identifier)
                                 ; vipDiscount      ; 500     ; myStorePromoGroup
                                 ; summerSale       ; 100     ; myStorePromoGroup
```

### Exclusivity

- **Exclusive group**: Only one rule fires per group (highest priority wins)
- **Non-exclusive group**: All matching rules fire (discounts stack)

## Custom Promotion Actions

### Custom Rule Action Definition

Register a new action type:

```impex
INSERT_UPDATE RuleActionDefinition; id[unique=true]           ; name[lang=en]         ; priority; breadcrumb[lang=en]; translatorId                ; translatorParameters; categories(id)
                                  ; y_custom_loyalty_points   ; Award Loyalty Points  ; 500     ; Award {points} loyalty points ; ruleExecutableActionTranslator ; actionId->customLoyaltyPointsAction ; cart
```

### Custom Action Implementation

```java
package com.example.promotions.actions;

import de.hybris.platform.ruleengineservices.rao.RuleEngineResultRAO;
import de.hybris.platform.ruleengineservices.rule.evaluation.RuleActionContext;
import de.hybris.platform.ruleengineservices.rule.evaluation.actions.AbstractRuleExecutableSupport;
import de.hybris.platform.ruleengineservices.rule.evaluation.actions.RAOAction;

import java.math.BigDecimal;

public class CustomLoyaltyPointsAction extends AbstractRuleExecutableSupport implements RAOAction {

    @Override
    public boolean performActionInternal(final RuleActionContext context) {
        final BigDecimal points = context.getParameter("points", BigDecimal.class);

        // Create a custom RAO (Rule Action Object) for loyalty points
        final LoyaltyPointsRAO loyaltyRAO = new LoyaltyPointsRAO();
        loyaltyRAO.setPoints(points.intValue());

        final RuleEngineResultRAO result = context.getRuleEngineResultRAO();
        result.getActions().add(loyaltyRAO);

        context.scheduleForUpdate(result);
        context.insertFacts(loyaltyRAO);

        return true;
    }
}
```

### Spring Registration

```xml
<bean id="customLoyaltyPointsAction"
      class="com.example.promotions.actions.CustomLoyaltyPointsAction"
      parent="abstractRuleExecutableSupport"/>
```

## ImpEx Configuration

### Complete Promotion Setup

```impex
# -----------------------------------------------
# Promotion Group
# -----------------------------------------------
INSERT_UPDATE PromotionGroup; identifier[unique=true]
                            ; myStorePromoGroup

# -----------------------------------------------
# Promotion Source Rules
# -----------------------------------------------

# 10% off orders over $100
INSERT_UPDATE PromotionSourceRule; code[unique=true]      ; name[lang=en]           ; priority; stackable; status(code); website(identifier); promotionGroup(identifier)
                                 ; order10percentOff      ; 10% Off Orders Over $100; 100     ; false    ; PUBLISHED   ; myStore            ; myStorePromoGroup

INSERT_UPDATE PromotionSourceRuleTemplate; code[unique=true]; conditions; actions
                                         ; order10percentOff; [{"definitionId":"y_cart_total","parameters":{"value":{"type":"Map(ItemType(Currency),java.math.BigDecimal)","value":{"USD":100}},"operator":{"type":"Enum(de.hybris.platform.ruledefinitions.AmountOperator)","value":"GREATER_THAN_OR_EQUAL"}}}]; [{"definitionId":"y_order_percentage_discount","parameters":{"value":{"type":"java.math.BigDecimal","value":10}}}]

# Coupon-based: 20% off with code SUMMER20
INSERT_UPDATE PromotionSourceRule; code[unique=true]; name[lang=en]               ; priority; stackable; status(code); website(identifier); promotionGroup(identifier)
                                 ; summer20coupon   ; Summer 20% Off with Coupon  ; 200     ; false    ; PUBLISHED   ; myStore            ; myStorePromoGroup

# Free shipping over $50
INSERT_UPDATE PromotionSourceRule; code[unique=true]; name[lang=en]                ; priority; stackable; status(code); website(identifier); promotionGroup(identifier)
                                 ; freeShipping50   ; Free Shipping Over $50       ; 50      ; true     ; PUBLISHED   ; myStore            ; myStorePromoGroup
```

## Testing Promotions

### Backoffice Testing

1. Navigate to **Marketing > Promotion Rules**
2. Create or select a rule
3. Use "Test Rule" to simulate with sample cart data
4. Check "Rule Logs" for evaluation details

### Programmatic Testing

```java
@Resource
private PromotionEngineService promotionEngineService;

@Test
public void testOrderPercentageDiscount() {
    // Set up cart with total > $100
    final CartModel cart = createCartWithTotal(BigDecimal.valueOf(150));

    // Evaluate promotions
    final PromotionOrderResults results = promotionEngineService.updatePromotions(
            Collections.singleton(promotionGroup), cart);

    // Verify discount applied
    assertThat(results.getAppliedOrderPromotions()).hasSize(1);
    assertThat(cart.getTotalDiscounts()).isEqualTo(BigDecimal.valueOf(15.00));
}
```

### Common Testing Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Promotion not firing | Rule not published | Publish the rule module in Backoffice |
| Wrong discount amount | Currency mismatch | Ensure condition values match cart currency |
| Coupon not accepted | Coupon inactive or max redemptions reached | Check coupon status and limits |
| Multiple discounts stacking | Rules in non-exclusive group | Move to exclusive group or adjust priority |
