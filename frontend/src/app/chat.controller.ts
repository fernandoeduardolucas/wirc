import { Injectable } from '@angular/core';

export interface ChatMessage {
  user: string;
  message: string;
}

interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{ message: string }>;
}

@Injectable({ providedIn: 'root' })
export class ChatController {
  private readonly endpoint = 'http://localhost:8080/graphql';

  async loadMessages(): Promise<ChatMessage[]> {
    const response = await this.runQuery<{ messages: ChatMessage[] }>(
      'query { messages { user message } }'
    );
    return response.data?.messages ?? [];
  }

  async sendMessage(user: string, message: string): Promise<ChatMessage> {
    const response = await this.runQuery<{ sendMessage: ChatMessage }>(
      'mutation($user: String!, $message: String!) { sendMessage(user: $user, message: $message) { user message } }',
      { user, message }
    );

    if (!response.data?.sendMessage) {
      throw new Error('Resposta GraphQL inválida para sendMessage.');
    }

    return response.data.sendMessage;
  }

  private async runQuery<T>(query: string, variables: Record<string, string> = {}): Promise<GraphqlResponse<T>> {
    const raw = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables })
    });

    if (!raw.ok) {
      throw new Error(`Falha HTTP ${raw.status}: ${raw.statusText}`);
    }

    const payload = (await raw.json()) as GraphqlResponse<T>;

    if (payload.errors?.length) {
      throw new Error(payload.errors[0].message);
    }

    return payload;
  }
}
