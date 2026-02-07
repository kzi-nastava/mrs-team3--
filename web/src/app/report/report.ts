import {
  Component,
  OnInit,
  ViewChild,
  ChangeDetectorRef,
  AfterViewInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { jwtDecode } from 'jwt-decode';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';

import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Legend,
  BarController,
  BarElement,
  Filler
} from 'chart.js';

import { DatePickerModule } from 'primeng/datepicker';
import { TabsModule } from 'primeng/tabs';
import { AuthService } from '../services/auth.service';
import { ReportService } from '../services/report.service';

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Legend,
  BarController,
  BarElement,
  Filler
);

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    BaseChartDirective,
    DatePickerModule,
    TabsModule,
    SelectModule
  ],
  templateUrl: './report.html',
  styleUrl: './report.css',
})
export class Report implements OnInit, AfterViewInit {
  @ViewChild('ridesChartAll') ridesChartAll?: BaseChartDirective;
  @ViewChild('distanceChartAll') distanceChartAll?: BaseChartDirective;
  @ViewChild('moneyChartAll') moneyChartAll?: BaseChartDirective;
  @ViewChild('ridesChartSingle') ridesChartSingle?: BaseChartDirective;
  @ViewChild('distanceChartSingle') distanceChartSingle?: BaseChartDirective;
  @ViewChild('moneyChartSingle') moneyChartSingle?: BaseChartDirective;

  constructor(
    private reportService: ReportService,
    private cdr: ChangeDetectorRef,
    private authService: AuthService 
  ) { }

  activeTabIndex = 0;

  isAdmin = false;
  selectedUserId: number | null = null;

  userOptions: { label: string; value: number | null }[] = [
    { label: 'All users', value: null }
  ];


  private dateChangeTimer: any = null;

  fromDate: Date = new Date('2026-01-01');
  toDate: Date = new Date('2026-02-01');

  totalRides = 0;
  totalDistance = 0;
  totalMoney = 0;
  avgRides = 0;

  private commonOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index'
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        padding: 16,
        cornerRadius: 10,
        titleFont: { size: 14, weight: 'bold' },
        bodyFont: { size: 13 },
        displayColors: false,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          font: { size: 11 },
          maxRotation: 0,
          autoSkipPadding: 20,
          color: '#64748b'
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.1)' },
        ticks: {
          font: { size: 11 },
          color: '#64748b',
          padding: 10
        }
      }
    }
  };

  public lineChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{
      label: 'Rides per day',
      data: [],
      backgroundColor: (context: any) => {
        const ctx = context.chart.ctx;
        const gradient = ctx.createLinearGradient(0, 0, 0, 350);
        gradient.addColorStop(0, 'rgba(59, 130, 246, 0.85)');
        gradient.addColorStop(1, 'rgba(59, 130, 246, 0.4)');
        return gradient;
      },
      borderColor: 'transparent',
      borderWidth: 0,
      borderRadius: 8,
      borderSkipped: false,
      barPercentage: 0.7
    }]
  };

  public lineChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index'
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        padding: 16,
        cornerRadius: 10,
        titleFont: { size: 14, weight: 'bold' },
        bodyFont: { size: 13 },
        displayColors: false,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          font: { size: 11 },
          maxRotation: 0,
          autoSkipPadding: 20,
          color: '#64748b'
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.1)' },
        ticks: {
          font: { size: 11 },
          color: '#64748b',
          padding: 10
        }
      }
    }
  };

  public distanceChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{
      label: 'Distance (km)',
      data: [],
      backgroundColor: (context: any) => {
        const ctx = context.chart.ctx;
        const gradient = ctx.createLinearGradient(0, 0, 0, 350);
        gradient.addColorStop(0, 'rgba(16, 185, 129, 0.85)');
        gradient.addColorStop(1, 'rgba(16, 185, 129, 0.4)');
        return gradient;
      },
      borderColor: 'transparent',
      borderWidth: 0,
      borderRadius: 8,
      borderSkipped: false,
      barPercentage: 0.7
    }]
  };

  public distanceChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index'
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        padding: 16,
        cornerRadius: 10,
        titleFont: { size: 14, weight: 'bold' },
        bodyFont: { size: 13 },
        displayColors: false,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context) => {
            const value = context.parsed?.y ?? 0;
            return `${value.toLocaleString()} km`;
          }
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          font: { size: 11 },
          maxRotation: 0,
          autoSkipPadding: 20,
          color: '#64748b'
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.1)' },
        ticks: {
          font: { size: 11 },
          color: '#64748b',
          padding: 10,
          callback: (value) => `${Number(value).toLocaleString()}`
        }
      }
    }
  };

  public moneyChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{
      label: 'Money (RSD)',
      data: [],
      backgroundColor: (context: any) => {
        const ctx = context.chart.ctx;
        const gradient = ctx.createLinearGradient(0, 0, 0, 350);
        gradient.addColorStop(0, 'rgba(139, 92, 246, 0.85)');
        gradient.addColorStop(1, 'rgba(139, 92, 246, 0.4)');
        return gradient;
      },
      borderColor: 'transparent',
      borderWidth: 0,
      borderRadius: 8,
      borderSkipped: false,
      barPercentage: 0.7
    }]
  };

  public moneyChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index'
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        padding: 16,
        cornerRadius: 10,
        titleFont: { size: 14, weight: 'bold' },
        bodyFont: { size: 13 },
        displayColors: false,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context) => {
            const value = context.parsed?.y ?? 0;
            return `${value.toLocaleString()} RSD`;
          }
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          font: { size: 11 },
          maxRotation: 0,
          autoSkipPadding: 20,
          color: '#64748b'
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.1)' },
        ticks: {
          font: { size: 11 },
          color: '#64748b',
          padding: 10,
          callback: (value) => `${Number(value).toLocaleString()}`
        }
      }
    }
  };

  ngOnInit(): void {

    const role = this.authService.getUserRole();
    const id = this.authService.getUserId();

    this.isAdmin = role === 'ADMIN';

    if (!this.isAdmin && id) {
      this.selectedUserId = id;
    }

    if (this.isAdmin) {
      this.loadUsers();
    }

    this.loadReport();
  }





  ngAfterViewInit(): void {
    setTimeout(() => this.forceChartsUpdate(), 100);
  }

  onTabChange(_: any) {
    // кад промениш таб, Chart.js тражи refresh јер canvas постане видљив тек тад
    setTimeout(() => this.forceChartsUpdate(), 50);
  }

  onDateChange() {
    // debounce да не зове API на сваки микроклик по календару
    if (this.dateChangeTimer) clearTimeout(this.dateChangeTimer);

    this.dateChangeTimer = setTimeout(() => {
      this.loadReport();
    }, 250);
  }

  private resetCharts() {
    // ✅ Kreiranje NOVIH objekata umesto mutacije - mora da trigggeruje Angular change detection

    // RIDES
    this.lineChartData = {
      labels: [],
      datasets: [{
        ...this.lineChartData.datasets[0],
        data: []
      }]
    };

    // DISTANCE
    this.distanceChartData = {
      labels: [],
      datasets: [{
        ...this.distanceChartData.datasets[0],
        data: []
      }]
    };

    // MONEY
    this.moneyChartData = {
      labels: [],
      datasets: [{
        ...this.moneyChartData.datasets[0],
        data: []
      }]
    };

    // ✅ Forsiraj update nakon reset-a
    this.cdr.detectChanges();
    setTimeout(() => this.forceChartsUpdate(), 0);
  }

  private normalizeRange(from: Date, to: Date): { fromIso: string, toIso: string } {
    // ако корисник обрне датуме, само замени
    let f = new Date(from);
    let t = new Date(to);

    if (f.getTime() > t.getTime()) {
      const tmp = f;
      f = t;
      t = tmp;
      // ажурирај UI да одмах види исправно
      this.fromDate = f;
      this.toDate = t;
    }

    // ✅ Kreiramo UTC Date objekte sa tačnim datumom (bez timezone shift-a)
    const fromYear = f.getFullYear();
    const fromMonth = f.getMonth();
    const fromDay = f.getDate();

    const toYear = t.getFullYear();
    const toMonth = t.getMonth();
    const toDay = t.getDate();

    // Date.UTC kreira timestamp u UTC bez lokalnog offset-a
    const fromUtc = new Date(Date.UTC(fromYear, fromMonth, fromDay, 0, 0, 0, 0));
    const toUtc = new Date(Date.UTC(toYear, toMonth, toDay, 23, 59, 59, 999));

    return { fromIso: fromUtc.toISOString(), toIso: toUtc.toISOString() };
  }

  private formatDateLocal(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    // Backend očekuje ISO timestamp, ali koristimo lokalni datum bez UTC konverzije
    return `${year}-${month}-${day}T00:00:00.000Z`;
  }

  private forceChartsUpdate() {
    // ✅ Update SVE chart instance-e
    // Koristimo 'resize' umesto 'none' da bi se prazan grafik iscrtao
    this.ridesChartAll?.chart?.update('resize');
    this.distanceChartAll?.chart?.update('resize');
    this.moneyChartAll?.chart?.update('resize');
    this.ridesChartSingle?.chart?.update('resize');
    this.distanceChartSingle?.chart?.update('resize');
    this.moneyChartSingle?.chart?.update('resize');
  }

  loadReport(from?: string, to?: string) {
    // ако није прослеђено, користи date pickere
    if (!from || !to) {
      if (!this.fromDate || !this.toDate) return;

      const range = this.normalizeRange(this.fromDate, this.toDate);
      from = range.fromIso;
      to = range.toIso;
    }

    console.log('📅 Loading report:', { from, to });

    this.reportService
      .getReport(from, to, this.selectedUserId)
      .subscribe({
        next: (res: any) => {
          console.log('📊 Backend response:', res);

          // ✅ AKO NEMA PODATAKA — RESETUJ GRAFIKE
          if (!res.daily || res.daily.length === 0) {
            console.warn('⚠️ No data returned from backend');
            this.resetCharts();

            this.totalRides = 0;
            this.totalDistance = 0;
            this.totalMoney = 0;
            this.avgRides = 0;

            return;
          }

          // 👉 TEK POSLE OVOGA ide normalna logika

          // ✅ Sortiraj podatke po datumu (od starijih ka novijim - s leva na desno)
          const sortedDaily = [...res.daily].sort((a: any, b: any) => {
            return new Date(a.date).getTime() - new Date(b.date).getTime();
          });

          const labels = sortedDaily.map((d: any) => d.date);

          // RIDES - ✅ Kopiraj niz da bi Angular detektovao promenu
          this.lineChartData = {
            labels: [...labels],
            datasets: [{
              ...this.lineChartData.datasets[0],
              data: sortedDaily.map((d: any) => d.rideCount)
            }]
          };

          // DISTANCE
          this.distanceChartData = {
            labels: [...labels],
            datasets: [{
              ...this.distanceChartData.datasets[0],
              data: sortedDaily.map((d: any) => d.totalDistance)
            }]
          };

          // MONEY
          this.moneyChartData = {
            labels: [...labels],
            datasets: [{
              ...this.moneyChartData.datasets[0],
              data: sortedDaily.map((d: any) => d.totalMoney)
            }]
          };

          // SUMMARY
          this.totalRides = res.totalRides ?? 0;
          this.totalDistance = res.totalDistance ?? 0;
          this.totalMoney = res.totalMoney ?? 0;
          this.avgRides = res.avgRidesPerDay ?? 0;

          // ✅ Forsiraj Angular change detection
          this.cdr.detectChanges();

          // ✅ Update sve chartove nakon što se podaci promene
          setTimeout(() => this.forceChartsUpdate(), 50);
        },

        error: (err) => {
          console.error('Report error:', err);
          this.resetCharts();
        }
      });
  }

  private loadUsers() {
    this.reportService.getAllUsers().subscribe((users: any[]) => {

      const mapped = users.map(u => ({
        label: `${u.firstName} ${u.lastName}`,
        value: u.id
      }));

      this.userOptions = [
        { label: 'All users', value: null },
        ...mapped
      ];
    });
  }

}