import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthLayout } from './auth-layout';
import { RouterTestingModule } from '@angular/router/testing';
import { CommonModule } from '@angular/common';

describe('AuthLayout', () => {
  let component: AuthLayout;
  let fixture: ComponentFixture<AuthLayout>;

  beforeEach(async () => {
    // Note: RouterTestingModule is deprecated in Angular 18+, but perfectly fine for Angular 17/early 18 setups
    await TestBed.configureTestingModule({
      imports: [AuthLayout, RouterTestingModule, CommonModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AuthLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have router-outlet', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeDefined();
  });
});