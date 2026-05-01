import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ReviewApi } from '@core/api/review.api';
import { SellerApi } from '@core/api/seller.api';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { ReviewReplyResponse } from '@core/models/review.types';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

@Component({
  selector: 'sc-seller-review-replies',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-replies.component.html',
  styleUrls: ['./review-replies.component.scss'],
  imports: [
    FormsModule,
    DatePipe,
    SlicePipe,
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    ModalComponent,
    ConfirmDialogComponent,
    FormFieldComponent,
  ],
})
export class SellerReviewRepliesComponent implements OnInit {
  private readonly reviewApi = inject(ReviewApi);
  private readonly sellerApi = inject(SellerApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly replies = signal<ReviewReplyResponse[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  private sellerId = '';

  // Edit modal
  readonly showEditModal = signal(false);
  readonly editingReply = signal<ReviewReplyResponse | null>(null);
  readonly editContent = signal('');

  // Delete confirm
  readonly showDeleteConfirm = signal(false);
  readonly deletingReply = signal<ReviewReplyResponse | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      const seller = await firstValueFrom(this.sellerApi.me());
      this.sellerId = seller.id;
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.loading.set(false);
    }
  }

  async refresh(): Promise<void> {
    const replies = await firstValueFrom(this.reviewApi.sellerReplies(this.sellerId));
    this.replies.set(replies);
  }

  // ── Edit ──────────────────────────────────────────────
  openEditModal(reply: ReviewReplyResponse): void {
    this.editingReply.set(reply);
    this.editContent.set(reply.content);
    this.showEditModal.set(true);
  }

  async submitEdit(): Promise<void> {
    const reply = this.editingReply();
    if (!reply) return;
    this.saving.set(true);
    try {
      await firstValueFrom(
        this.reviewApi.updateReply(reply.id, this.sellerId, { content: this.editContent() }),
      );
      this.toast.show(this.i18n.t('seller.replyUpdated'), 'success');
      this.showEditModal.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  // ── Delete ────────────────────────────────────────────
  openDeleteConfirm(reply: ReviewReplyResponse): void {
    this.deletingReply.set(reply);
    this.showDeleteConfirm.set(true);
  }

  async confirmDelete(): Promise<void> {
    const reply = this.deletingReply();
    if (!reply) return;
    try {
      await firstValueFrom(this.reviewApi.deleteReply(reply.id, this.sellerId));
      this.toast.show(this.i18n.t('seller.replyDeleted'), 'success');
      this.showDeleteConfirm.set(false);
      this.replies.update(list => list.filter(r => r.id !== reply.id));
    } catch {
      /* error.interceptor handles toast */
    }
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
    this.deletingReply.set(null);
  }
}
