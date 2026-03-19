# CCv2 Cloud Deployment

## Table of Contents
- [Overview](#overview)
- [manifest.json Reference](#manifestjson-reference)
- [Aspects](#aspects)
- [Extensions Configuration](#extensions-configuration)
- [Properties Configuration](#properties-configuration)
- [Solr Configuration](#solr-configuration)
- [Tests Configuration](#tests-configuration)
- [Build Process](#build-process)
- [Environment Management](#environment-management)
- [Deployment Pipeline](#deployment-pipeline)
- [Logging and Monitoring](#logging-and-monitoring)
- [Performance Tuning for CCv2](#performance-tuning-for-ccv2)
- [Common Issues](#common-issues)

## Overview

**SAP Commerce Cloud v2 (CCv2)** is the managed public cloud offering for SAP Commerce. The build and deployment lifecycle is code-driven via a `manifest.json` file at the root of your repository.

### Key Differences from On-Premise
- No direct server access — everything configured via manifest.json and Cloud Portal
- Infrastructure managed by SAP (servers, DB, Solr, scaling)
- Build triggered by git push; deploy promoted through environments
- Configuration split across aspects (storefront / api / backoffice)
- No `localextensions.xml` — extensions listed in manifest.json

### Supported Versions (2024)
- 2211 (LTS — recommended)
- 2105 (previous LTS)

## manifest.json Reference

The `manifest.json` file lives at the repository root and controls the entire build:

```json
{
  "commerceSuiteVersion": "2211",

  "useConfig": {
    "extensions": {
      "location": "hybris/config/localextensions.xml",
      "exclusive": false
    },
    "properties": [
      {
        "location": "hybris/config/common.properties",
        "aspect": "common"
      },
      {
        "location": "hybris/config/api.properties",
        "aspect": "api"
      },
      {
        "location": "hybris/config/backoffice.properties",
        "aspect": "backoffice"
      },
      {
        "location": "hybris/config/storefront.properties",
        "aspect": "storefront"
      }
    ],
    "solr": {
      "location": "hybris/config/solr"
    }
  },

  "extensions": [
    "commercewebservices",
    "acceleratorwebservices",
    "oauth2",
    "backoffice",
    "yacceleratorstorefront",
    "myextension",
    "myextension2"
  ],

  "tests": {
    "extensions": [
      "myextension",
      "myextension2"
    ],
    "annotations": [
      "UnitTest",
      "IntegrationTest"
    ],
    "packages": [
      "de.hybris.bootstrap.annotations"
    ]
  },

  "aspects": [
    {
      "name": "backoffice",
      "properties": [
        {
          "key": "backoffice.fill.typefacade.cache.maxEntries",
          "value": "2000"
        },
        {
          "key": "backoffice.cockpitng.additionalResourceLoader.enabled",
          "value": "true"
        }
      ],
      "webapps": [
        {
          "name": "hac",
          "contextPath": "/hac"
        },
        {
          "name": "backoffice",
          "contextPath": "/backoffice"
        }
      ]
    },
    {
      "name": "api",
      "properties": [
        {
          "key": "occ.rewrite.overlapping.paths.enabled",
          "value": "true"
        },
        {
          "key": "corsfilter.commercewebservices.allowedOrigins",
          "value": "https://yourstorefront.com"
        },
        {
          "key": "corsfilter.commercewebservices.allowedMethods",
          "value": "GET HEAD OPTIONS PATCH PUT POST DELETE"
        },
        {
          "key": "corsfilter.commercewebservices.allowedHeaders",
          "value": "origin content-type accept authorization cache-control if-none-match x-anonymous-consents x-profile-tag-debug x-consent-reference occ-personalization-id occ-personalization-time"
        }
      ],
      "webapps": [
        {
          "name": "commercewebservices",
          "contextPath": "/occ"
        },
        {
          "name": "acceleratorwebservices",
          "contextPath": "/acceleratorwebservices"
        },
        {
          "name": "oauth2",
          "contextPath": "/authorizationserver"
        }
      ]
    },
    {
      "name": "storefront",
      "properties": [
        {
          "key": "spring.session.enabled",
          "value": "true"
        }
      ],
      "webapps": [
        {
          "name": "yacceleratorstorefront",
          "contextPath": ""
        }
      ]
    }
  ]
}
```

## Aspects

Aspects define separate runtime pods with different configurations:

| Aspect | Purpose | Typical Webapps |
|--------|---------|-----------------|
| `backoffice` | Admin UI, HAC | `backoffice`, `hac` |
| `api` | OCC REST API (for Composable Storefront / headless) | `commercewebservices`, `oauth2` |
| `storefront` | JSP Accelerator storefront | `yacceleratorstorefront` |
| `backgroundProcessing` | CronJobs, async jobs | No webapps |
| `accstorefront` | Combined accelerator storefront | Multiple |

### Minimal Headless Setup (No JSP Storefront)

For projects using Composable Storefront or a custom frontend:

```json
{
  "aspects": [
    {
      "name": "backoffice",
      "webapps": [
        { "name": "hac", "contextPath": "/hac" },
        { "name": "backoffice", "contextPath": "/backoffice" }
      ]
    },
    {
      "name": "api",
      "webapps": [
        { "name": "commercewebservices", "contextPath": "/occ" },
        { "name": "oauth2", "contextPath": "/authorizationserver" }
      ]
    }
  ]
}
```

## Extensions Configuration

### Inline Extensions List
```json
{
  "extensions": [
    "commercewebservices",
    "backoffice",
    "myextension"
  ]
}
```

### External localextensions.xml Reference
```json
{
  "useConfig": {
    "extensions": {
      "location": "hybris/config/localextensions.xml",
      "exclusive": false
    }
  }
}
```

> **Note:** If `exclusive` is `true`, only extensions listed in your file are loaded (no platform defaults). Use `false` to merge with platform defaults.

## Properties Configuration

### Common Properties (`hybris/config/common.properties`)
Properties shared across all aspects:
```properties
# Catalog versioning
catalog.sync.workers=4
sync.full.transactions.batchsize=100

# Media
media.default.context=localhost

# Session
storefront.sessiontoken.cookie.secure=true
```

### Aspect-Specific Properties (`hybris/config/api.properties`)
```properties
# OCC
commercewebservices.swagger.enabled=false

# CORS for Composable Storefront
corsfilter.commercewebservices.allowedOrigins=https://yourstorefront.com
corsfilter.commercewebservices.allowedMethods=GET HEAD OPTIONS PATCH PUT POST DELETE
corsfilter.commercewebservices.allowedHeaders=origin content-type accept authorization cache-control if-none-match x-anonymous-consents x-profile-tag-debug x-consent-reference occ-personalization-id occ-personalization-time

# Auth
sap.oauth2.anonymous.token.enabled=true
```

### Inline Properties in manifest.json
```json
{
  "aspects": [
    {
      "name": "api",
      "properties": [
        {
          "key": "corsfilter.commercewebservices.allowedOrigins",
          "value": "https://yourstorefront.com"
        }
      ]
    }
  ]
}
```

> **Priority**: Inline manifest.json properties → aspect-specific property files → common property files.

## Solr Configuration

Custom Solr configuration is referenced via:
```json
{
  "useConfig": {
    "solr": {
      "location": "hybris/config/solr"
    }
  }
}
```

Place custom Solr configs under `hybris/config/solr/`. The directory structure mirrors the Solr `configsets` format.

## Tests Configuration

Configure which tests run during CCv2 build:

```json
{
  "tests": {
    "extensions": [
      "myextension"
    ],
    "annotations": [
      "UnitTest",
      "IntegrationTest"
    ],
    "packages": [
      "de.hybris.bootstrap.annotations"
    ],
    "excludedGroups": [
      "SomeSlowTestGroup"
    ]
  }
}
```

> **Note:** CCv2 runs tests automatically on every build. Integration tests require a running database and are slower — ensure your tests are annotated correctly with `@UnitTest` or `@IntegrationTest`.

## Build Process

### Build Trigger
A build is triggered automatically when you push to the connected repository branch.

### Build Artifacts
After a successful build, CCv2 creates a build artifact (Docker image) that can be deployed to any environment.

### Build Configuration
```bash
# Validate manifest.json locally before push (optional tool)
npx @spartacus/schematics validate-manifest

# Ensure extensions compile locally
ant clean all
ant unittests  # run unit tests locally before push
```

## Environment Management

CCv2 environments: **Development**, **Staging**, **Production**

### Deployment Promotion Flow
```
Build → Deploy to Development → Test → Promote to Staging → Test → Promote to Production
```

### Environment-Specific Configuration
Use the CCv2 Cloud Portal to set environment-specific properties:
1. Navigate to Cloud Portal > Environments > \<Environment\> > Properties
2. Add key-value pairs for environment-specific overrides
3. Properties take effect on next deployment

### Initializing vs Updating
- **Initialize**: Drops and recreates the database. Use only for new environments.
- **Update**: Safe for production — applies type system changes without data loss.

```json
{
  "deployments": {
    "dbUpdateMode": "UPDATE"
  }
}
```

## Deployment Pipeline

### Recommended CI/CD Pipeline

```yaml
# Example: GitHub Actions pipeline
name: SAP Commerce CCv2

on:
  push:
    branches: [main, release/*]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate manifest.json
        run: |
          python3 -c "import json; json.load(open('manifest.json'))"
      - name: Trigger CCv2 Build
        run: |
          # Use SAP Commerce Cloud API to trigger build
          curl -X POST "https://api.commerce.ondemand.com/v2/subscriptions/$SUBSCRIPTION_ID/builds" \
            -H "Authorization: Bearer $CCv2_API_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"branch": "main", "name": "Build from CI"}'
```

## Logging and Monitoring

### Log Access
Logs are accessible via CCv2 Cloud Portal > Environments > Logs, or via the CCv2 CLI:

```bash
# CCv2 CLI
ccv2 logs --environment dev --aspect api --tail
```

### Log Levels via Properties
```properties
# Reduce noise in production
log4j2.logger.hybris.name=de.hybris.platform
log4j2.logger.hybris.level=WARN

# Debug a specific extension
log4j2.logger.myext.name=com.example.myextension
log4j2.logger.myext.level=DEBUG
```

### Application Performance Monitoring
CCv2 integrates with Dynatrace for APM. Enable via Cloud Portal > Environments > Application Performance Monitoring.

## Performance Tuning for CCv2

### JVM Settings (via manifest.json)
```json
{
  "aspects": [
    {
      "name": "api",
      "properties": [
        {
          "key": "tomcat.generaloptions",
          "value": "-Xmx6g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
        }
      ]
    }
  ]
}
```

### Caching Configuration
```properties
# Entity cache
cache.region.entity.size=100000

# Query cache
cache.region.query.size=20000

# Cluster cache invalidation
cluster.cache.invalidation.enabled=true
```

### Connection Pool
```properties
db.pool.maxActive=100
db.pool.maxIdle=50
db.pool.minIdle=10
db.pool.maxWait=30000
```

## Common Issues

### Build Fails: Extension Not Found
- Ensure extension name in `manifest.json` matches exactly
- Check the extension compiles locally: `ant clean all`

### Deployment Fails: Type System Error
- Run `ant typecodecheck` locally
- Check for duplicate typecodes across extensions

### CORS Errors from Composable Storefront
- Verify `corsfilter.commercewebservices.allowedOrigins` includes the storefront URL
- Check `allowedHeaders` includes `x-anonymous-consents` and authorization headers
- Ensure the CORS filter is on the `api` aspect

### Properties Not Taking Effect
- Properties set in Cloud Portal override file-based properties
- After property changes, redeploy the affected aspect
- Check property key spelling — typos silently fail

### OCC Returns 401 for Anonymous Access
- Enable anonymous token: `sap.oauth2.anonymous.token.enabled=true`
- Verify OAuth client is configured with correct grants in ImpEx
