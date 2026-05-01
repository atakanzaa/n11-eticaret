import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';

export interface TimelineEvent {
  label: string;
  detail?: string;
  date: string;
  status?: 'done' | 'active' | 'pending';
}

@Component({
  selector: 'sc-timeline',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './timeline.component.html',
  styleUrls: ['./timeline.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelineComponent {
  events = input.required<TimelineEvent[]>();
}
