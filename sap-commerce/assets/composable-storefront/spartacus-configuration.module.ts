// spartacus-configuration.module.ts
// Root configuration module for Composable Storefront.
// Adjust baseUrl, baseSite, currency and language for your environment.

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
          // prefix: '/occ/v2/'  // default
        },
      },
      context: {
        urlParameters: ['baseSite', 'language', 'currency'],
        baseSite: ['electronics-spa'],
        currency: ['USD'],
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
