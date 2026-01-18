import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DriverHistoryComponent } from './driver-history';

describe('DriverHistoryComponent', () => {
  let component: DriverHistoryComponent;
  let fixture: ComponentFixture<DriverHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DriverHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DriverHistoryComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
