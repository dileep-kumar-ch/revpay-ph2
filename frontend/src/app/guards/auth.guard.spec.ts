import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { AuthGuard } from './auth.guard';

describe('authGuard', () => {
  const executeGuard = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => 
      TestBed.runInInjectionContext(() => TestBed.inject(AuthGuard).canActivate(route, state));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
