import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideHistoryComponent } from './ride-history';

describe('RideHistoryComponent', () => {
  let component: RideHistoryComponent;
  let fixture: ComponentFixture<RideHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideHistoryComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
