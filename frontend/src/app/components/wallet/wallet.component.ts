import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { PaymentService } from '../../services/payment.service';
import { TransactionService } from '../../services/transaction.service';
import { Wallet } from '../../models/wallet.model';

interface Invoice {
  id: number;
  amount: number;
  dueDate: string;
  business: {
    username: string;
  };
}

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.css']
})
export class WalletComponent implements OnInit {
  wallet: Wallet | null = null;
  sendMoneyForm!: FormGroup;
  fundForm!: FormGroup;
  withdrawForm!: FormGroup;
  pendingInvoices: Invoice[] = [];

  loading = false;
  successMessage = '';
  errorMessage = '';
  activeTab: 'send' | 'add' | 'withdraw' | 'bills' = 'send';
  showPinModal = false;
  pendingOperation: (() => void) | null = null;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private transactionService: TransactionService
  ) { }

  ngOnInit(): void {
    this.sendMoneyForm = this.fb.group({
      receiverUsername: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      note: ['']
    });

    this.fundForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1)]]
    });

    this.withdrawForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1)]]
    });

    this.loadWallet();
    this.loadPendingInvoices();
  }

  loadWallet(): void {
    this.paymentService.getBalance().subscribe({
      next: (wallet) => {
        this.wallet = wallet;
      },
      error: (error) => {
        console.error('Error loading wallet:', error);
      }
    });
  }

  loadPendingInvoices(): void {
    this.pendingInvoices = [];
  }

  onSendMoney(): void {
    if (this.sendMoneyForm.invalid) return;
    this.promptPin(() => {
      this.executeOperation(() => this.transactionService.sendMoney(this.sendMoneyForm.value), 'Money sent successfully!');
    });
  }

  onAddFunds(): void {
    if (this.fundForm.invalid) return;
    const { amount } = this.fundForm.value;
    this.promptPin(() => {
      this.executeOperation(() => this.paymentService.addFunds(amount), `Successfully added ${amount} to your wallet!`);
    });
  }

  onWithdrawFunds(): void {
    if (this.withdrawForm.invalid) return;
    const { amount } = this.withdrawForm.value;
    this.promptPin(() => {
      this.executeOperation(() => this.paymentService.withdrawFunds(amount), `Successfully withdrew ${amount} to your default card!`);
    });
  }

  onPayInvoice(invoice: Invoice): void {
    this.errorMessage = `Invoice payment is not available yet (Invoice #${invoice.id}).`;
  }

  promptPin(operation: () => void): void {
    this.pendingOperation = operation;
    this.showPinModal = true;
  }

  onPinVerified(): void {
    this.showPinModal = false;
    if (this.pendingOperation) {
      this.pendingOperation();
      this.pendingOperation = null;
    }
  }

  onPinCancel(): void {
    this.showPinModal = false;
    this.pendingOperation = null;
  }

  private executeOperation(operation: () => Observable<unknown>, successMsg: string): void {
    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    operation().subscribe({
      next: () => {
        this.successMessage = successMsg;
        this.sendMoneyForm.reset();
        this.fundForm.reset();
        this.withdrawForm.reset();
        this.loadWallet();
        this.loadPendingInvoices();
        this.loading = false;
      },
      error: (error: any) => {
        this.errorMessage = error.error?.message || 'Operation failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
