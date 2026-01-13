import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuestSidebarComponent } from './guest-sidebar';

describe('GuestSidebarComponent', () => {
  let component: GuestSidebarComponent;
  let fixture: ComponentFixture<GuestSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuestSidebarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GuestSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});