import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {Textarea} from 'primeng/textarea';
import {Button} from 'primeng/button';
import {MessageService} from 'primeng/api';

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

  constructor(private readonly messages: MessageService) {
  }

  // no backend endpoint exists for this yet (a real submission target is a separate, later
  // concern) — this is the page/UX the user asked for, submitting locally confirms the flow
  protected submit(): void {
    this.submitted.set(true);
    this.messages.add({severity: 'success', summary: 'Message sent', detail: 'We will get back to you shortly.'});
    this.name = '';
    this.email = '';
    this.message = '';
  }
}
