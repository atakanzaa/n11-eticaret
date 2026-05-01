import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'sc-star-rating',
  standalone: true,
  templateUrl: './star-rating.component.html',
  styleUrls: ['./star-rating.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StarRatingComponent {
  value = input.required<number>();
  max = input<number>(5);
  count = input<number | null>(null);
  size = input<'sm' | 'base' | 'lg'>('base');
  interactive = input<boolean>(false);

  ratingChange = output<number>();

  stars = computed(() => {
    const val = this.value();
    const m = this.max();
    const result: ('full' | 'half' | 'empty')[] = [];
    for (let i = 1; i <= m; i++) {
      if (val >= i) result.push('full');
      else if (val >= i - 0.5) result.push('half');
      else result.push('empty');
    }
    return result;
  });

  onStarClick(index: number): void {
    if (this.interactive()) {
      this.ratingChange.emit(index + 1);
    }
  }
}
