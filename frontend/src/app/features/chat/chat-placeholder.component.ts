import { Component } from '@angular/core';

@Component({
  selector: 'app-chat-placeholder',
  standalone: true,
  template: `
    <div class="placeholder">
      <p class="placeholder__text">Chat will appear here</p>
    </div>
  `,
  styles: [`
    .placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding: var(--spacing-8);
    }

    .placeholder__text {
      color: var(--color-text-tertiary);
      font-size: var(--font-size-base);
    }
  `],
})
export class ChatPlaceholderComponent {}
