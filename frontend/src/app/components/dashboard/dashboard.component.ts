import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { WalletService } from '../../services/wallet.service';
import { TransactionService } from '../../services/transaction.service';
import { PaymentService } from '../../services/payment.service';
import { Wallet } from '../../models/wallet.model';
import { Transaction } from '../../models/transaction.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  wallet: Wallet | null = null;
  recentTransactions: Transaction[] = [];
  username = '';
  loading = true;
  unreadNotifications = 0;

  constructor(
    private authService: AuthService,
    private walletService: WalletService,
    private transactionService: TransactionService,
    private paymentService: PaymentService,
    private router: Router
  ) { }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.username = user?.username || '';

    this.loadDashboardData();
    this.loadUnreadCount();
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

  loadUnreadCount(): void {
    this.unreadNotifications = 0;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isBusinessAccount(): boolean {
    return this.authService.isBusinessAccount();
  }
}
