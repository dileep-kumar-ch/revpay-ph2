import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PaymentService } from '../../services/payment.service';
import { TransactionService } from '../../services/transaction.service';
import { NotificationService } from '../../services/notification.service';

import { Wallet } from '../../models/wallet.model';

import { Card } from '../../models/card.model';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.css']
})
export class WalletComponent implements OnInit {
  wallet: Wallet | null = null;
  cards: Card[] = [];
  filteredFundCards: Card[] = [];
  sendMoneyForm!: FormGroup;
  fundForm!: FormGroup;
  withdrawForm!: FormGroup;


  loading = false;
  successMessage = '';
  errorMessage = '';
  activeTab: 'send' | 'add' | 'withdraw' | 'bills' = 'send';
  readonly paymentMethodOptions: Array<'DEFAULT' | 'DEBIT' | 'CREDIT'> = ['DEFAULT', 'DEBIT', 'CREDIT'];
  showPinModal = false;
  pendingOperation: (() => void) | null = null;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private transactionService: TransactionService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.sendMoneyForm = this.fb.group({
      receiverUsername: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      note: ['']
    });

    this.fundForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1)]],
      paymentMethodType: ['DEFAULT', Validators.required],
      cardId: ['', Validators.required]
    });

    this.withdrawForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1)]]
    });

    this.loadWallet();
    this.loadCards();

    this.fundForm.get('paymentMethodType')?.valueChanges.subscribe(() => {
      this.applyFundingCardSelection();
    });
   
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

  loadCards(): void {
    this.paymentService.getCards().subscribe(cards => {
      this.cards = [...cards].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
      this.applyFundingCardSelection();
    });
  }

  private applyFundingCardSelection(): void {
    const method = this.fundForm.get('paymentMethodType')?.value as 'DEFAULT' | 'DEBIT' | 'CREDIT';
    const selectedCardId = this.fundForm.get('cardId')?.value;
    const defaultCard = this.cards.find(c => c.isDefault);

    if (method === 'DEFAULT') {
      this.filteredFundCards = defaultCard ? [defaultCard] : [];
      this.fundForm.patchValue({ cardId: defaultCard?.id ?? '' }, { emitEvent: false });
      return;
    }

    this.filteredFundCards = this.cards.filter(card => card.paymentMethodType === method);
    const stillValid = this.filteredFundCards.some(card => card.id === selectedCardId);

    if (!stillValid) {
      const preferred = this.filteredFundCards.find(card => card.isDefault) || this.filteredFundCards[0];
      this.fundForm.patchValue({ cardId: preferred?.id ?? '' }, { emitEvent: false });
    }
  }

 

  onSendMoney(): void {
    if (this.sendMoneyForm.invalid) return;
    this.promptPin(() => {
      this.executeOperation(() => this.transactionService.sendMoney(this.sendMoneyForm.value), 'Money sent successfully!');
    });
  }

  onAddFunds(): void {
    if (this.fundForm.invalid) return;
    const { amount, cardId } = this.fundForm.value;
    this.promptPin(() => {
      this.executeOperation(() => this.paymentService.addFunds(amount, cardId), `Successfully added ${amount} to your wallet!`);
    });
  }

  onWithdrawFunds(): void {
    if (this.withdrawForm.invalid) return;
    const { amount } = this.withdrawForm.value;
    this.promptPin(() => {
      this.executeOperation(() => this.paymentService.withdrawFunds(amount), `Successfully withdrew ${amount} to your default card!`);
    });
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

  private executeOperation(operation: () => any, successMsg: string): void {
    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    operation().subscribe({
      next: () => {
        this.successMessage = successMsg;
        this.sendMoneyForm.reset();
        this.fundForm.reset({ paymentMethodType: 'DEFAULT', cardId: '' });
        this.applyFundingCardSelection();
        this.withdrawForm.reset();
        this.loadWallet();
        this.notificationService.refreshUnreadCount();
        
        this.loading = false;
      },
      error: (error: any) => {
        this.errorMessage = error.error?.message || 'Operation failed. Please try again.';
        this.notificationService.refreshUnreadCount();
        this.loading = false;
      }
    });
  }
}
