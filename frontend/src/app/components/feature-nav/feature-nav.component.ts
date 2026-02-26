import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-feature-nav',
  templateUrl: './feature-nav.component.html',
  styleUrls: ['./feature-nav.component.css']
})
export class FeatureNavComponent {
  constructor(private authService: AuthService) { }

  isBusinessAccount(): boolean {
    return this.authService.isBusinessAccount();
  }

  logout(): void {
    this.authService.logout();
  }
}
