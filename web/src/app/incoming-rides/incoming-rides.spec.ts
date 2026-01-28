import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IncomingRides } from './incoming-rides';

describe('IncomingRides', () => {
  let component: IncomingRides;
  let fixture: ComponentFixture<IncomingRides>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncomingRides]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IncomingRides);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
