// custom-feature.module.ts
// Feature module template: registers a custom Angular CMS component override
// and provides its configuration via provideConfig.

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CmsConfig, provideConfig } from '@spartacus/core';
import { MediaModule } from '@spartacus/storefront';

import { CustomBannerComponent } from './custom-banner.component';

@NgModule({
  declarations: [CustomBannerComponent],
  imports: [
    CommonModule,
    RouterModule,
    MediaModule,
  ],
  providers: [
    provideConfig({
      cmsComponents: {
        // Map Commerce CMS type code → Angular component class
        BannerComponent: {
          component: CustomBannerComponent,
        },
        // Add more overrides here as needed
      },
    } as CmsConfig),
  ],
})
export class CustomFeatureModule {}
