import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlSegment, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { adminGuard, anonymousGuard, authGuard } from './guards';
import { FakeAuthService } from './testing/fake-auth.service';

describe('guards', () => {
  let auth: FakeAuthService;
  let router: Router;

  beforeEach(() => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
  });

  const runAuth = () => TestBed.runInInjectionContext(() =>
    authGuard({} as ActivatedRouteSnapshot, { url: '/bancos/nuevo' } as RouterStateSnapshot));
  const runAdmin = () => TestBed.runInInjectionContext(() =>
    adminGuard({}, [new UrlSegment('admin', {}), new UrlSegment('bancos', {})]));
  const runAnonymous = () => TestBed.runInInjectionContext(() =>
    anonymousGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  it('authGuard redirects anonymous users to login keeping the return URL', async () => {
    const result = (await runAuth()) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/login?returnUrl=%2Fbancos%2Fnuevo');
  });

  it('authGuard lets signed-in users through', async () => {
    auth.signIn();

    expect(await runAuth()).toBe(true);
  });

  it('adminGuard sends anonymous users to login and regular users to the map', async () => {
    expect(router.serializeUrl((await runAdmin()) as UrlTree)).toBe('/login?returnUrl=%2Fadmin%2Fbancos');

    auth.signIn({ rol: 'USER' });
    expect(router.serializeUrl((await runAdmin()) as UrlTree)).toBe('/mapa');

    auth.signIn({ rol: 'ADMIN' });
    expect(await runAdmin()).toBe(true);
  });

  it('anonymousGuard keeps signed-in users away from login', async () => {
    expect(await runAnonymous()).toBe(true);

    auth.signIn();
    expect(router.serializeUrl((await runAnonymous()) as UrlTree)).toBe('/mapa');
  });
});
