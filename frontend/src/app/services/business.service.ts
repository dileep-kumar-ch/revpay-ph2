import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
    BankAccountRequest,
    BankAccountResponse,
    BusinessAnalyticsResponse,
    BusinessCardRequest,
    BusinessProfile,
    BusinessVerificationUpdateRequest,
    CreateInvoiceRequest,
    InvoicePaymentRequest,
    InvoiceResponse,
    LoanApplicationRequest,
    LoanResponse
} from '../models/business.model';
import { Card } from '../models/card.model';

@Injectable({
    providedIn: 'root'
})
export class BusinessService {
    private readonly baseUrl = `${environment.apiUrl}/business`;

    constructor(private http: HttpClient) { }

    getProfile(): Observable<BusinessProfile> {
        return this.http.get<BusinessProfile>(`${this.baseUrl}/profile`);
    }

    submitVerification(payload: BusinessVerificationUpdateRequest): Observable<BusinessProfile> {
        return this.http.post<BusinessProfile>(`${this.baseUrl}/profile/verification`, payload);
    }

    addCard(payload: BusinessCardRequest): Observable<Card> {
        return this.http.post<Card>(`${this.baseUrl}/payment-methods/cards`, payload);
    }

    removeCard(cardId: number): Observable<{ message: string }> {
        return this.http.delete<{ message: string }>(`${this.baseUrl}/payment-methods/cards/${cardId}`);
    }

    addBankAccount(payload: BankAccountRequest): Observable<BankAccountResponse> {
        return this.http.post<BankAccountResponse>(`${this.baseUrl}/payment-methods/bank-accounts`, payload);
    }

    listBankAccounts(): Observable<BankAccountResponse[]> {
        return this.http.get<BankAccountResponse[]>(`${this.baseUrl}/payment-methods/bank-accounts`);
    }

    removeBankAccount(bankAccountId: number): Observable<{ message: string }> {
        return this.http.delete<{ message: string }>(`${this.baseUrl}/payment-methods/bank-accounts/${bankAccountId}`);
    }

    createInvoice(payload: CreateInvoiceRequest): Observable<InvoiceResponse> {
        return this.http.post<InvoiceResponse>(`${this.baseUrl}/invoices`, payload);
    }

    listInvoices(status?: string): Observable<InvoiceResponse[]> {
        if (!status) {
            return this.http.get<InvoiceResponse[]>(`${this.baseUrl}/invoices`);
        }
        return this.http.get<InvoiceResponse[]>(`${this.baseUrl}/invoices?status=${status}`);
    }

    lookupInvoices(type: string, value: string): Observable<InvoiceResponse[]> {
        return this.http.get<InvoiceResponse[]>(`${this.baseUrl}/invoices/lookup?type=${type}&value=${encodeURIComponent(value)}`);
    }

    payInvoice(payload: InvoicePaymentRequest): Observable<InvoiceResponse> {
        return this.http.post<InvoiceResponse>(`${this.baseUrl}/invoices/pay`, payload);
    }

    applyLoan(payload: LoanApplicationRequest): Observable<LoanResponse> {
        return this.http.post<LoanResponse>(`${this.baseUrl}/loans`, payload);
    }

    listLoans(): Observable<LoanResponse[]> {
        return this.http.get<LoanResponse[]>(`${this.baseUrl}/loans`);
    }

    payInstallment(loanId: number, repaymentId: number): Observable<LoanResponse> {
        return this.http.post<LoanResponse>(`${this.baseUrl}/loans/${loanId}/repayments/${repaymentId}/pay`, {});
    }

    getAnalytics(from?: string, to?: string): Observable<BusinessAnalyticsResponse> {
        const queryParams: string[] = [];
        if (from) queryParams.push(`from=${from}`);
        if (to) queryParams.push(`to=${to}`);
        const query = queryParams.length ? `?${queryParams.join('&')}` : '';
        return this.http.get<BusinessAnalyticsResponse>(`${this.baseUrl}/analytics${query}`);
    }
}
