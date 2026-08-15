import { Routes } from '@angular/router';
import { HomePageComponent } from './pages/home-page/home-page.component';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { MyPlacesComponent } from './pages/my-places/my-places.component';
import { adminGuard, authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    title: 'ŽEBRÁK – Mapa levných nákupů a outletů v ČR',
  },
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [adminGuard],
    title: 'Administrace – ŽEBRÁK',
  },
  {
    path: 'my-places',
    component: MyPlacesComponent,
    canActivate: [authGuard],
    title: 'Moje přidaná místa – ŽEBRÁK',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
