import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';   // ← needed
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { SecuritySettingsComponent } from './components/security-settings/security-settings.component';
import { RecoveryComponent } from './components/recovery/recovery.component';
import { PinVerificationComponent } from './components/security-settings/pin-verification.component';
import { ForgotPinComponent } from './components/recovery/forgot-pin.component';
import { FeatureNavComponent } from './components/feature-nav/feature-nav.component'; // ← import it
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    SecuritySettingsComponent,
    RecoveryComponent,
    PinVerificationComponent,
    ForgotPinComponent,
    FeatureNavComponent          // ← declare it here
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,          // ← makes [formGroup] available
    FormsModule,
    HttpClientModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }