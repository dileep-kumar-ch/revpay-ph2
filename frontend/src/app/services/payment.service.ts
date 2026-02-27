import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Card, AddCardRequest } from '../models/card.model';

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    private cardsUrl = `${environment.apiUrl}/cards`;
    private walletUrl = `${environment.apiUrl}/wallet`;

    constructor(private http: HttpClient) { }

    private normalizeCard(card: any): Card {
        return {
            ...card,
            isDefault: card?.isDefault ?? card?.defaultCard ?? card?.default ?? false
        } as Card;
    }

    // Card Operations
    getCards(): Observable<Card[]> {
        return this.http.get<any[]>(this.cardsUrl).pipe(
            map(cards => (cards || []).map(card => this.normalizeCard(card)))
        );
    }

    addCard(card: AddCardRequest): Observable<Card> {
        return this.http.post<any>(this.cardsUrl, card).pipe(
            map(response => this.normalizeCard(response))
        );
    }

    deleteCard(id: number): Observable<any> {
        return this.http.delete(`${this.cardsUrl}/${id}`);
    }

    setDefaultCard(id: number): Observable<Card> {
        return this.http.patch<any>(`${this.cardsUrl}/${id}/default`, {}).pipe(
            map(response => this.normalizeCard(response))
        );
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
