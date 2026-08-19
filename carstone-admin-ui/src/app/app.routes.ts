import {Routes} from '@angular/router';
import {EntityPageComponent} from 'crudstone';
import {AdminShellComponent} from './shell/admin-shell.component';

export const routes: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {path: '', redirectTo: 'listings', pathMatch: 'full'},
      {path: ':entity', component: EntityPageComponent},
    ],
  },
];
