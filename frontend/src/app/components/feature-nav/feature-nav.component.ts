import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-feature-nav',
  templateUrl: './feature-nav.component.html',
  styleUrls: ['./feature-nav.component.css']
})
export class FeatureNavComponent implements OnInit, OnDestroy {
  unreadNotifications = 0;
  private subs = new Subscription();

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.subs.add(this.notificationService.unreadCount$.subscribe(count => {
      this.unreadNotifications = count;
    }));
    this.notificationService.refreshUnreadCount();
    this.subs.add(interval(10000).subscribe(() => this.notificationService.refreshUnreadCount()));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  isBusinessAccount(): boolean {
    return this.authService.isBusinessAccount();
  }

  logout(): void {
    this.authService.logout();
  }
}
