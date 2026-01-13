import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DriverSidebarComponent } from './driver-sidebar';

describe('DriverSidebarComponent', () => {
  let component: DriverSidebarComponent;
  let fixture: ComponentFixture<DriverSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DriverSidebarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DriverSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});