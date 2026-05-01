import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ReviewApi } from '@core/api/review.api';
import { ReviewResponse } from '@core/models/review.types';
import { Page } from '@core/models/common.types';
import { ToastService } from '@core/toast.service';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StarRatingComponent } from '@shared/ui/star-rating/star-rating.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

@Component({
  selector: 'sc-admin-review-moderation',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    SpinnerComponent,
    EmptyStateComponent,
    StarRatingComponent,
    PaginationComponent,
    ModalComponent,
    FormFieldComponent,
  ],
  templateUrl: './review-moderation.component.html',
  styleUrls: ['./review-moderation.component.scss'],
})
export class AdminReviewModerationComponent implements OnInit {
  private readonly reviewApi = inject(ReviewApi);
  private readonly toast = inject(ToastService);

  readonly reviews = signal<ReviewResponse[]>([]);
  readonly loading = signal(true);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isFirst = signal(true);
  readonly isLast = signal(true);

  readonly rejectModalOpen = signal(false);
  readonly rejecting = signal(false);
  rejectTargetId: string | null = null;
  rejectReason = '';

  private readonly pageSize = 20;

  async ngOnInit(): Promise<void> {
    await this.loadPage(0);
  }

  async loadPage(page: number): Promise<void> {
    this.loading.set(true);
    try {
      const result: Page<ReviewResponse> = await firstValueFrom(
        this.reviewApi.listPending(page, this.pageSize),
      );
      this.reviews.set(result.content);
      this.currentPage.set(result.number);
      this.totalPages.set(result.totalPages);
      this.isFirst.set(result.first);
      this.isLast.set(result.last);
    } finally {
      this.loading.set(false);
    }
  }

  async approve(id: string): Promise<void> {
    try {
      await firstValueFrom(this.reviewApi.approve(id));
      this.reviews.update((arr) => arr.filter((r) => r.id !== id));
      this.toast.show('Degerlendirme onaylandi', 'success');
    } catch {
      /* error.interceptor handles toast */
    }
  }

  openRejectModal(id: string): void {
    this.rejectTargetId = id;
    this.rejectReason = '';
    this.rejectModalOpen.set(true);
  }

  async confirmReject(): Promise<void> {
    if (!this.rejectTargetId || !this.rejectReason.trim()) return;
    this.rejecting.set(true);
    try {
      await firstValueFrom(this.reviewApi.reject(this.rejectTargetId, this.rejectReason));
      this.reviews.update((arr) => arr.filter((r) => r.id !== this.rejectTargetId));
      this.toast.show('Degerlendirme reddedildi', 'success');
      this.rejectModalOpen.set(false);
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.rejecting.set(false);
      this.rejectTargetId = null;
      this.rejectReason = '';
    }
  }

  onPageChange(page: number): void {
    this.loadPage(page);
  }
}
