import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { MyStatsComponent } from './my-stats.component';
import { UserProfileService } from '../../services/user-profile.service';
import { SellerProfileService } from '../../services/seller-profile.service';
import { AuthService } from '../../services/auth';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

describe('MyStatsComponent', () => {
    let component: MyStatsComponent;
    let fixture: ComponentFixture<MyStatsComponent>;
    let authServiceMock: any;
    let userProfileServiceMock: jasmine.SpyObj<UserProfileService>;
    let sellerProfileServiceMock: jasmine.SpyObj<SellerProfileService>;

    const mockClientUser = { id: 'user-1', role: 'CLIENT', firstName: 'John' };
    const mockSellerUser = { id: 'seller-1', role: 'SELLER', firstName: 'Jane' };

    const mockUserProfile = {
        userId: 'user-1',
        totalSpent: 125.50,
        totalOrders: 3,
        mostBoughtCategory: 'Electronics',
        averageRating: 4.5
    };

    const mockSellerProfile = {
        userId: 'seller-1',
        totalRevenue: 2500.00,
        totalSales: 50,
        totalOrders: 45,
        isVerified: true,
        averageRating: 4.8
    };

    beforeEach(async () => {
        authServiceMock = { currentUser$: new BehaviorSubject(mockClientUser) };
        userProfileServiceMock = jasmine.createSpyObj('UserProfileService', ['getUserStatistics']);
        sellerProfileServiceMock = jasmine.createSpyObj('SellerProfileService', ['getSellerStatistics']);

        await TestBed.configureTestingModule({
            imports: [
                MyStatsComponent,
                HttpClientTestingModule,
                RouterTestingModule,
                NoopAnimationsModule,
                MatSnackBarModule,
                MatCardModule,
                MatIconModule,
                MatProgressSpinnerModule
            ],
            providers: [
                { provide: AuthService, useValue: authServiceMock },
                { provide: UserProfileService, useValue: userProfileServiceMock },
                { provide: SellerProfileService, useValue: sellerProfileServiceMock }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(MyStatsComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load client stats on init when user is CLIENT', () => {
        userProfileServiceMock.getUserStatistics.and.returnValue(of(mockUserProfile));
        fixture.detectChanges();

        expect(component.currentUserRole).toBe('CLIENT');
        expect(userProfileServiceMock.getUserStatistics).toHaveBeenCalledWith('user-1');
        expect(component.userProfile?.totalSpent).toBe(125.50);
        expect(component.formattedTotalSpent).toBe('$125.50');
    });

    it('should load seller stats on init when user is SELLER', () => {
        authServiceMock.currentUser$.next(mockSellerUser);
        sellerProfileServiceMock.getSellerStatistics.and.returnValue(of(mockSellerProfile as any));
        fixture.detectChanges();

        expect(component.currentUserRole).toBe('SELLER');
        expect(sellerProfileServiceMock.getSellerStatistics).toHaveBeenCalledWith('seller-1');
        expect(component.sellerProfile?.totalRevenue).toBe(2500);
        expect(component.formattedRevenue).toBe('$2500.00');
    });

    it('should handle client stats error gracefully', () => {
        userProfileServiceMock.getUserStatistics.and.returnValue(throwError(() => new Error('API Error')));
        spyOn(console, 'error');
        fixture.detectChanges();

        expect(component.isLoading).toBeFalse();
        expect(console.error).toHaveBeenCalled();
    });

    it('should handle missing dates safely', () => {
        userProfileServiceMock.getUserStatistics.and.returnValue(of(mockUserProfile));
        fixture.detectChanges();
        expect(component.lastOrderDisplay).toBe('No orders yet');
    });
});