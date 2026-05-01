import { Directive, input, TemplateRef, inject } from '@angular/core';

@Directive({
  selector: '[scTableCell]',
  standalone: true,
})
export class ScTableCellDirective {
  scTableCell = input.required<string>();
  readonly template = inject(TemplateRef);
}
