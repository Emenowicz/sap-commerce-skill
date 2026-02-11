# Business Processes

## Table of Contents
- [Overview](#overview)
- [Process Definition XML](#process-definition-xml)
- [Action Beans](#action-beans)
- [Process Lifecycle](#process-lifecycle)
- [Order Process Customization](#order-process-customization)
- [Event-Driven Triggers](#event-driven-triggers)
- [Error Handling and Retry](#error-handling-and-retry)
- [Spring Configuration](#spring-configuration)
- [ImpEx Setup](#impex-setup)

## Overview

Business processes in SAP Commerce define stateful workflows that coordinate multiple actions. They are used for order processing, return/refund flows, approval workflows, and custom multi-step operations.

Key components:
- **Process Definition** (XML): Declares actions, transitions, wait states, and end nodes
- **Action Beans** (Java): Implement each step's logic
- **Process Engine**: Manages lifecycle, state persistence, and event handling
- **BusinessProcessService**: API for starting, triggering, and managing processes

## Process Definition XML

Process definitions are XML files placed in `resources/processes/` within an extension.

### Basic Structure

```xml
<?xml version="1.0" encoding="utf-8"?>
<process xmlns="http://www.hybris.de/xsd/processdefinition"
         name="customOrderProcess"
         start="validateOrder"
         onError="error">

    <!-- Action nodes perform logic and decide transition -->
    <action id="validateOrder" bean="validateOrderAction">
        <transition name="OK" to="checkPayment"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="checkPayment" bean="checkPaymentAction">
        <transition name="OK" to="allocateStock"/>
        <transition name="NOK" to="waitForPayment"/>
    </action>

    <!-- Wait node pauses until an external event triggers it -->
    <wait id="waitForPayment" then="checkPayment" prependProcessCode="true">
        <event>PaymentConfirmed</event>
    </wait>

    <action id="allocateStock" bean="allocateStockAction">
        <transition name="OK" to="sendConfirmation"/>
        <transition name="NOK" to="waitForStock"/>
    </action>

    <wait id="waitForStock" then="allocateStock" prependProcessCode="true">
        <event>StockAvailable</event>
    </wait>

    <action id="sendConfirmation" bean="sendConfirmationEmailAction">
        <transition name="OK" to="success"/>
        <transition name="NOK" to="error"/>
    </action>

    <!-- End nodes -->
    <end id="success" state="SUCCEEDED">Order processed successfully</end>
    <end id="error" state="ERROR">Order processing failed</end>
</process>
```

### Node Types

| Node | Purpose |
|------|---------|
| `<action>` | Executes a Spring bean and follows a transition |
| `<wait>` | Pauses process until an event is received |
| `<end>` | Terminates the process with a state |
| `<notify>` | Sends a notification without blocking |
| `<split>` | Forks into parallel branches |
| `<join>` | Waits for all parallel branches to complete |

### Process with Parallel Branches

```xml
<process xmlns="http://www.hybris.de/xsd/processdefinition"
         name="parallelFulfillment"
         start="splitShipments">

    <split id="splitShipments">
        <targetNode name="shipWarehouse1"/>
        <targetNode name="shipWarehouse2"/>
    </split>

    <action id="shipWarehouse1" bean="shipFromWarehouse1Action">
        <transition name="OK" to="joinShipments"/>
    </action>

    <action id="shipWarehouse2" bean="shipFromWarehouse2Action">
        <transition name="OK" to="joinShipments"/>
    </action>

    <join id="joinShipments" then="sendShippedEmail"/>

    <action id="sendShippedEmail" bean="sendShippedEmailAction">
        <transition name="OK" to="success"/>
    </action>

    <end id="success" state="SUCCEEDED">All shipments complete</end>
</process>
```

## Action Beans

### AbstractSimpleDecisionAction

For actions with binary outcomes (OK/NOK):

```java
package com.example.actions;

import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateOrderAction extends AbstractSimpleDecisionAction<OrderProcessModel> {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateOrderAction.class);

    @Override
    public Transition executeAction(final OrderProcessModel process) {
        LOG.info("Validating order: {}", process.getOrder().getCode());

        if (isOrderValid(process.getOrder())) {
            return Transition.OK;
        }
        return Transition.NOK;
    }

    private boolean isOrderValid(final de.hybris.platform.core.model.order.OrderModel order) {
        return order.getEntries() != null && !order.getEntries().isEmpty();
    }
}
```

### AbstractAction (Multiple Transitions)

For actions with more than two outcomes:

```java
package com.example.actions;

import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;

import java.util.Set;

public class CheckInventoryAction extends AbstractAction<OrderProcessModel> {

    private static final String AVAILABLE = "available";
    private static final String PARTIAL = "partial";
    private static final String UNAVAILABLE = "unavailable";

    @Override
    public String execute(final OrderProcessModel process) throws Exception {
        final int availableQty = checkStock(process.getOrder());
        final int requestedQty = getRequestedQuantity(process.getOrder());

        if (availableQty >= requestedQty) {
            return AVAILABLE;
        } else if (availableQty > 0) {
            return PARTIAL;
        }
        return UNAVAILABLE;
    }

    @Override
    public Set<String> getTransitions() {
        return Set.of(AVAILABLE, PARTIAL, UNAVAILABLE);
    }
}
```

The corresponding process XML would have three transitions:
```xml
<action id="checkInventory" bean="checkInventoryAction">
    <transition name="available" to="allocateStock"/>
    <transition name="partial" to="handlePartialStock"/>
    <transition name="unavailable" to="notifyOutOfStock"/>
</action>
```

### AbstractProceduralAction

For actions with no decision (always proceeds to the next step):

```java
package com.example.actions;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;

public class UpdateOrderStatusAction extends AbstractProceduralAction<OrderProcessModel> {

    @Override
    public void executeAction(final OrderProcessModel process) throws Exception {
        // Use a standard OrderStatus value (e.g., CHECKED_VALID, CREATED)
        // or define a custom value like PROCESSING in your items.xml enumtypes
        process.getOrder().setStatus(OrderStatus.CHECKED_VALID);
        modelService.save(process.getOrder());
    }
}
```

## Process Lifecycle

### Starting a Process

```java
@Resource
private BusinessProcessService businessProcessService;

@Resource
private ModelService modelService;

public void startOrderProcess(final OrderModel order) {
    final String processCode = "order-process-" + order.getCode() + "-" + System.currentTimeMillis();

    final OrderProcessModel process = businessProcessService.createProcess(
            processCode, "customOrderProcess");
    process.setOrder(order);
    modelService.save(process);

    businessProcessService.startProcess(process);
}
```

### Triggering Events (Resume from Wait)

```java
// Resume a process waiting for a "PaymentConfirmed" event
businessProcessService.triggerEvent("order-process-00001-1234567890_PaymentConfirmed");
```

When `prependProcessCode="true"` is set on a wait node, the event name is prefixed with the process code and an underscore.

### Restarting a Failed Process

```java
final BusinessProcessModel process = businessProcessService.getProcess("order-process-00001-1234567890");
businessProcessService.restartProcess(process, "validateOrder");
```

## Order Process Customization

The default order process (`order-process.xml` in `basecommerce`) can be customized:

### Override Process Definition

1. Copy the original process definition to your extension's `resources/processes/`
2. Modify the XML (add/remove/reorder actions)
3. Register via ImpEx to replace the default

### Add a Custom Step

```xml
<!-- Insert after payment check, before fulfillment -->
<action id="fraudCheck" bean="fraudCheckAction">
    <transition name="OK" to="allocateStock"/>
    <transition name="NOK" to="cancelOrder"/>
    <transition name="REVIEW" to="waitForManualReview"/>
</action>

<wait id="waitForManualReview" then="fraudCheck" prependProcessCode="true">
    <event>FraudReviewComplete</event>
</wait>
```

### Return/Refund Process

```xml
<process xmlns="http://www.hybris.de/xsd/processdefinition"
         name="returnProcess"
         start="validateReturn"
         onError="error">

    <action id="validateReturn" bean="validateReturnAction">
        <transition name="OK" to="approveReturn"/>
        <transition name="NOK" to="rejectReturn"/>
    </action>

    <action id="approveReturn" bean="approveReturnAction">
        <transition name="OK" to="processRefund"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="processRefund" bean="processRefundAction">
        <transition name="OK" to="sendRefundConfirmation"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="sendRefundConfirmation" bean="sendRefundConfirmationAction">
        <transition name="OK" to="success"/>
    </action>

    <action id="rejectReturn" bean="rejectReturnAction">
        <transition name="OK" to="sendRejectionEmail"/>
    </action>

    <action id="sendRejectionEmail" bean="sendRejectionEmailAction">
        <transition name="OK" to="rejected"/>
    </action>

    <end id="success" state="SUCCEEDED">Return processed and refund issued</end>
    <end id="rejected" state="SUCCEEDED">Return rejected</end>
    <end id="error" state="ERROR">Return processing failed</end>
</process>
```

## Event-Driven Triggers

### Publishing Events to Trigger Processes

```java
@Resource
private EventService eventService;

public void onPaymentReceived(final OrderModel order) {
    final OrderPaymentEvent event = new OrderPaymentEvent();
    event.setOrderCode(order.getCode());
    eventService.publishEvent(event);
}
```

### Event Listener to Start a Process

```java
public class OrderPaymentEventListener extends AbstractEventListener<OrderPaymentEvent> {

    @Resource
    private BusinessProcessService businessProcessService;

    @Override
    protected void onEvent(final OrderPaymentEvent event) {
        final String processCode = "payment-" + event.getOrderCode();
        businessProcessService.triggerEvent(processCode + "_PaymentConfirmed");
    }
}
```

## Error Handling and Retry

### Process-Level Error Handling

Use `onError` attribute to define a fallback end state:

```xml
<process name="myProcess" start="step1" onError="errorEnd">
    ...
    <end id="errorEnd" state="ERROR">Process failed</end>
</process>
```

### Action-Level Error Handling

Actions can catch exceptions and return appropriate transitions:

```java
@Override
public Transition executeAction(final OrderProcessModel process) {
    try {
        externalService.callApi(process.getOrder());
        return Transition.OK;
    } catch (final Exception e) {
        LOG.error("External API call failed for order: {}", process.getOrder().getCode(), e);
        process.getOrder().setStatus(OrderStatus.SUSPENDED);
        modelService.save(process.getOrder());
        return Transition.NOK;
    }
}
```

### Retry with Wait Node

Use a wait node to pause and retry after external dependencies become available:

```xml
<action id="callExternalApi" bean="callExternalApiAction">
    <transition name="OK" to="nextStep"/>
    <transition name="RETRY" to="waitAndRetry"/>
</action>

<wait id="waitAndRetry" then="callExternalApi" prependProcessCode="true">
    <event>RetryApiCall</event>
</wait>
```

## Spring Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Action beans -->
    <bean id="validateOrderAction" class="com.example.actions.ValidateOrderAction"
          parent="abstractAction"/>

    <bean id="checkPaymentAction" class="com.example.actions.CheckPaymentAction"
          parent="abstractAction">
        <property name="paymentService" ref="paymentService"/>
    </bean>

    <bean id="allocateStockAction" class="com.example.actions.AllocateStockAction"
          parent="abstractAction">
        <property name="stockService" ref="stockService"/>
        <property name="warehouseService" ref="warehouseService"/>
    </bean>

    <bean id="sendConfirmationEmailAction" class="com.example.actions.SendConfirmationEmailAction"
          parent="abstractAction">
        <property name="emailService" ref="emailService"/>
    </bean>

    <!-- Process definition registration -->
    <bean id="customOrderProcessDefinitionResource"
          class="de.hybris.platform.processengine.definition.ProcessDefinitionResource">
        <property name="resource" value="classpath:/processes/custom-order-process.xml"/>
    </bean>

</beans>
```

## ImpEx Setup

### Register Process Definition

Process definitions can also be registered via ImpEx if not using Spring:

```impex
# Register process definition
INSERT_UPDATE ProcessDefinition; code[unique=true]     ; resource
                               ; customOrderProcess    ; jar:com.example.constants.MyExtensionConstants&/myextension/processes/custom-order-process.xml
```

### Start a Process via ImpEx (for testing)

```impex
# Create and start a process instance (useful for testing)
INSERT_UPDATE OrderProcess; code[unique=true]              ; processDefinitionName; order(code)
                          ; testOrderProcess-001           ; customOrderProcess   ; testOrder001
```
