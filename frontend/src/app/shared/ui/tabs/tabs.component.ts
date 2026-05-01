import {
  AfterContentInit,
  ChangeDetectionStrategy,
  Component,
  ContentChildren,
  input,
  output,
  QueryList,
  signal,
} from '@angular/core';
import { TabPanelComponent } from './tab-panel.component';

@Component({
  selector: 'sc-tabs',
  standalone: true,
  templateUrl: './tabs.component.html',
  styleUrls: ['./tabs.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabsComponent implements AfterContentInit {
  activeIndex = input<number>(0);
  tabChange = output<number>();

  @ContentChildren(TabPanelComponent) panels!: QueryList<TabPanelComponent>;

  readonly selectedIndex = signal(0);

  ngAfterContentInit(): void {
    this.select(this.activeIndex());
  }

  select(index: number): void {
    this.selectedIndex.set(index);
    this.panels.forEach((p, i) => (p.active = i === index));
    this.tabChange.emit(index);
  }
}
