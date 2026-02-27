import { Component, OnInit } from '@angular/core';
import { BusinessAnalyticsResponse } from '../../models/business.model';
import { BusinessService } from '../../services/business.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-business-analytics',
  templateUrl: './business-analytics.component.html',
  styleUrls: ['./business-analytics.component.css']
})
export class BusinessAnalyticsComponent implements OnInit {
  analytics: BusinessAnalyticsResponse | null = null;
  loading = false;
  error = '';

  constructor(
    private authService: AuthService,
    private businessService: BusinessService
  ) { }

  ngOnInit(): void {
    if (this.authService.isBusinessAccount()) {
      this.loadAnalytics();
    } else {
      this.error = 'Business account required to view analytics.';
    }
  }

  loadAnalytics(): void {
    this.loading = true;
    this.businessService.getAnalytics().subscribe({
      next: (response) => {
        this.analytics = response;
        this.error = '';
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load business analytics';
        this.loading = false;
      }
    });
  }
}
