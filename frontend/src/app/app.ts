import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Shell } from './core/layout/shell';

@Component({
  selector: 'bf-root',
  imports: [Shell],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<bf-shell />`,
})
export class App {}
