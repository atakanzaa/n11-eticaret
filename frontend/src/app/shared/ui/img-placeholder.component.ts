import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Striped placeholder used everywhere a product image hasn't been uploaded yet. */
@Component({
  selector: 'sc-img-placeholder',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="ph" [style.background]="bg()" [style.aspectRatio]="ratio()">{{ label() }}</div>`,
  styles: [
    `
      .ph {
        display: grid;
        place-items: center;
        color: var(--sc-text-faint);
        font-size: 12px;
        border-radius: var(--sc-radius);
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }
    `,
  ],
})
export class ImgPlaceholderComponent {
  label = input<string>('görsel');
  ratio = input<string>('1 / 1');
  tint = input<string | null>(null);

  bg = computed(() => {
    const t = this.tint() || 'var(--sc-surface-2)';
    return `repeating-linear-gradient(45deg, rgba(0,0,0,0.04) 0 6px, transparent 6px 12px), ${t}`;
  });
}
