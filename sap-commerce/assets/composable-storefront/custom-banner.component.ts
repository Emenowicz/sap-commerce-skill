// custom-banner.component.ts
// Example: override the default BannerComponent with a custom Angular component.
// Register it via provideConfig({ cmsComponents: { BannerComponent: { component: CustomBannerComponent } } })

import { Component, OnInit } from '@angular/core';
import { CmsComponent } from '@spartacus/core';
import { CmsComponentData } from '@spartacus/storefront';
import { Observable } from 'rxjs';

interface CustomBannerData extends CmsComponent {
  headline?: string;
  media?: { url: string; altText: string };
  urlLink?: string;
}

@Component({
  selector: 'app-custom-banner',
  template: `
    <ng-container *ngIf="data$ | async as data">
      <a [routerLink]="data.urlLink" class="custom-banner">
        <cx-media [container]="data.media"></cx-media>
        <h2 class="custom-banner__headline">{{ data.headline }}</h2>
      </a>
    </ng-container>
  `,
  styleUrls: ['./custom-banner.component.scss'],
})
export class CustomBannerComponent implements OnInit {
  data$: Observable<CustomBannerData>;

  constructor(public component: CmsComponentData<CustomBannerData>) {}

  ngOnInit(): void {
    this.data$ = this.component.data$;
  }
}
