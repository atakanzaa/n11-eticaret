import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'sc-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
  /** 0-based page number (Spring Data convention) */
  currentPage = input.required<number>();
  totalPages = input.required<number>();
  isFirst = input<boolean>(true);
  isLast = input<boolean>(true);

  pageChange = output<number>();

  prev(): void {
    const p = this.currentPage();
    if (p > 0) this.pageChange.emit(p - 1);
  }

  next(): void {
    const p = this.currentPage();
    if (p < this.totalPages() - 1) this.pageChange.emit(p + 1);
  }
}
