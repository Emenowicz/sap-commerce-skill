# Composable Storefront (Spartacus)

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites and Setup](#prerequisites-and-setup)
- [Backend Configuration (OCC + CORS)](#backend-configuration-occ--cors)
- [Scaffolding a New App](#scaffolding-a-new-app)
- [Configuration Module](#configuration-module)
- [Feature Libraries](#feature-libraries)
- [Customizing CMS Components](#customizing-cms-components)
- [Overriding Services](#overriding-services)
- [Custom Angular Components](#custom-angular-components)
- [B2B Composable Storefront](#b2b-composable-storefront)
- [Server-Side Rendering (SSR)](#server-side-rendering-ssr)
- [PWA Support](#pwa-support)
- [Routing](#routing)
- [i18n and Localization](#i18n-and-localization)
- [Upgrading](#upgrading)
- [Headless Deployment](#headless-deployment)

## Overview

**Composable Storefront** (formerly **Spartacus**) is the modern, Angular-based storefront for SAP Commerce Cloud. It replaces the legacy JSP/Spring MVC Accelerator approach and communicates with the SAP Commerce backend exclusively via **OCC REST APIs**.

Key benefits over the JSP Accelerator:
- Decoupled frontend (can be hosted on CDN, separate infrastructure)
- Progressive Web App (PWA) and Server-Side Rendering (SSR) support
- Angular ecosystem (Angular CLI, component libraries, RxJS)
- Easier frontend team collaboration (no Java knowledge needed)
- Supports headless commerce — any frontend can consume OCC APIs

**Version alignment**: Composable Storefront releases track SAP Commerce versions. Use `@spartacus/*@latest` for the latest 2211.x release.

## Architecture

```
Angular SPA (Composable Storefront)
        ↓ HTTP (OCC REST APIs)
SAP Commerce Cloud Backend
        └── commercewebservices (OCC)
        └── cms / CMS content
        └── auth server (OAuth2)
```

### Key Libraries

| Package | Purpose |
|---------|---------|
| `@spartacus/core` | Core services, OCC adapters, auth, routing |
| `@spartacus/storefront` | Base UI components (cart, checkout, product) |
| `@spartacus/assets` | Default translations and icons |
| `@spartacus/styles` | Default SCSS styles |
| `@spartacus/b2b` | B2B features (org management, approval) |
| `@spartacus/asm` | Assisted Service Module (agent support) |
| `@spartacus/cds` | Context-driven services (personalization) |

## Prerequisites and Setup

### SAP Commerce Backend
1. Enable `commercewebservices` extension in `localextensions.xml` / `manifest.json`
2. Import Spartacus-compatible site data (OCC-enabled base site):
   ```impex
   INSERT_UPDATE BaseStore;uid[unique=true];name;currencies(isocode);defaultCurrency(isocode);languages(isocode);defaultLanguage(isocode)
   ;electronics;Electronics Store;USD,GBP;USD;en;en

   INSERT_UPDATE CMSSite;uid[unique=true];stores(uid);channel(code);defaultPreviewCatalogVersion(catalog(id),version);urlPatterns
   ;electronics-spa;electronics;B2C;electronicsContentCatalog:Staged;(?i)^https?://[^/]+(/[^?]*)?\?(.*)$,(?i)^https?://[^/]+(/.*)$
   ```
3. Configure CORS (see [Backend Configuration](#backend-configuration-occ--cors))

### Frontend Requirements
- Node.js 18+
- Angular CLI 17+
- Access to SAP Commerce OCC endpoint

### Registry (SAP RBSC)
For production / licensed builds, point npm to the SAP repository:
```bash
# .npmrc
registry=https://73554900100900004337.npmsrv.cdn.repositories.cloud.sap
```

For open-source builds, use the public npm registry (packages are also published to npmjs.com).

## Backend Configuration (OCC + CORS)

### CORS Properties
Composable Storefront requires proper CORS configuration on the SAP Commerce backend. Add to `local.properties` (development) or CCv2 environment properties:

```properties
# Allow Composable Storefront origin
corsfilter.commercewebservices.allowedOrigins=http://localhost:4200 https://yourstorefront.com
corsfilter.commercewebservices.allowedMethods=GET HEAD OPTIONS PATCH PUT POST DELETE
corsfilter.commercewebservices.allowedHeaders=origin content-type accept authorization cache-control if-none-match x-anonymous-consents x-profile-tag-debug x-consent-reference occ-personalization-id occ-personalization-time

# Media CORS
corsfilter.mediacontroller.allowedOrigins=*
corsfilter.mediacontroller.allowedMethods=GET HEAD OPTIONS
```

### OCC User Endpoint
Ensure the user (guest/customer) endpoint is enabled:
```properties
# Allow anonymous cart access
sap.oauth2.anonymous.token.enabled=true
```

### OAuth2 Client for Spartacus
```impex
INSERT_UPDATE OAuthClientDetails;clientId[unique=true];resourceIds;scope;authorizedGrantTypes;authorities;clientSecret;registeredRedirectUri
;mobile_android;hybris;basic;authorization_code,refresh_token,password,client_credentials;ROLE_CLIENT;secret;http://localhost:4200/
```

## Scaffolding a New App

```bash
# Create new Angular app
ng new my-storefront --style=scss
cd my-storefront

# Add Spartacus schematics (recommended approach)
ng add @spartacus/schematics@latest \
  --base-url https://your-commerce-backend.com \
  --base-site=electronics-spa \
  --ssr

# The schematic adds SpartacusModule, routing, styles, and default config
```

### Manual Setup (without schematics)
```bash
npm install @spartacus/core @spartacus/storefront @spartacus/assets @spartacus/styles
```

## Configuration Module

The `SpartacusConfigurationModule` holds all global configuration:

```typescript
// spartacus-configuration.module.ts
import { NgModule } from '@angular/core';
import { provideConfig } from '@spartacus/core';
import { layoutConfig, mediaConfig } from '@spartacus/storefront';

@NgModule({
  providers: [
    provideConfig(layoutConfig),
    provideConfig(mediaConfig),
    provideConfig({
      backend: {
        occ: {
          baseUrl: 'https://your-commerce-backend.com',
          // prefix defaults to '/occ/v2/'
        },
      },
      context: {
        urlParameters: ['baseSite', 'language', 'currency'],
        baseSite: ['electronics-spa'],
        currency: ['USD', 'GBP'],
        language: ['en'],
      },
      pwa: {
        enabled: true,
        addToHomeScreen: true,
      },
    }),
  ],
})
export class SpartacusConfigurationModule {}
```

### Root SpartacusModule

```typescript
// spartacus.module.ts
import { NgModule } from '@angular/core';
import { BaseStorefrontModule } from '@spartacus/storefront';
import { SpartacusConfigurationModule } from './spartacus-configuration.module';
import { SpartacusFeaturesModule } from './spartacus-features.module';

@NgModule({
  imports: [
    BaseStorefrontModule,
    SpartacusFeaturesModule,
    SpartacusConfigurationModule,
  ],
  exports: [BaseStorefrontModule],
})
export class SpartacusModule {}
```

## Feature Libraries

Features are lazy-loaded Angular modules. Enable in `SpartacusFeaturesModule`:

```typescript
// spartacus-features.module.ts
import { NgModule } from '@angular/core';
import { CmsConfig, ConfigModule, I18nConfig, provideConfig } from '@spartacus/core';
import {
  ProductDetailsPageModule,
  ProductListingPageModule,
} from '@spartacus/storefront';

// Import lazy feature modules
import { UserAccountFeatureModule } from '@spartacus/user/account';
import { CartBaseFeatureModule } from '@spartacus/cart/base';
import { CheckoutFeatureModule } from '@spartacus/checkout/base';
import { OrderFeatureModule } from '@spartacus/order';

@NgModule({
  imports: [
    ProductDetailsPageModule,
    ProductListingPageModule,
    UserAccountFeatureModule,
    CartBaseFeatureModule,
    CheckoutFeatureModule,
    OrderFeatureModule,
  ],
})
export class SpartacusFeaturesModule {}
```

## Customizing CMS Components

CMS components are driven by the backend CMS. Override any component by mapping your Angular component to a CMS component type:

### Override an Existing Component

```typescript
// my-banner.module.ts
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { provideConfig, CmsConfig } from '@spartacus/core';
import { MediaModule } from '@spartacus/storefront';
import { MyBannerComponent } from './my-banner.component';

@NgModule({
  declarations: [MyBannerComponent],
  imports: [CommonModule, RouterModule, MediaModule],
  providers: [
    provideConfig({
      cmsComponents: {
        // Map CMS type → Angular component
        BannerComponent: {
          component: MyBannerComponent,
        },
      },
    } as CmsConfig),
  ],
})
export class MyBannerModule {}
```

```typescript
// my-banner.component.ts
import { Component, OnInit } from '@angular/core';
import { CmsComponent } from '@spartacus/core';
import { CmsComponentData } from '@spartacus/storefront';
import { Observable } from 'rxjs';

interface MyBannerCmsData extends CmsComponent {
  headline?: string;
  media?: { url: string; altText: string };
  urlLink?: string;
}

@Component({
  selector: 'app-my-banner',
  templateUrl: './my-banner.component.html',
})
export class MyBannerComponent implements OnInit {
  data$: Observable<MyBannerCmsData>;

  constructor(public component: CmsComponentData<MyBannerCmsData>) {}

  ngOnInit(): void {
    this.data$ = this.component.data$;
  }
}
```

```html
<!-- my-banner.component.html -->
<ng-container *ngIf="data$ | async as data">
  <a [routerLink]="data.urlLink">
    <cx-media [container]="data.media"></cx-media>
    <h2>{{ data.headline }}</h2>
  </a>
</ng-container>
```

### Register CMS Component for B2B Checkout Extension

```typescript
provideConfig({
  cmsComponents: {
    ManageBudgetsListComponent: {
      childRoutes: {
        children: [
          { path: 'custom-create', component: MyBudgetFormComponent },
        ],
      },
    },
  },
});
```

## Overriding Services

Override default services by providing custom implementations:

```typescript
// custom-product.service.ts
import { Injectable } from '@angular/core';
import { ProductService } from '@spartacus/core';

@Injectable({ providedIn: 'root' })
export class CustomProductService extends ProductService {
  // Override specific methods
}
```

```typescript
// In feature module providers:
providers: [
  { provide: ProductService, useClass: CustomProductService },
]
```

### Using ConfigModule for Service Overrides

```typescript
provideConfig({
  cmsComponents: {
    SearchBoxComponent: {
      // Custom service for the component
      providers: [
        { provide: SearchBoxComponentService, useClass: CustomSearchBoxService },
      ],
    },
  },
});
```

## Custom Angular Components

### Creating a Custom Page Component

```typescript
// custom-page.component.ts
import { Component } from '@angular/core';
import { RoutingService, AuthService } from '@spartacus/core';

@Component({
  selector: 'app-custom-page',
  template: `
    <div class="custom-page">
      <h1>Custom Page</h1>
      <ng-container *ngIf="isLoggedIn$ | async">
        <p>Welcome back!</p>
      </ng-container>
    </div>
  `,
})
export class CustomPageComponent {
  isLoggedIn$ = this.authService.isUserLoggedIn();

  constructor(
    private routingService: RoutingService,
    private authService: AuthService
  ) {}
}
```

### Custom Route

```typescript
// app-routing.module.ts
import { RouterModule, Routes } from '@angular/router';
import { CmsPageGuard, PageLayoutComponent } from '@spartacus/storefront';

const routes: Routes = [
  {
    path: 'custom',
    component: PageLayoutComponent,
    canActivate: [CmsPageGuard],
    data: { cxRoute: 'customPage' },
  },
];
```

```typescript
// In configuration:
provideConfig({
  routing: {
    routes: {
      customPage: { paths: ['custom-page'] },
    },
  },
});
```

## B2B Composable Storefront

```bash
ng add @spartacus/schematics@latest --base-url ... --base-site=powertools-spa --ssr
# Then add B2B feature:
ng add @spartacus/b2b
```

```typescript
// B2B features
import { B2bStorefrontModule } from '@spartacus/b2b';

@NgModule({
  imports: [
    B2bStorefrontModule.withConfig({
      // B2B-specific configuration
    }),
  ],
})
export class AppModule {}
```

B2B features include: Organization management, Cost centers, Budgets, Order approvals, Quick order, Saved carts, Scheduled replenishment.

## Server-Side Rendering (SSR)

SSR improves SEO and initial page load. Enabled via Angular Universal:

```bash
ng add @spartacus/schematics@latest --base-url ... --base-site=... --ssr
```

This adds:
- `app.server.module.ts`
- `server.ts` (Express server)
- SSR-aware configuration

### SSR Configuration

```typescript
// In SpartacusConfigurationModule:
provideConfig({
  i18n: {
    backend: {
      loadPath: 'assets/i18n-assets/{{lng}}/{{ns}}.json',
    },
    chunks: defaultCmsContentProviders,
    fallbackLang: 'en',
  },
});
```

### Running SSR Locally

```bash
npm run build:ssr
npm run serve:ssr
```

## PWA Support

```typescript
provideConfig({
  pwa: {
    enabled: true,
    addToHomeScreen: true,
  },
});
```

Add Angular service worker:
```bash
ng add @angular/pwa
```

## Routing

Composable Storefront uses the backend CMS page structure to drive routing via `CmsPageGuard`.

```typescript
// Custom route configuration
provideConfig({
  routing: {
    routes: {
      product: {
        paths: ['product/:productCode/:name', 'product/:productCode'],
        paramsMapping: { productCode: 'code' },
      },
      category: {
        paths: ['category/:categoryCode'],
      },
    },
  },
});
```

## i18n and Localization

```typescript
// Load translation chunks
import { translations, translationChunksConfig } from '@spartacus/assets';

provideConfig({
  i18n: {
    resources: translations,
    chunks: translationChunksConfig,
    fallbackLang: 'en',
  },
});
```

### Custom Translations

```typescript
// Custom translation chunks
provideConfig({
  i18n: {
    resources: {
      en: {
        myFeature: () => import('./i18n/en/my-feature.json'),
      },
    },
  },
});
```

## Upgrading

Composable Storefront follows SAP Commerce version cadence. Use schematics to upgrade:

```bash
ng update @spartacus/schematics@<new-version>
```

Check the [Composable Storefront Release Notes](https://help.sap.com/docs/SAP_COMMERCE_COMPOSABLE_STOREFRONT) for breaking changes before upgrading.

## Headless Deployment

Composable Storefront SPA can be deployed completely independently of the SAP Commerce backend:

```
CDN / Static Hosting  ← Angular SPA build artifacts
        ↓ OCC API calls
SAP Commerce Cloud (CCv2 or on-premise)
```

### Build for Production

```bash
ng build --configuration production
# Output in dist/my-storefront/
# Deploy to CDN (AWS CloudFront, Azure CDN, etc.)
```

### Environment-Specific Base URLs

Use Angular environment files to set OCC base URL per environment:

```typescript
// environments/environment.prod.ts
export const environment = {
  production: true,
  occBaseUrl: 'https://api.my-commerce.com',
};
```

```typescript
// In configuration:
provideConfig({
  backend: {
    occ: {
      baseUrl: environment.occBaseUrl,
    },
  },
});
```
