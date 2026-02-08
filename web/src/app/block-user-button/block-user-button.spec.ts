import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockUserButtonComponent } from './block-user-button';

describe('BlockUserButtonComponent', () => {
  let component: BlockUserButtonComponent;
  let fixture: ComponentFixture<BlockUserButtonComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlockUserButtonComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BlockUserButtonComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
