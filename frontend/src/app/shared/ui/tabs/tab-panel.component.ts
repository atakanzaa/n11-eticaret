import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-tab-panel',
  standalone: true,
  template: `
    <div class="panel" [hidden]="!active" role="tabpanel">
      <ng-content />
    </div>
  `,
  styles: [`:host { display: block; } .panel[hidden] { display: none; }`],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabPanelComponent {
  label = input.required<string>();
  disabled = input<boolean>(false);

  /** Managed by parent TabsComponent */
  active = false;
}
