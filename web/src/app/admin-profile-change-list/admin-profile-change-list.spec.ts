import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminProfileChangeList } from './admin-profile-change-list';

describe('AdminProfileChangeList', () => {
  let component: AdminProfileChangeList;
  let fixture: ComponentFixture<AdminProfileChangeList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminProfileChangeList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminProfileChangeList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
