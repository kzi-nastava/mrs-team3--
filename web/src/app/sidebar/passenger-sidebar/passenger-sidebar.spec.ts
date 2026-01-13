import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PassengerSidebarComponent } from './passenger-sidebar';

describe('PassengerSidebarComponent', () => {
  let component: PassengerSidebarComponent;
  let fixture: ComponentFixture<PassengerSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PassengerSidebarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PassengerSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});