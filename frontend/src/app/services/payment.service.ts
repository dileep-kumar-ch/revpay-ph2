import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Card, AddCardRequest } from '../models/card.model';

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    private cardsUrl = `${environment.apiUrl}/cards`;
    private walletUrl = `${environment.apiUrl}/wallet`;

    constructor(private http: HttpClient) { }

    // Card Operations
    getCards(): Observable<Card[]> {
        return this.http.get<Card[]>(this.cardsUrl);
    }

    addCard(card: AddCardRequest): Observable<Card> {
        return this.http.post<Card>(this.cardsUrl, card);
    }

    deleteCard(id: number): Observable<any> {
        return this.http.delete(`${this.cardsUrl}/${id}`);
    }

    setDefaultCard(id: number): Observable<Card> {
        return this.http.patch<Card>(`${this.cardsUrl}/${id}/default`, {});
    }

    // Wallet Operations
    getBalance(): Observable<any> {
        return this.http.get(`${this.walletUrl}/balance`);
    }

    addFunds(amount: number, cardId: number): Observable<any> {
        return this.http.post(`${this.walletUrl}/add-funds`, { amount, cardId });
    }

    withdrawFunds(amount: number): Observable<any> {
        return this.http.post(`${this.walletUrl}/withdraw`, { amount });
    }
}
