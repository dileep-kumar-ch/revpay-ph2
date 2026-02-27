import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TransactionService } from '../../services/transaction.service';
import { PaymentService } from '../../services/payment.service';
import { NotificationService } from '../../services/notification.service';
import { Wallet } from '../../models/wallet.model';
import { Transaction } from '../../models/transaction.model';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  wallet: Wallet | null = null;
  recentTransactions: Transaction[] = [];
  username = '';
  loading = true;
  unreadNotifications = 0;
  private unreadPollingSub: Subscription | null = null;
  private unreadSub: Subscription | null = null;

  constructor(
    private authService: AuthService,
    private transactionService: TransactionService,
    private paymentService: PaymentService,
    private notificationService: NotificationService,
    private router: Router
  ) { }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.username = user?.username || '';

    this.loadDashboardData();
    this.unreadSub = this.notificationService.unreadCount$.subscribe(count => {
      this.unreadNotifications = count;
    });
    this.notificationService.refreshUnreadCount();
    this.unreadPollingSub = interval(10000).subscribe(() => this.notificationService.refreshUnreadCount());
  }

  ngOnDestroy(): void {
    if (this.unreadPollingSub) {
      this.unreadPollingSub.unsubscribe();
    }
    if (this.unreadSub) {
      this.unreadSub.unsubscribe();
    }
  }

  loadDashboardData(): void {
    this.loading = true;

    this.paymentService.getBalance().subscribe({
      next: (wallet) => {
        this.wallet = wallet;
      },
      error: (error) => {
        console.error('Error loading wallet:', error);
      }
    });

    this.transactionService.getTransactions().subscribe({
      next: (transactions) => {
        this.recentTransactions = transactions.slice(0, 5);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading transactions:', error);
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isBusinessAccount(): boolean {
    return this.authService.isBusinessAccount();
  }
}
