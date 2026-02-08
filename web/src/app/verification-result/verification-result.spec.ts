import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VerificationResultComponent } from './verification-result';

describe('VerificationResult', () => {
  let component: VerificationResultComponent;
  let fixture: ComponentFixture<VerificationResultComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerificationResultComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VerificationResultComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
