import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-pin-verification',
    template: `
    <div class="modal fade show d-block pin-modal-backdrop" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content pin-modal-content border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title">Security Verification</h5>
            <button type="button" class="btn-close" (click)="cancel.emit()"></button>
          </div>
          <div class="modal-body py-4">
            <p class="pin-modal-subtitle mb-4">Please enter your 4-digit transaction PIN to authorize this transfer.</p>
            
            <div class="d-flex justify-content-center gap-2 mb-4">
                <input *ngFor="let i of [0,1,2,3]" 
                     #pinInput
                     type="password" 
                     class="form-control pin-input" 
                     maxlength="1" 
                     inputmode="numeric"
                     pattern="[0-9]*"
                     [(ngModel)]="pinValues[i]"
                     (keyup)="onKeyUp($event, i)"
                     (paste)="onPaste($event)"
                     autocomplete="off">
            </div>
            
            <div *ngIf="errorMessage" class="alert alert-danger py-2 small mb-0">{{ errorMessage }}</div>
            
            <div class="text-center mt-3">
              <a (click)="onForgotPin()" class="small pin-forgot-link" style="cursor: pointer; text-decoration: none;">
                Forgot transaction PIN?
              </a>
            </div>
          </div>
          <div class="modal-footer border-0 pt-0">
            <button type="button" class="btn pin-btn-cancel" (click)="cancel.emit()">Cancel</button>
            <button type="button" class="btn pin-btn-authorize px-4" 
                    [disabled]="!isPinComplete() || loading" 
                    (click)="verify()">
              {{ loading ? 'Verifying...' : 'Authorize' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .pin-modal-backdrop {
      background: rgba(15, 23, 42, 0.45);
    }
    .pin-modal-content {
      background: #fffaf5;
      border: 1px solid #fdba74;
      border-radius: 12px;
    }
    .modal-title {
      color: #9a3412;
      font-weight: 700;
    }
    .pin-modal-subtitle {
      color: #7c2d12;
    }
    .pin-input {
      width: 50px;
      height: 60px;
      text-align: center;
      font-size: 1.5rem;
      background: #fff7ed;
      border: 1px solid #fdba74;
      color: #7c2d12;
    }
    .pin-input:focus {
      background: #ffffff;
      border-color: #ea580c;
      box-shadow: 0 0 0 0.2rem rgba(234, 88, 12, 0.2);
      color: #7c2d12;
    }
    .pin-forgot-link {
      color: #c2410c;
    }
    .pin-forgot-link:hover {
      color: #ea580c;
      text-decoration: underline;
    }
    .pin-btn-cancel {
      border: 1px solid #fdba74;
      color: #9a3412;
      background: #ffffff;
      font-weight: 600;
    }
    .pin-btn-cancel:hover {
      background: #fff7ed;
      color: #7c2d12;
    }
    .pin-btn-authorize {
      background: #ea580c;
      border: 1px solid #ea580c;
      color: #ffffff;
      font-weight: 700;
    }
    .pin-btn-authorize:hover:not(:disabled) {
      background: #c2410c;
      border-color: #c2410c;
      color: #ffffff;
    }
    .pin-btn-authorize:disabled {
      opacity: 0.6;
    }
  `]
})
export class PinVerificationComponent {
    @Output() verified = new EventEmitter<void>();
    @Output() verifiedPin = new EventEmitter<string>();
    @Output() cancel = new EventEmitter<void>();

    pinValues: string[] = ['', '', '', ''];
    loading = false;
    errorMessage = '';

    constructor(
        private authService: AuthService,
        private router: Router
    ) { }

    onForgotPin(): void {
        this.cancel.emit();
        this.router.navigate(['/forgot-pin']);
    }

    onKeyUp(event: any, index: number): void {
        this.pinValues[index] = (this.pinValues[index] || '').replace(/\D/g, '');
        if (event.key === 'Backspace' && !this.pinValues[index] && index > 0) {
            const inputs = document.querySelectorAll('.pin-input');
            (inputs[index - 1] as HTMLElement).focus();
        } else if (this.pinValues[index] && index < 3) {
            const inputs = document.querySelectorAll('.pin-input');
            (inputs[index + 1] as HTMLElement).focus();
        }
    }

    onPaste(event: ClipboardEvent): void {
        const pasteData = event.clipboardData?.getData('text');
        if (pasteData && /^\d{4}$/.test(pasteData)) {
            this.pinValues = pasteData.split('');
        }
        event.preventDefault();
    }

    isPinComplete(): boolean {
        return this.pinValues.every(v => v !== '');
    }

    verify(): void {
        this.loading = true;
        this.errorMessage = '';
        const pin = this.pinValues.join('');

        this.authService.verifyTransactionPin(pin).subscribe({
            next: (res) => {
                if (res.valid) {
                    this.verifiedPin.emit(pin);
                    this.verified.emit();
                } else {
                    this.errorMessage = 'Incorrect PIN. Please try again.';
                    this.pinValues = ['', '', '', ''];
                    this.loading = false;
                }
            },
            error: (err) => {
                this.errorMessage = err.error?.message || 'Verification failed';
                this.loading = false;
            }
        });
    }
}
