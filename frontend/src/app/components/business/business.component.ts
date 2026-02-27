import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { BusinessService } from '../../services/business.service';
import { NotificationService } from '../../services/notification.service';
import {
  CreateInvoiceRequest,
  InvoicePaymentRequest,
  InvoiceResponse,
  LoanResponse
} from '../../models/business.model';

type BusinessTab = 'invoices' | 'loans';

@Component({
  selector: 'app-business',
  templateUrl: './business.component.html',
  styleUrls: ['./business.component.css']
})
export class BusinessComponent implements OnInit {
  tab: BusinessTab = 'invoices';
  loading = false;
  error = '';
  success = '';
  showPinModal = false;
  pendingOperation: (() => void) | null = null;

  invoices: InvoiceResponse[] = [];
  loans: LoanResponse[] = [];
  selectedSupportingDocumentName = '';

  invoiceStatusFilter = '';
  lookupResults: InvoiceResponse[] = [];

  invoiceForm: FormGroup;
  invoicePayForm: FormGroup;
  loanForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private businessService: BusinessService,
    private notificationService: NotificationService
  ) {
    this.invoiceForm = this.fb.group({
      customerName: ['', Validators.required],
      customerEmail: [''],
      customerPhone: [''],
      customerId: [''],
      currency: ['USD', Validators.required],
      dueDate: ['', Validators.required],
      description: [''],
      paymentTerms: [''],
      items: this.fb.array([this.createInvoiceItemGroup()])
    });

    this.invoicePayForm = this.fb.group({
      lookupType: ['INVOICE_NUMBER', Validators.required],
      lookupValue: ['', Validators.required]
    });

    this.loanForm = this.fb.group({
      loanAmount: [null, [Validators.required, Validators.min(1)]],
      purpose: ['', Validators.required],
      financialDetails: ['', Validators.required],
      supportingDocumentsPath: [''],
      termMonths: [12, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    if (!this.isBusinessUser()) {
      this.error = 'Business account required to access this page.';
      return;
    }
    this.loadAll();
  }

  isBusinessUser(): boolean {
    return this.authService.isBusinessAccount();
  }

  get invoiceItems(): FormArray {
    return this.invoiceForm.get('items') as FormArray;
  }

  switchTab(tab: BusinessTab): void {
    this.tab = tab;
    this.clearMessages();
  }

  addInvoiceItem(): void {
    this.invoiceItems.push(this.createInvoiceItemGroup());
  }

  removeInvoiceItem(index: number): void {
    if (this.invoiceItems.length > 1) {
      this.invoiceItems.removeAt(index);
    }
  }

  createInvoice(): void {
    if (this.invoiceForm.invalid) return;
    this.loading = true;
    const payload = this.invoiceForm.value as CreateInvoiceRequest;
    this.businessService.createInvoice(payload).subscribe({
      next: () => {
        this.showSuccess('Invoice created and notification triggered.');
        this.notificationService.refreshUnreadCount();
        this.invoiceForm.reset({
          customerName: '',
          customerEmail: '',
          customerPhone: '',
          customerId: '',
          currency: 'USD',
          dueDate: '',
          description: '',
          paymentTerms: ''
        });
        this.invoiceItems.clear();
        this.invoiceItems.push(this.createInvoiceItemGroup());
        this.loadInvoices();
        this.loading = false;
      },
      error: (err) => this.handleError(err, 'Failed to create invoice')
    });
  }

  filterInvoices(): void {
    this.loadInvoices();
  }

  lookupInvoices(): void {
    const lookupType = this.invoicePayForm.get('lookupType')?.value as InvoicePaymentRequest['lookupType'] | null;
    const lookupValue = String(this.invoicePayForm.get('lookupValue')?.value ?? '').trim();

    if (!lookupType || !lookupValue) {
      this.lookupResults = [];
      this.error = 'Enter a lookup value to search invoices.';
      this.success = '';
      return;
    }

    this.loading = true;
    this.businessService.lookupInvoices(lookupType, lookupValue).subscribe({
      next: (results) => {
        this.lookupResults = results;
        if (results.length === 0) {
          this.error = 'No invoices found for this lookup.';
          this.success = '';
        } else {
          this.error = '';
        }
        this.loading = false;
      },
      error: (err) => this.handleError(err, 'Failed to lookup invoices')
    });
  }

  payInvoice(): void {
    if (this.invoicePayForm.invalid) return;
    this.loading = true;
    this.businessService.payInvoice(this.invoicePayForm.value).subscribe({
      next: (invoice) => {
        this.showSuccess(`Invoice ${invoice.invoiceNumber} paid successfully.`);
        this.notificationService.refreshUnreadCount();
        this.loadInvoices();
        this.loading = false;
      },
      error: (err) => this.handleError(err, 'Failed to process invoice payment')
    });
  }

  applyLoan(): void {
    if (this.loanForm.invalid) return;
    this.loading = true;
    this.businessService.applyLoan(this.loanForm.value).subscribe({
      next: () => {
        this.showSuccess('Loan application submitted.');
        this.loanForm.reset({ loanAmount: null, supportingDocumentsPath: '', termMonths: 12 });
        this.selectedSupportingDocumentName = '';
        this.loadLoans();
        this.loading = false;
      },
      error: (err) => this.handleError(err, 'Failed to submit loan application')
    });
  }

  payInstallment(loanId: number, repaymentId: number): void {
    this.promptPin(() => this.executeLoanRepayment(loanId, repaymentId));
  }

  onSupportingDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      this.selectedSupportingDocumentName = '';
      this.loanForm.patchValue({ supportingDocumentsPath: '' });
      return;
    }
    this.selectedSupportingDocumentName = file.name;
    this.loanForm.patchValue({ supportingDocumentsPath: file.name });
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

  private loadAll(): void {
    this.loading = true;
    this.loadInvoices();
    this.loadLoans();
    this.loading = false;
  }

  private loadInvoices(): void {
    const status = this.invoiceStatusFilter || undefined;
    this.businessService.listInvoices(status).subscribe({
      next: (invoices) => {
        this.invoices = invoices;
      },
      error: () => {
        this.invoices = [];
      }
    });
  }

  private loadLoans(): void {
    this.businessService.listLoans().subscribe({
      next: (loans) => {
        this.loans = loans;
      },
      error: () => {
        this.loans = [];
      }
    });
  }

  private createInvoiceItemGroup(): FormGroup {
    return this.fb.group({
      itemName: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [0, [Validators.required, Validators.min(0.01)]]
    });
  }

  private executeLoanRepayment(loanId: number, repaymentId: number): void {
    this.loading = true;
    this.businessService.payInstallment(loanId, repaymentId).subscribe({
      next: () => {
        this.showSuccess('Repayment completed successfully.');
        this.loadLoans();
        this.loading = false;
      },
      error: (err) => this.handleError(err, 'Failed to complete repayment')
    });
  }

  private clearMessages(): void {
    this.error = '';
    this.success = '';
  }

  private showSuccess(message: string): void {
    this.success = message;
    this.error = '';
  }

  private handleError(err: unknown, fallbackMessage: string): void {
    const maybeErr = err as { error?: { message?: string | Record<string, string> }; message?: string };
    const backendMessage = maybeErr?.error?.message;
    if (typeof backendMessage === 'string' && backendMessage.trim()) {
      this.error = backendMessage;
    } else if (backendMessage && typeof backendMessage === 'object') {
      this.error = Object.values(backendMessage).join(', ');
    } else if (typeof maybeErr?.message === 'string') {
      this.error = maybeErr.message;
    } else {
      this.error = fallbackMessage;
    }
    this.success = '';
    this.loading = false;
  }
}
