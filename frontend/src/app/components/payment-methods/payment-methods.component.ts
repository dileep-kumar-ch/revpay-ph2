import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { PaymentService } from '../../services/payment.service';
import { NotificationService } from '../../services/notification.service';
import { Card, AddCardRequest } from '../../models/card.model';

@Component({
  selector: 'app-payment-methods',
  templateUrl: './payment-methods.component.html',
  styleUrls: ['./payment-methods.component.css']
})
export class PaymentMethodsComponent implements OnInit {
  cards: Card[] = [];
  cardForm: FormGroup;
  showAddCard = false;
  settingDefaultCardId: number | null = null;
  loading = false;
  error = '';
  success = '';
  private successTimerId?: number;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private notificationService: NotificationService
  ) {
    this.cardForm = this.fb.group({
      cardHolderName: ['', [Validators.required, Validators.pattern('^[A-Za-z ]+$')]],
      paymentMethodType: ['DEBIT', Validators.required],
      cardNumber: ['', [Validators.required, Validators.pattern('^[0-9]{16}$')]],
      expiryDate: ['', [
        Validators.required,
        Validators.pattern('^(0[1-9]|1[0-2])\\/([0-9]{2})$'),
        this.expiryNotPastValidator()
      ]],
      cvv: ['', [Validators.required, Validators.pattern('^[0-9]{3}$')]],
      setAsDefault: [false]
    });
  }

  ngOnInit(): void {
    this.loadCards();
  }

  get existingDefaultCard(): Card | undefined {
    return this.cards.find(card => card.isDefault);
  }

  private expiryNotPastValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value as string;
      if (!raw) return null;

      const match = raw.match(/^(0[1-9]|1[0-2])\/(\d{2})$/);
      if (!match) return null;

      const month = Number(match[1]);
      const year = 2000 + Number(match[2]);
      const now = new Date();
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth() + 1;

      if (year < currentYear) return { expired: true };
      if (year === currentYear && month < currentMonth) return { expired: true };

      return null;
    };
  }

  loadCards(): void {
    this.loading = true;
    this.paymentService.getCards().subscribe({
      next: (cards) => {
        this.cards = [...cards].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load cards';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.cardForm.valid) {
      const existingDefault = this.existingDefaultCard;
      let requestedDefault = this.cardForm.get('setAsDefault')?.value === true;

      if (requestedDefault && existingDefault) {
        const replaceDefault = confirm(
          `A default card ending in ${existingDefault.lastFourDigits} already exists. Do you want to replace it?`
        );

        if (!replaceDefault) {
          this.cardForm.patchValue({ setAsDefault: false });
          requestedDefault = false;
        }
      }

      const payload: AddCardRequest = {
        cardHolderName: this.cardForm.get('cardHolderName')?.value,
        cardNumber: this.cardForm.get('cardNumber')?.value,
        expiryDate: this.cardForm.get('expiryDate')?.value,
        cvv: this.cardForm.get('cvv')?.value,
        paymentMethodType: this.cardForm.get('paymentMethodType')?.value,
        setAsDefault: requestedDefault
      };

      this.loading = true;
      this.paymentService.addCard(payload).subscribe({
        next: (card) => {
          this.showSuccess('Card added successfully');
          this.notificationService.refreshUnreadCount();
          this.cardForm.reset({ paymentMethodType: 'DEBIT', setAsDefault: false });

          // Optimistic UI update, then sync from server for source-of-truth state.
          if (card.isDefault) {
            this.cards = this.cards.map(c => ({ ...c, isDefault: false }));
          }
          this.cards = [card, ...this.cards].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));

          if (requestedDefault) {
            // Guarantee: if user selected default while adding, force this new card as default.
            this.paymentService.setDefaultCard(card.id).subscribe({
              next: () => this.loadCards(),
              error: () => {
                this.error = 'Card added, but failed to set it as default';
                this.loadCards();
              }
            });
          } else {
            this.loadCards();
          }

          setTimeout(() => {
            this.showAddCard = false;
          }, 2000);
        },
        error: (err) => {
          this.error = 'Failed to add card';
          this.loading = false;
        }
      });
    }
  }

  setDefaultCard(card: Card): void {
    if (card.isDefault) return;

    const existingDefault = this.existingDefaultCard;
    const message = existingDefault
      ? `Card ending in ${existingDefault.lastFourDigits} is currently default. Set card ending in ${card.lastFourDigits} as new default?`
      : `Set card ending in ${card.lastFourDigits} as default?`;

    if (!confirm(message)) return;

    this.settingDefaultCardId = card.id;
    this.error = '';
    this.success = '';

    this.paymentService.setDefaultCard(card.id).subscribe({
      next: () => {
        this.showSuccess('Default card updated successfully');
        this.notificationService.refreshUnreadCount();
        this.settingDefaultCardId = null;
        this.loadCards();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update default card';
        this.settingDefaultCardId = null;
      }
    });
  }

  private showSuccess(message: string): void {
    this.success = message;
    if (this.successTimerId) {
      clearTimeout(this.successTimerId);
    }
    this.successTimerId = window.setTimeout(() => {
      this.success = '';
    }, 2000);
  }

  formatCardNumber(value: string): string {
    if (!value) return '';
    return value.replace(/\s+/g, '').replace(/(\d{4})/g, '$1 ').trim();
  }

  onCardNumberInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = (input.value || '').replace(/\D/g, '').slice(0, 16);
    this.cardForm.get('cardNumber')?.setValue(digits, { emitEvent: false });
    input.value = digits;
  }

  onCvvInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = (input.value || '').replace(/\D/g, '').slice(0, 3);
    this.cardForm.get('cvv')?.setValue(digits, { emitEvent: false });
    input.value = digits;
  }

  onCardHolderNameInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const sanitized = (input.value || '').replace(/[^A-Za-z ]/g, '').replace(/\s{2,}/g, ' ');
    this.cardForm.get('cardHolderName')?.setValue(sanitized, { emitEvent: false });
    input.value = sanitized;
  }

  onExpiryInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = (input.value || '').replace(/\D/g, '').slice(0, 4);
    const formatted = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
    this.cardForm.get('expiryDate')?.setValue(formatted, { emitEvent: false });
    input.value = formatted;
  }

  deleteCard(id: number): void {
    if (confirm('Are you sure you want to delete this card?')) {
      this.paymentService.deleteCard(id).subscribe({
        next: () => {
          this.showSuccess('Card removed successfully');
          this.notificationService.refreshUnreadCount();
          this.loadCards();
        },
        error: (err) => {
          this.error = 'Failed to delete card';
        }
      });
    }
  }
}
