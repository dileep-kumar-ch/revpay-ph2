import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { MoneyRequestService } from '../../services/money-request.service';
import {
  NotificationCategoryFilter,
  NotificationItem,
  NotificationPreference,
  NotificationService
} from '../../services/notification.service';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit, OnDestroy {
  notifications: NotificationItem[] = [];
  selectedCategory: NotificationCategoryFilter = 'ALL';
  unreadOnly = false;
  loading = false;
  error = '';
  savingPreferences = false;
  showPinModal = false;
  pendingRequestAction: { requestId: number; notificationId: number } | null = null;
  processingRequestId: number | null = null;
  private requestStatusById: Record<number, string> = {};

  preferences: NotificationPreference = {
    transactionsEnabled: true,
    requestsEnabled: true,
    alertsEnabled: true,
    lowBalanceThreshold: 100
  };

  private pollSub: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private moneyRequestService: MoneyRequestService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadPreferences();
    this.loadNotifications();
    this.loadRequestStatuses();
    this.notificationService.refreshUnreadCount();
    this.pollSub = interval(10000).subscribe(() => {
      this.loadNotifications(false);
      this.loadRequestStatuses(false);
      this.notificationService.refreshUnreadCount();
    });
  }

  ngOnDestroy(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
    }
  }

  setCategory(category: NotificationCategoryFilter): void {
    this.selectedCategory = category;
    this.loadNotifications();
  }

  toggleUnreadOnly(): void {
    this.unreadOnly = !this.unreadOnly;
    this.loadNotifications();
  }

  loadNotifications(showLoader = true): void {
    if (showLoader) {
      this.loading = true;
    }
    this.notificationService.getNotifications(this.selectedCategory, this.unreadOnly).subscribe({
      next: (items) => {
        this.notifications = items;
        this.error = '';
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load notifications.';
        this.loading = false;
      }
    });
  }

  markOneAsRead(notification: NotificationItem, navigate = false): void {
    if (notification.read) {
      if (navigate) {
        this.navigateTo(notification);
      }
      return;
    }
    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.notificationService.refreshUnreadCount();
        if (navigate) {
          this.navigateTo(notification);
        }
      },
      error: () => {
        this.error = 'Failed to update notification state.';
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications = this.notifications.map(item => ({ ...item, read: true }));
      },
      error: () => {
        this.error = 'Failed to mark all notifications as read.';
      }
    });
  }

  onOpenNotification(notification: NotificationItem): void {
    this.markOneAsRead(notification, true);
  }

  canRespondToNotification(notification: NotificationItem): boolean {
    if (notification.type !== 'MONEY_REQUEST_RECEIVED') {
      return false;
    }
    const requestId = this.extractRequestId(notification);
    if (requestId === null) {
      return false;
    }
    const currentStatus = this.requestStatusById[requestId] || notification.status;
    return currentStatus === 'PENDING';
  }

  onAcceptRequest(notification: NotificationItem): void {
    const requestId = this.extractRequestId(notification);
    if (requestId === null) {
      this.error = 'Unable to identify money request.';
      return;
    }
    this.pendingRequestAction = { requestId, notificationId: notification.id };
    this.showPinModal = true;
  }

  onRejectRequest(notification: NotificationItem): void {
    const requestId = this.extractRequestId(notification);
    if (requestId === null) {
      this.error = 'Unable to identify money request.';
      return;
    }
    this.processingRequestId = requestId;
    this.moneyRequestService.respondToRequest(requestId, false).subscribe({
      next: () => {
        this.requestStatusById[requestId] = 'DECLINED';
        notification.status = 'DECLINED';
        notification.read = true;
        this.processingRequestId = null;
        this.notificationService.refreshUnreadCount();
        this.loadRequestStatuses(false);
        this.loadNotifications(false);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reject request.';
        this.processingRequestId = null;
      }
    });
  }

  onPinVerified(): void {
    this.showPinModal = false;
    if (!this.pendingRequestAction) {
      return;
    }
    const { requestId } = this.pendingRequestAction;
    this.processingRequestId = requestId;
    this.moneyRequestService.respondToRequest(requestId, true).subscribe({
      next: () => {
        this.requestStatusById[requestId] = 'ACCEPTED';
        this.pendingRequestAction = null;
        this.processingRequestId = null;
        this.notificationService.refreshUnreadCount();
        this.loadRequestStatuses(false);
        this.loadNotifications(false);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to accept request.';
        this.pendingRequestAction = null;
        this.processingRequestId = null;
      }
    });
  }

  onPinCancel(): void {
    this.showPinModal = false;
    this.pendingRequestAction = null;
  }

  savePreferences(): void {
    this.savingPreferences = true;
    this.notificationService.updatePreferences(this.preferences).subscribe({
      next: (saved) => {
        this.preferences = saved;
        this.savingPreferences = false;
      },
      error: () => {
        this.error = 'Failed to save notification preferences.';
        this.savingPreferences = false;
      }
    });
  }

  private loadPreferences(): void {
    this.notificationService.getPreferences().subscribe({
      next: (prefs) => {
        this.preferences = prefs;
      },
      error: () => {
        this.error = 'Failed to load notification preferences.';
      }
    });
  }

  private navigateTo(notification: NotificationItem): void {
    const target = notification.navigationTarget;
    if (!target || !target.startsWith('/')) {
      return;
    }

    if (target.startsWith('/transactions/')) {
      this.router.navigate(['/transactions']);
      return;
    }
    if (target.startsWith('/requests/')) {
      this.router.navigate(['/requests']);
      return;
    }
    if (target.startsWith('/business/invoices/')) {
      this.router.navigate(['/business']);
      return;
    }

    this.router.navigateByUrl(target);
  }

  private extractRequestId(notification: NotificationItem): number | null {
    const target = notification.navigationTarget || '';
    const match = target.match(/^\/requests\/(\d+)$/);
    if (!match) {
      return null;
    }
    const id = Number(match[1]);
    return Number.isFinite(id) ? id : null;
  }

  private loadRequestStatuses(showError = false): void {
    this.moneyRequestService.getMyRequests().subscribe({
      next: (requests) => {
        const map: Record<number, string> = {};
        requests.forEach(req => {
          map[req.id] = req.status;
        });
        this.requestStatusById = map;
      },
      error: () => {
        if (showError) {
          this.error = 'Failed to load request statuses.';
        }
      }
    });
  }
}
