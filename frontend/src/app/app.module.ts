import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { WalletComponent } from './components/wallet/wallet.component';
import { TransactionsComponent } from './components/transactions/transactions.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { MoneyRequestsComponent } from './components/money-requests/money-requests.component';
import { SecuritySettingsComponent } from './components/security-settings/security-settings.component';
import { ForgotPinComponent } from './components/recovery/forgot-pin.component';
import { PinVerificationComponent } from './components/security-settings/pin-verification.component';
import { RecoveryComponent } from './components/recovery/recovery.component';
import { FeatureNavComponent } from './components/feature-nav/feature-nav.component';
import { PaymentMethodsComponent } from './components/payment-methods/payment-methods.component';
import { BusinessComponent } from './components/business/business.component';
import { BusinessAnalyticsComponent } from './components/business-analytics/business-analytics.component';
import { NotificationsComponent } from './components/notifications/notifications.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    DashboardComponent,
    WalletComponent,
    TransactionsComponent,
    MoneyRequestsComponent,
    SecuritySettingsComponent,
    RecoveryComponent,
    PinVerificationComponent,
    ForgotPinComponent,
    FeatureNavComponent,
    PaymentMethodsComponent,
    BusinessComponent,
    BusinessAnalyticsComponent,
    NotificationsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
