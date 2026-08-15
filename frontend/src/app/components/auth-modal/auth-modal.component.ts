import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div class="modal-card">
        <!-- Header & Tabs -->
        <div class="modal-header">
          <div class="tab-switcher">
            <button
              class="tab-btn"
              [class.active]="isLoginTab()"
              (click)="isLoginTab.set(true); errorMessage.set(null)"
            >
              Přihlášení
            </button>
            <button
              class="tab-btn"
              [class.active]="!isLoginTab()"
              (click)="isLoginTab.set(false); errorMessage.set(null)"
            >
              Registrace
            </button>
          </div>
          <button class="close-btn" (click)="close.emit()">✕</button>
        </div>

        <div class="modal-body">
          <!-- Error alert -->
          <div *ngIf="errorMessage()" class="alert-error">
            {{ errorMessage() }}
          </div>

          <!-- Login Form -->
          <form *ngIf="isLoginTab()" [formGroup]="loginForm" (ngSubmit)="submitLogin()">
            <div class="form-group">
              <label class="form-label">E-mail</label>
              <input
                type="email"
                class="input-field"
                placeholder="vas@email.cz"
                formControlName="email"
              />
              <div *ngIf="loginForm.get('email')?.touched && loginForm.get('email')?.invalid" class="field-error">
                Zadejte platný e-mail
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Heslo</label>
              <input
                type="password"
                class="input-field"
                placeholder="Vaše heslo"
                formControlName="password"
              />
              <div *ngIf="loginForm.get('password')?.touched && loginForm.get('password')?.invalid" class="field-error">
                Zadejte heslo
              </div>
            </div>

            <button
              type="submit"
              class="btn btn-primary submit-btn"
              [disabled]="loginForm.invalid || isLoading()"
            >
              {{ isLoading() ? 'Přihlašuji...' : 'Přihlásit se' }}
            </button>
          </form>

          <!-- Register Form -->
          <form *ngIf="!isLoginTab()" [formGroup]="registerForm" (ngSubmit)="submitRegister()">
            <div class="form-group">
              <label class="form-label">Vaše jméno nebo přezdívka</label>
              <input
                type="text"
                class="input-field"
                placeholder="např. Honza Lovec Slev"
                formControlName="nickname"
              />
              <div *ngIf="registerForm.get('nickname')?.touched && registerForm.get('nickname')?.invalid" class="field-error">
                Přezdívka musí mít 2 až 50 znaků
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">E-mail</label>
              <input
                type="email"
                class="input-field"
                placeholder="vas@email.cz"
                formControlName="email"
              />
              <div *ngIf="registerForm.get('email')?.touched && registerForm.get('email')?.invalid" class="field-error">
                Zadejte platný e-mail
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Heslo (minimálně 6 znaků)</label>
              <input
                type="password"
                class="input-field"
                placeholder="••••••••"
                formControlName="password"
              />
              <div *ngIf="registerForm.get('password')?.touched && registerForm.get('password')?.invalid" class="field-error">
                Heslo musí mít alespoň 6 znaků
              </div>
            </div>

            <button
              type="submit"
              class="btn btn-primary submit-btn"
              [disabled]="registerForm.invalid || isLoading()"
            >
              {{ isLoading() ? 'Registruji...' : 'Vytvořit účet zdarma' }}
            </button>
          </form>
        </div>

        <div class="modal-footer">
          <p class="footer-hint">
            Výchozí administrátorský účet: <code>admin&#64;zebrak.cz</code> / heslo: <code>admin123</code>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-color);
    }
    .tab-switcher {
      display: flex;
      background: var(--bg-app);
      padding: 4px;
      border-radius: var(--radius-md);
      gap: 4px;
    }
    .tab-btn {
      border: none;
      background: transparent;
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--text-muted);
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: all var(--transition-fast);
    }
    .tab-btn.active {
      background: #ffffff;
      color: var(--primary-600);
      box-shadow: var(--shadow-sm);
    }
    .close-btn {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      color: var(--text-muted);
      cursor: pointer;
      padding: 0.25rem;
      line-height: 1;
    }
    .close-btn:hover {
      color: var(--text-main);
    }
    .modal-body {
      padding: 1.5rem;
    }
    .form-group {
      margin-bottom: 1.15rem;
    }
    .form-label {
      display: block;
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-main);
      margin-bottom: 0.35rem;
    }
    .field-error {
      color: #dc2626;
      font-size: 0.75rem;
      font-weight: 500;
      margin-top: 0.25rem;
    }
    .alert-error {
      background-color: #fee2e2;
      color: #991b1b;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      font-size: 0.875rem;
      margin-bottom: 1.25rem;
      border: 1px solid #fecaca;
    }
    .submit-btn {
      width: 100%;
      padding: 0.75rem;
      margin-top: 0.5rem;
    }
    .modal-footer {
      padding: 1rem 1.5rem;
      background: var(--bg-app);
      border-top: 1px solid var(--border-color);
      border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    }
    .footer-hint {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-align: center;
    }
    .footer-hint code {
      background: #e2e8f0;
      padding: 2px 6px;
      border-radius: 4px;
      color: #0f172a;
    }
  `],
})
export class AuthModalComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  @Output() close = new EventEmitter<void>();
  @Output() loggedIn = new EventEmitter<void>();

  readonly isLoginTab = signal(true);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  registerForm = this.fb.group({
    nickname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }

  submitLogin(): void {
    if (this.loginForm.invalid) return;
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const val = this.loginForm.value;
    this.authService
      .login({ email: val.email!, password: val.password! })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.loggedIn.emit();
          this.close.emit();
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.error?.message || 'Nesprávný e-mail nebo heslo.'
          );
        },
      });
  }

  submitRegister(): void {
    if (this.registerForm.invalid) return;
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const val = this.registerForm.value;
    this.authService
      .register({
        nickname: val.nickname!,
        email: val.email!,
        password: val.password!,
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.loggedIn.emit();
          this.close.emit();
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.error?.message || 'Registrace se nezdařila. Zkontrolujte údaje.'
          );
        },
      });
  }
}
