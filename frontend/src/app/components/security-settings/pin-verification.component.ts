import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-pin-verification',
    template: `
    <div class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content glass-morphism border-0 shadow-lg">
          <div class="modal-header border-0 pb-0">
            <h5 class="modal-title text-white">Security Verification</h5>
            <button type="button" class="btn-close btn-close-white" (click)="cancel.emit()"></button>
          </div>
          <div class="modal-body py-4">
            <p class="text-white-50 mb-4">Please enter your 4-digit transaction PIN to authorize this transfer.</p>
            
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
              <a (click)="onForgotPin()" class="text-info small" style="cursor: pointer; text-decoration: none;">
                Forgot transaction PIN?
              </a>
            </div>
          </div>
          <div class="modal-footer border-0 pt-0">
            <button type="button" class="btn btn-outline-light" (click)="cancel.emit()">Cancel</button>
            <button type="button" class="btn btn-primary px-4" 
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
    .glass-morphism {
      background: rgba(30, 60, 114, 0.95);
      backdrop-filter: blur(15px);
    }
    .pin-input {
      width: 50px;
      height: 60px;
      text-align: center;
      font-size: 1.5rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      color: white;
    }
    .pin-input:focus {
      background: rgba(255, 255, 255, 0.2);
      border-color: #00d2ff;
      box-shadow: 0 0 10px rgba(0, 210, 255, 0.5);
      color: white;
    }
    .btn-primary {
      background: #00d2ff;
      border: none;
      color: #000;
      font-weight: 600;
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
