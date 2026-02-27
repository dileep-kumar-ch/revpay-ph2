import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, SendMoneyRequest } from '../models/transaction.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = `${environment.apiUrl}/transactions`;

  constructor(private http: HttpClient) { }

  sendMoney(request: SendMoneyRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/send`, request);
  }

  getTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(this.apiUrl);
  }
}
