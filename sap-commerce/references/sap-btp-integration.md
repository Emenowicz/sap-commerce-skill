# SAP BTP Integration

## Table of Contents
- [Overview](#overview)
- [API Registry Module](#api-registry-module)
- [SAP BTP Extensions Integration Module (Kyma)](#sap-btp-extensions-integration-module-kyma)
- [Exposing Commerce Events](#exposing-commerce-events)
- [Exposing OCC APIs to Kyma](#exposing-occ-apis-to-kyma)
- [Consuming External APIs from Commerce](#consuming-external-apis-from-commerce)
- [Kyma Function Example](#kyma-function-example)
- [Webhook Outbound Sync](#webhook-outbound-sync)
- [Integration API (OData)](#integration-api-odata)
- [Required Extensions](#required-extensions)
- [Configuration](#configuration)

## Overview

SAP Business Technology Platform (BTP) integration with SAP Commerce Cloud enables extending commerce functionality **without modifying the core platform**. This follows SAP's "side-by-side extensibility" model.

### Key Integration Points

| Integration | Purpose |
|-------------|---------|
| **API Registry** | Expose Commerce events and OCC APIs to external systems |
| **Kyma Runtime** | Serverless functions / microservices that react to Commerce events |
| **Integration API** | OData-based API for ERP/system integration (S/4HANA, etc.) |
| **Outbound Sync** | Push Commerce data changes to external endpoints (webhooks) |

### Architecture

```
SAP Commerce Cloud
  └── API Registry Module        → Publishes events + APIs to SAP BTP
  └── BTP Extensions Module      → Connects to Kyma runtime

SAP BTP (Kyma Runtime)
  └── Functions / Microservices  → React to Commerce events
  └── Service Bindings           → Consume Commerce OCC APIs
```

## API Registry Module

The API Registry module defines how to expose events and APIs to external target systems.

### Required Extensions
```
apiregistryservices
apiversioning
webhookservices
```

Add to `localextensions.xml` / `manifest.json`.

### Key Models

| Model | Purpose |
|-------|---------|
| `ConsumedDestination` | External system endpoint Commerce calls |
| `ExposedDestination` | Commerce API or event exposed to external systems |
| `DestinationTarget` | Target system (e.g., Kyma runtime) |
| `EventConfiguration` | Maps Commerce events to exposed destinations |

### Configure a Destination Target (via ImpEx)

```impex
INSERT_UPDATE DestinationTarget;id[unique=true];destinationChannel(code)
;kyma-destination-target;DEFAULT

INSERT_UPDATE ExposedOAuthCredential;id[unique=true];oAuthClientDetails(clientId);password
;kyma-oauth-credential;kyma_client;secret

INSERT_UPDATE ExposedDestination;id[unique=true];url;destinationTarget(id);credential(id);active
;kyma-occ-destination;https://your-commerce.com/occ/v2;kyma-destination-target;kyma-oauth-credential;true
```

## SAP BTP Extensions Integration Module (Kyma)

The Kyma integration module connects SAP Commerce to SAP BTP Kyma runtime, enabling:
- Event-driven microservices
- Serverless functions triggered by Commerce events
- Registered service consumption

### Required Extensions
```
kymaintegrationservices
```

### Kyma Connection Setup

1. In the Kyma Dashboard, create an Application and obtain the connection URL
2. In SAP Commerce HAC or via ImpEx, register the Kyma runtime:

```impex
INSERT_UPDATE KymaIntegrationConfig;id[unique=true];applicationId;kymaUrl
;kymaConfig;commerce-application;https://kyma.your-instance.kyma.ondemand.com
```

3. Pair the Commerce application with Kyma using the pairing token from Kyma Dashboard

### Enable Kyma Integration

```properties
# project.properties or local.properties
kymaintegrationservices.kyma.event.retry.count=3
kymaintegrationservices.kyma.event.retry.initial-delay-ms=1000
```

## Exposing Commerce Events

Commerce platform events (order placed, customer registered, etc.) can be exposed via the API Registry and sent to Kyma.

### Built-in Events

| Event | Description |
|-------|-------------|
| `OrderPlacedEvent` | Order submitted by customer |
| `OrderCancelledEvent` | Order cancelled |
| `OrderCompletedEvent` | Order fulfilled |
| `FraudOrderEvent` | Order flagged for fraud |
| `CustomerRegistrationEvent` | New customer registered |
| `CatalogSyncFinishedEvent` | Catalog sync completed |

### Register an Event Configuration (ImpEx)

```impex
INSERT_UPDATE EventConfiguration;eventClass[unique=true];destinationTarget(id)[unique=true];version[unique=true];exportFlag;priority(code);mappingType(code)
;de.hybris.platform.order.events.OrderPlacedEvent;kyma-destination-target;1;true;MEDIUM;GENERIC_ORDER
```

### Custom Event

```java
// 1. Define the event class
public class CustomOrderEvent extends AbstractCommerceEvent<OrderModel> {

    public CustomOrderEvent(final OrderModel order) {
        super(order);
    }

    public OrderModel getOrder() {
        return getSource();
    }
}

// 2. Publish from service
@Resource
private EventService eventService;

public void processCustomAction(OrderModel order) {
    eventService.publishEvent(new CustomOrderEvent(order));
}

// 3. Register in EventConfiguration ImpEx (same as above, with your event class FQN)
```

## Exposing OCC APIs to Kyma

The API Registry can expose the OCC API specification (OpenAPI/Swagger) to Kyma so external microservices can discover and call Commerce APIs:

```impex
INSERT_UPDATE ExposedDestination;id[unique=true];url;destinationTarget(id);active
;occ-api;https://your-commerce.com/occ/v2/swagger.json;kyma-destination-target;true
```

This allows Kyma to:
- Register the OCC API spec
- Enable microservices to call Commerce APIs with proper auth

## Consuming External APIs from Commerce

Commerce can call external systems (payment providers, ERP, etc.) via `ConsumedDestination`:

```impex
INSERT_UPDATE BasicCredential;id[unique=true];username;password
;external-api-credential;apiuser;apipassword

INSERT_UPDATE ConsumedDestination;id[unique=true];url;credential(id);active
;payment-service;https://payment-provider.com/api;external-api-credential;true
```

### Calling the Destination in Java

```java
@Resource
private DestinationService<ConsumedDestination> destinationService;

@Resource
private OutboundServiceFacade outboundServiceFacade;

public void callExternalService(OrderModel order) {
    ConsumedDestination destination = destinationService
        .getDestinationByIdAndByDestinationTargetId("payment-service", "default");

    // Use RestTemplate or WebClient with the destination URL
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<String> response = restTemplate.postForEntity(
        destination.getUrl() + "/process",
        order.getCode(),
        String.class
    );
}
```

## Kyma Function Example

A Kyma serverless function reacting to the `OrderPlacedEvent`:

```javascript
// handler.js (Kyma Function - Node.js)
const axios = require('axios');

module.exports = {
  main: async function(event, context) {
    const orderEvent = event.data;

    console.log(`Order placed: ${orderEvent.orderCode}`);

    // Example: notify external fulfillment system
    await axios.post('https://fulfillment.example.com/orders', {
      orderCode: orderEvent.orderCode,
      customerId: orderEvent.customerId,
      totalPrice: orderEvent.totalPrice,
    });

    return { status: 'processed' };
  }
};
```

### Kyma Function Subscription to Commerce Event

```yaml
# event-subscription.yaml (Kyma)
apiVersion: eventing.kyma-project.io/v1alpha1
kind: Subscription
metadata:
  name: order-placed-subscription
spec:
  sink: http://order-function.default.svc.cluster.local
  filter:
    filters:
      - eventSource:
          property: source
          type: exact
          value: commerce-application
        eventType:
          property: type
          type: exact
          value: sap.cx.commerce.order.placed.v1
```

## Webhook Outbound Sync

Webhook Outbound Sync pushes Commerce data changes to external HTTP endpoints without requiring full Kyma setup:

### Configure Webhook (ImpEx)

```impex
INSERT_UPDATE WebhookConfiguration;eventClass[unique=true];destination(id)[unique=true];active
;de.hybris.platform.order.events.OrderPlacedEvent;my-webhook-destination;true
```

### Webhook Destination

```impex
INSERT_UPDATE ConsumedDestination;id[unique=true];url;active
;my-webhook-destination;https://webhook.example.com/commerce-events;true
```

Webhooks are retried on failure with exponential backoff.

## Integration API (OData)

The Integration API module exposes Commerce data as OData services for S/4HANA, middleware, and other ERP systems.

### Define an Integration Object (ImpEx)

```impex
INSERT_UPDATE IntegrationObject;code[unique=true]
;OrderIntegration

INSERT_UPDATE IntegrationObjectItem;integrationObject(code)[unique=true];code[unique=true];type(code)
;OrderIntegration;Order;Order
;OrderIntegration;OrderEntry;OrderEntry

INSERT_UPDATE IntegrationObjectItemAttribute;integrationObjectItem(integrationObject(code),code)[unique=true];attributeName[unique=true];attributeDescriptor(enclosingType(code),qualifier)
;OrderIntegration:Order;code;Order:code
;OrderIntegration:Order;totalPrice;Order:totalPrice
;OrderIntegration:Order;status;Order:status
;OrderIntegration:OrderEntry;quantity;OrderEntry:quantity
;OrderIntegration:OrderEntry;product;OrderEntry:product
```

### OData Endpoint

```
GET /odata2webservices/OrderIntegration/Orders
GET /odata2webservices/OrderIntegration/Orders('ORDER-001')
POST /odata2webservices/OrderIntegration/Orders
```

### Enable Integration API Extension

```
odata2webservices
integrationservices
integrationbackoffice
```

## Required Extensions

| Use Case | Extensions to Add |
|----------|------------------|
| API Registry + Events | `apiregistryservices`, `apiversioning`, `webhookservices` |
| Kyma Runtime | `kymaintegrationservices` |
| Outbound Sync | `outboundservices`, `outboundsyncservices` |
| OData Integration API | `odata2webservices`, `integrationservices` |

## Configuration

### local.properties (On-Premise) / CCv2 Properties

```properties
# API Registry
apiregistryservices.events.exportEnabled=true

# Kyma
kymaintegrationservices.kyma.event.retry.count=5
kymaintegrationservices.kyma.event.retry.initial-delay-ms=2000

# Outbound services
outboundservices.response.payload.max.length=4096

# Integration API
odata2webservices.page.size.max=200
```

### OAuth Client for Kyma Callback

```impex
INSERT_UPDATE OAuthClientDetails;clientId[unique=true];resourceIds;scope;authorizedGrantTypes;authorities;clientSecret
;kyma_client;hybris;basic;authorization_code,refresh_token,password,client_credentials;ROLE_CLIENT;secret
```
