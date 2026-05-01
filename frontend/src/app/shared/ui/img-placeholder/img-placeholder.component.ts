import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Striped placeholder used everywhere a product image hasn't been uploaded yet. */
@Component({
  selector: 'sc-img-placeholder',
  standalone: true,
  templateUrl: './img-placeholder.component.html',
  styleUrls: ['./img-placeholder.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImgPlaceholderComponent {
  label = input<string>('gorsel');
  ratio = input<string>('1 / 1');
  tint = input<string | null>(null);

  bg = computed(() => {
    const t = this.tint() || 'var(--sc-surface-2)';
    return `repeating-linear-gradient(45deg, rgba(0,0,0,0.04) 0 6px, transparent 6px 12px), ${t}`;
  });
}
