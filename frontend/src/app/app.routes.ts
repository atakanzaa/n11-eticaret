import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';

export const routes: Routes = [
  // Buyer storefront — public + (account subtree gated by authGuard)
  {
    path: '',
    loadComponent: () =>
      import('./features/buyer/buyer-shell.component').then(m => m.BuyerShellComponent),
    children: [
      { path: '', loadComponent: () => import('./features/buyer/home/home.component').then(m => m.HomeComponent) },
      { path: 'arama', loadComponent: () => import('./features/buyer/search/search.component').then(m => m.SearchComponent) },
      { path: 'kategori/:slug', loadComponent: () => import('./features/buyer/search/search.component').then(m => m.SearchComponent) },
      { path: 'urun/:id', loadComponent: () => import('./features/buyer/product/product.component').then(m => m.ProductComponent) },
      { path: 'sepet', canActivate: [authGuard], loadComponent: () => import('./features/buyer/cart/cart.component').then(m => m.CartComponent) },
      { path: 'odeme', canActivate: [authGuard], loadComponent: () => import('./features/buyer/checkout/checkout.component').then(m => m.CheckoutComponent) },
      { path: 'odeme/3ds/:paymentId', canActivate: [authGuard], loadComponent: () => import('./features/buyer/payment/challenge/challenge.component').then(m => m.PaymentChallengeComponent) },
      { path: 'odeme/sonuc/:paymentId', canActivate: [authGuard], loadComponent: () => import('./features/buyer/payment/result/result.component').then(m => m.PaymentResultComponent) },
      { path: 'siparis-tamamlandi/:id', canActivate: [authGuard], loadComponent: () => import('./features/buyer/order-success/order-success.component').then(m => m.OrderSuccessComponent) },
      { path: 'asistan', canActivate: [authGuard], loadComponent: () => import('./features/buyer/ai-chat-fullscreen/ai-chat-fullscreen.component').then(m => m.AiChatFullscreenComponent) },
      {
        path: 'hesap',
        canActivate: [authGuard],
        loadComponent: () => import('./features/buyer/account/account-shell.component').then(m => m.AccountShellComponent),
        children: [
          { path: '', redirectTo: 'siparislerim', pathMatch: 'full' },
          { path: 'siparislerim', loadComponent: () => import('./features/buyer/account/orders/orders.component').then(m => m.OrdersComponent) },
          { path: 'siparis/:id', loadComponent: () => import('./features/buyer/account/order-detail/order-detail.component').then(m => m.OrderDetailComponent) },
          { path: 'iade/:id', loadComponent: () => import('./features/buyer/account/return/return.component').then(m => m.ReturnComponent) },
          { path: 'profil', loadComponent: () => import('./features/buyer/account/profile/profile.component').then(m => m.ProfileComponent) },
        ],
      },
    ],
  },

  // Auth pages — own minimal shell (no shopping header)
  {
    path: '',
    loadComponent: () => import('./features/auth/auth-shell.component').then(m => m.AuthShellComponent),
    children: [
      { path: 'giris', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
      { path: 'kayit', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
      { path: 'yetkisiz', loadComponent: () => import('./features/auth/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent) },
    ],
  },

  // Seller dashboard — gated by SELLER role
  {
    path: 'satici',
    canActivate: [roleGuard(['SELLER'])],
    loadComponent: () =>
      import('./features/seller/seller-shell.component').then(m => m.SellerShellComponent),
    children: [
      { path: '', loadComponent: () => import('./features/seller/dashboard/dashboard.component').then(m => m.SellerDashboardComponent) },
      { path: 'urunler', loadComponent: () => import('./features/seller/products/products.component').then(m => m.SellerProductsComponent) },
      { path: 'siparisler', loadComponent: () => import('./features/seller/orders/seller-orders.component').then(m => m.SellerOrdersComponent) },
    ],
  },

  // Admin console — gated by ADMIN role
  {
    path: 'admin',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/admin/admin-shell.component').then(m => m.AdminShellComponent),
    children: [
      { path: '', loadComponent: () => import('./features/admin/overview/overview.component').then(m => m.AdminOverviewComponent) },
      { path: 'fraud', loadComponent: () => import('./features/admin/fraud/fraud.component').then(m => m.AdminFraudComponent) },
      { path: 'ai-kullanim', loadComponent: () => import('./features/admin/ai-usage/ai-usage.component').then(m => m.AdminAiUsageComponent) },
      { path: 'promosyonlar', loadComponent: () => import('./features/admin/promotions/promotions.component').then(m => m.AdminPromotionsComponent) },
    ],
  },

  { path: '**', redirectTo: '' },
];
