import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Wallet } from '../models/wallet.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private walletUrl = `${environment.apiUrl}/wallet`;

  constructor(private http: HttpClient) {}

  // Wallet Operations
  getBalance(): Observable<Wallet> {
    return this.http.get<Wallet>(`${this.walletUrl}/balance`);
  }

  addFunds(amount: number): Observable<Wallet> {
    return this.http.post<Wallet>(`${this.walletUrl}/add-funds`, { amount });
  }

  withdrawFunds(amount: number): Observable<Wallet> {
    return this.http.post<Wallet>(`${this.walletUrl}/withdraw`, { amount });
  }
}
