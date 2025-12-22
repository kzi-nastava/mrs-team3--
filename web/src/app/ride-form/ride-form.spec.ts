import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RideFormComponent } from './ride-form';

describe('RideForm', () => {
  let component: RideFormComponent;
  let fixture: ComponentFixture<RideFormComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RideFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RideFormComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
