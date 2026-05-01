import {
  AfterContentInit,
  ChangeDetectionStrategy,
  Component,
  ContentChildren,
  input,
  output,
  QueryList,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { SpinnerComponent } from '../spinner/spinner.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { ScTableCellDirective } from './table-cell.directive';

export interface TableColumn {
  key: string;
  label: string;
  width?: string;
}

@Component({
  selector: 'sc-data-table',
  standalone: true,
  imports: [NgTemplateOutlet, SpinnerComponent, EmptyStateComponent],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTableComponent implements AfterContentInit {
  columns = input.required<TableColumn[]>();
  rows = input.required<any[]>();
  trackByKey = input<string>('id');
  loading = input<boolean>(false);
  emptyMessage = input<string>('Kayit bulunamadi');

  rowClick = output<any>();

  @ContentChildren(ScTableCellDirective) cellTemplates!: QueryList<ScTableCellDirective>;

  private templateMap = new Map<string, ScTableCellDirective>();

  ngAfterContentInit(): void {
    this.cellTemplates.forEach(t => this.templateMap.set(t.scTableCell(), t));
  }

  getTemplate(key: string): ScTableCellDirective | undefined {
    return this.templateMap.get(key);
  }

  getCellValue(row: any, key: string): any {
    return row[key];
  }
}
