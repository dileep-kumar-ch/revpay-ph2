import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export type NotificationCategory = 'TRANSACTIONS' | 'REQUESTS' | 'ALERTS';
export type NotificationCategoryFilter = NotificationCategory | 'ALL';

export interface NotificationItem {
  id: number;
  category: NotificationCategory;
  type: string;
  title: string;
  message: string;
  amount?: number;
  counterparty?: string;
  status?: string;
  navigationTarget?: string;
  eventTime?: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationPreference {
  transactionsEnabled: boolean;
  requestsEnabled: boolean;
  alertsEnabled: boolean;
  lowBalanceThreshold: number;
}

export interface UpdateNotificationPreferenceRequest {
  transactionsEnabled?: boolean;
  requestsEnabled?: boolean;
  alertsEnabled?: boolean;
  lowBalanceThreshold?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;
  private unreadCountSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient) { }

  getNotifications(category: NotificationCategoryFilter = 'ALL', unreadOnly = false): Observable<NotificationItem[]> {
    const query: string[] = [`unreadOnly=${unreadOnly}`];
    if (category !== 'ALL') {
      query.push(`category=${category}`);
    }
    return this.http.get<NotificationItem[]>(`${this.apiUrl}?${query.join('&')}`);
  }

  getUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.apiUrl}/unread-count`);
  }

  refreshUnreadCount(): void {
    this.getUnreadCount().pipe(
      catchError(() => of({ unreadCount: 0 }))
    ).subscribe(response => this.unreadCountSubject.next(response.unreadCount ?? 0));
  }

  markAsRead(notificationId: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.apiUrl}/${notificationId}/read`, {}).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }

  markAllAsRead(): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => this.unreadCountSubject.next(0))
    );
  }

  getPreferences(): Observable<NotificationPreference> {
    return this.http.get<NotificationPreference>(`${this.apiUrl}/preferences`);
  }

  updatePreferences(payload: UpdateNotificationPreferenceRequest): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>(`${this.apiUrl}/preferences`, payload);
  }
}
