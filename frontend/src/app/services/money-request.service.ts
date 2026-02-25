import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MoneyRequest {
    id: number;
    requester: {
        username: string;
        fullName: string;
        email?: string;
    };
    payer: {
        username: string;
        fullName: string;
        email?: string;
    };
    amount: number;
    note: string;
    status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';
    direction: 'INCOMING' | 'OUTGOING';
    createdAt: string;
}

export interface CreateMoneyRequestDto {
    payerUsername: string;
    amount: number;
    note: string;
}

export interface CreateMoneyRequestResponse {
    message: string;
    requestId: number;
}

@Injectable({
    providedIn: 'root'
})
export class MoneyRequestService {
    private apiUrl = `${environment.apiUrl}/requests`;

    constructor(private http: HttpClient) { }

    createRequest(dto: CreateMoneyRequestDto): Observable<CreateMoneyRequestResponse> {
        return this.http.post<CreateMoneyRequestResponse>(`${this.apiUrl}/create`, dto);
    }

    getMyRequests(): Observable<MoneyRequest[]> {
        return this.http.get<MoneyRequest[]>(this.apiUrl);
    }

    respondToRequest(requestId: number, accept: boolean): Observable<any> {
        return this.http.post(`${this.apiUrl}/${requestId}/respond`, { accept });
    }
}
