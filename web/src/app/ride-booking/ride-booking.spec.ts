import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideBookingComponent } from './ride-booking';

describe('RideBookingComponent', () => {
  let component: RideBookingComponent;
  let fixture: ComponentFixture<RideBookingComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideBookingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideBookingComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
