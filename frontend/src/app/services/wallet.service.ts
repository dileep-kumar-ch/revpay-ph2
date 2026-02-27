import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Wallet } from '../models/wallet.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private apiUrl = `${environment.apiUrl}/wallet`;

  constructor(private http: HttpClient) { }

  getBalance(): Observable<Wallet> {
    return this.http.get<Wallet>(`${this.apiUrl}/balance`);
  }
}
