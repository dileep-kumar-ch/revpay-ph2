import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-security-settings',
  templateUrl: './security-settings.component.html',
  styleUrls: ['./security-settings.component.css']
})
export class SecuritySettingsComponent implements OnInit {
  pinForm!: FormGroup;
  successMessage = '';
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.pinForm = this.fb.group({
      pin: ['', [Validators.required, Validators.pattern('^[0-9]{4}$')]],
      confirmPin: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('pin')?.value === g.get('confirmPin')?.value
      ? null : { 'mismatch': true };
  }

  onPinSubmit(): void {
    if (this.pinForm.invalid) return;
    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.authService.setTransactionPin(this.pinForm.value.pin).subscribe({
      next: () => {
        this.successMessage = 'Transaction PIN updated successfully!';
        this.pinForm.reset();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update PIN.';
        this.loading = false;
      }
    });
  }
}
