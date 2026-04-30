import { ChangeDetectionStrategy, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Renders Iyzico's 3DS challenge form. The HTML payload comes from the previous
 * route (`CheckoutComponent`) via `history.state.html`. The form auto-submits
 * itself and posts back to Iyzico's sandbox; once the user clears the bank
 * challenge, Iyzico calls `/api/payments/iyzico/callback`, which redirects the
 * browser to `/odeme/sonuc/:paymentId`.
 */
@Component({
  selector: 'sc-payment-challenge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe],
  template: `
    <div class="card">
      <h2>{{ 'checkout.paymentChallenge' | t }}</h2>
      <p class="muted">Banka 3D Secure ekranı yükleniyor…</p>
      <div #host class="host" [innerHTML]="safeHtml"></div>
    </div>
  `,
  styles: [
    `
      .card {
        max-width: 720px;
        margin: 32px auto;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 32px;
      }
      .host {
        margin-top: 16px;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class PaymentChallengeComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);

  @ViewChild('host', { static: false }) host?: ElementRef<HTMLDivElement>;

  safeHtml: SafeHtml = '';

  ngOnInit(): void {
    const html = (history.state?.html as string | undefined) ?? '';
    if (!html) {
      // No payload — user probably navigated here directly. Send them back to checkout.
      this.router.navigate(['/odeme']);
      return;
    }
    this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(html);

    // Iyzico's sandbox sometimes ships a `<script>document.forms[0].submit()</script>`
    // tail; Angular won't re-execute scripts injected via [innerHTML]. We auto-submit
    // the first form on the next tick as a fallback.
    setTimeout(() => this.maybeAutoSubmit(), 250);
  }

  private maybeAutoSubmit(): void {
    const firstForm = this.host?.nativeElement.querySelector('form');
    if (firstForm instanceof HTMLFormElement) {
      try {
        firstForm.submit();
      } catch {
        /* the bank's frame will handle submission once the user clicks through */
      }
    }
  }
}
