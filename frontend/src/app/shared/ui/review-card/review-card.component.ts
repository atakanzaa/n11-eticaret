import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { StarRatingComponent } from '../star-rating/star-rating.component';

@Component({
  selector: 'sc-review-card',
  standalone: true,
  imports: [StarRatingComponent, DatePipe],
  templateUrl: './review-card.component.html',
  styleUrls: ['./review-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewCardComponent {
  author = input.required<string>();
  rating = input.required<number>();
  date = input.required<string>();
  title = input<string>('');
  comment = input<string>('');
  verified = input<boolean>(false);
  helpfulCount = input<number>(0);
}
