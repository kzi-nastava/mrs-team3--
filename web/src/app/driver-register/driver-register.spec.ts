import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DriverRegisterComponent } from './driver-register';

describe('DriverRegister', () => {
  let component: DriverRegisterComponent;
  let fixture: ComponentFixture<DriverRegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DriverRegisterComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DriverRegisterComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
