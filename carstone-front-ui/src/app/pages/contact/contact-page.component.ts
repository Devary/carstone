import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {InputTextModule} from 'primeng/inputtext';
import {Textarea} from 'primeng/textarea';
import {Button} from 'primeng/button';
import {MessageService} from 'primeng/api';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-contact-page',
  standalone: true,
  imports: [FormsModule, InputTextModule, Textarea, Button],
  templateUrl: './contact-page.component.html',
  styleUrl: './contact-page.component.scss',
})
export class ContactPageComponent {
  // plain mutable fields, NOT signals: [(ngModel)]'s banana-in-a-box syntax desugars to
  // [ngModel]="name" (ngModelChange)="name=$event" — assigning directly to a WritableSignal
  // reference like that doesn't set its value, it tries to reassign the signal itself, silently
  // breaking the whole form's data binding. Template-driven ngModel wants a plain property.
  protected name = '';
  protected email = '';
  protected message = '';
  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);

  private readonly http = inject(HttpClient);

  constructor(private readonly messages: MessageService) {
  }

  // POSTs to context-gen's own generic ContactFormResource (GET/POST /contact-form/{name}) —
  // carstone-front's MainContactForm declares this form's fields, MainContactFormHandler forwards
  // a valid submission as an email via Quarkus' own mailer extension. A 400 here means a required
  // field was blank (shouldn't happen — the button's own [disabled]="form.invalid" already
  // prevents that, this only covers a submission somehow reaching here anyway); a 501 means no
  // handler is registered at all (a deployment misconfiguration, not a user-facing input problem).
  protected submit(): void {
    this.submitting.set(true);
    this.http.post(`${environment.apiUrl}contact-form/main`, {
      name: this.name,
      email: this.email,
      message: this.message,
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
        this.messages.add({severity: 'success', summary: 'Message sent', detail: 'We will get back to you shortly.'});
        this.name = '';
        this.email = '';
        this.message = '';
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Message not sent',
          detail: typeof error.error === 'string' ? error.error : 'Something went wrong — please try again.',
        });
      },
    });
  }
}
