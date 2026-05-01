import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ReviewApi } from '@core/api/review.api';
import { ReviewReportResponse } from '@core/models/review.types';
import { Page } from '@core/models/common.types';
import { ToastService } from '@core/toast.service';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'sc-admin-review-reports',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    PaginationComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './review-reports.component.html',
  styleUrls: ['./review-reports.component.scss'],
})
export class AdminReviewReportsComponent implements OnInit {
  private readonly reviewApi = inject(ReviewApi);
  private readonly toast = inject(ToastService);

  readonly reports = signal<ReviewReportResponse[]>([]);
  readonly loading = signal(true);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isFirst = signal(true);
  readonly isLast = signal(true);

  readonly dismissDialogOpen = signal(false);
  private dismissTargetId: string | null = null;

  private readonly pageSize = 20;

  async ngOnInit(): Promise<void> {
    await this.loadPage(0);
  }

  async loadPage(page: number): Promise<void> {
    this.loading.set(true);
    try {
      const result: Page<ReviewReportResponse> = await firstValueFrom(
        this.reviewApi.listReports(page, this.pageSize),
      );
      this.reports.set(result.content);
      this.currentPage.set(result.number);
      this.totalPages.set(result.totalPages);
      this.isFirst.set(result.first);
      this.isLast.set(result.last);
    } finally {
      this.loading.set(false);
    }
  }

  confirmDismiss(id: string): void {
    this.dismissTargetId = id;
    this.dismissDialogOpen.set(true);
  }

  async onDismissConfirmed(): Promise<void> {
    this.dismissDialogOpen.set(false);
    if (!this.dismissTargetId) return;
    try {
      await firstValueFrom(this.reviewApi.dismissReport(this.dismissTargetId));
      this.reports.update((arr) => arr.filter((r) => r.id !== this.dismissTargetId));
      this.toast.show('Rapor kaldirildi', 'success');
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.dismissTargetId = null;
    }
  }

  onPageChange(page: number): void {
    this.loadPage(page);
  }

  statusVariant(status: string): 'warning' | 'success' | 'danger' | 'neutral' {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'RESOLVED': return 'success';
      case 'DISMISSED': return 'neutral';
      default: return 'neutral';
    }
  }
}
