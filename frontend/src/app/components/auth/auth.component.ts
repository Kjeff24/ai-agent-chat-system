import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  mode: 'login' | 'register' = 'login';
  error: string | null = null;
  loading = false;
  loginForm!: FormGroup;
  registerForm!: FormGroup;
  showLoginPassword = signal(false);
  showRegPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    public themeService: ThemeService
  ) {
    this.loginForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
    this.registerForm = this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  setMode(m: 'login' | 'register'): void {
    this.mode = m;
    this.error = null;
    this.loginForm.reset();
    this.registerForm.reset();
  }

  onSubmitLogin(): void {
    if (this.loginForm.invalid) return;
    this.error = null;
    this.loading = true;
    const { email, password } = this.loginForm.getRawValue();
    this.authService.login(email, password).subscribe({
      next: () => {
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error ?? 'Login failed. Please check your credentials.';
      }
    });
  }

  onSubmitRegister(): void {
    if (this.registerForm.invalid) return;
    this.error = null;
    this.loading = true;
    const { name, email, password } = this.registerForm.getRawValue();
    this.authService.register(name, email, password).subscribe({
      next: () => {
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error ?? 'Registration failed. Email may already be in use.';
      }
    });
  }
}
