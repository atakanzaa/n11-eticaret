import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ReviewApi } from '@core/api/review.api';
import { ReviewResponse } from '@core/models/review.types';
import { Page } from '@core/models/common.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { StarRatingComponent } from '@shared/ui/star-rating/star-rating.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'sc-my-reviews',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    PaginationComponent,
    StarRatingComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './my-reviews.component.html',
  styleUrls: ['./my-reviews.component.scss'],
})
export class MyReviewsComponent implements OnInit {
  private readonly reviewApi = inject(ReviewApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly reviews = signal<ReviewResponse[]>([]);
  readonly loading = signal(true);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isFirst = signal(true);
  readonly isLast = signal(true);

  readonly deletingReview = signal<ReviewResponse | null>(null);
  readonly showDeleteConfirm = signal(false);

  readonly hasItems = computed(() => this.reviews().length > 0);

  async ngOnInit(): Promise<void> {
    await this.loadPage(0);
  }

  async loadPage(page: number): Promise<void> {
    this.loading.set(true);
    try {
      const result: Page<ReviewResponse> = await firstValueFrom(this.reviewApi.myReviews(page, 20));
      this.reviews.set(result.content);
      this.currentPage.set(result.number);
      this.totalPages.set(result.totalPages);
      this.isFirst.set(result.first);
      this.isLast.set(result.last);
    } finally {
      this.loading.set(false);
    }
  }

  openDeleteConfirm(review: ReviewResponse): void {
    this.deletingReview.set(review);
    this.showDeleteConfirm.set(true);
  }

  async confirmDelete(): Promise<void> {
    const review = this.deletingReview();
    if (!review) return;
    try {
      await firstValueFrom(this.reviewApi.delete(review.id));
      this.reviews.update(arr => arr.filter(r => r.id !== review.id));
      this.toast.show(this.i18n.t('account.reviewDeleted'), 'success');
      this.showDeleteConfirm.set(false);
      this.deletingReview.set(null);
    } catch {
      // error.interceptor handles toast
    }
  }

  onPageChange(page: number): void {
    this.loadPage(page);
  }
}
