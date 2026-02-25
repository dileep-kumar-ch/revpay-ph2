import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-recovery',
  templateUrl: './recovery.component.html',
  styleUrls: ['./recovery.component.css']
})
export class RecoveryComponent implements OnInit {
  emailForm!: FormGroup;
  questionsForm!: FormGroup;
  step: 'EMAIL' | 'QUESTIONS' | 'SUCCESS' = 'EMAIL';
  questions: string[] = [];
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.questionsForm = this.fb.group({
      answers: this.fb.array([]),
      newPassword: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onEmailSubmit(): void {
    if (this.emailForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    const email = this.emailForm.value.email;

    this.authService.getRecoveryQuestions(email).subscribe({
      next: (questions) => {
        this.questions = questions;
        this.questionsForm = this.fb.group({
          answers: this.fb.array(questions.map(q => this.fb.group({
            question: [q],
            answer: ['', Validators.required]
          }))),
          newPassword: ['', [Validators.required, Validators.minLength(6)]]
        });
        this.step = 'QUESTIONS';
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Could not find account with that email.';
        this.loading = false;
      }
    });
  }

  get answers() {
    return this.questionsForm.get('answers') as any;
  }

  onResetSubmit(): void {
    if (this.questionsForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';

    const { answers, newPassword } = this.questionsForm.value;
    const email = this.emailForm.value.email;

    this.authService.resetPasswordWithQuestions(email, answers, newPassword).subscribe({
      next: () => {
        this.step = 'SUCCESS';
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Verification failed. Please check your answers.';
        this.loading = false;
      }
    });
  }
}
