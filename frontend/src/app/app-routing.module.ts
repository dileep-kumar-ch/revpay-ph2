import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { WalletComponent } from './components/wallet/wallet.component';
import { TransactionsComponent } from './components/transactions/transactions.component';
import { MoneyRequestsComponent } from './components/money-requests/money-requests.component';
import { RecoveryComponent } from './components/recovery/recovery.component';
import { SecuritySettingsComponent } from './components/security-settings/security-settings.component';
import { ForgotPinComponent } from './components/recovery/forgot-pin.component';
import { AuthGuard } from './guards/auth.guard';
import { PaymentMethodsComponent } from './components/payment-methods/payment-methods.component';
import { BusinessComponent } from './components/business/business.component';
import { BusinessAnalyticsComponent } from './components/business-analytics/business-analytics.component';
import { NotificationsComponent } from './components/notifications/notifications.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'wallet',
    component: WalletComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'transactions',
    component: TransactionsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'requests',
    component: MoneyRequestsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'security',
    component: SecuritySettingsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'payment-methods',
    component: PaymentMethodsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'business',
    component: BusinessComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'analytics',
    component: BusinessAnalyticsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'notifications',
    component: NotificationsComponent,
    canActivate: [AuthGuard]
  },
  { path: 'recovery', component: RecoveryComponent },
  { path: 'forgot-pin', component: ForgotPinComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
