import { Injectable } from '@angular/core';
import { AppError } from '../models/chat.models';

interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{
    message: string;
    extensions?: {
      code?: string;
      details?: AppError['details'];
    };
  }>;
}

@Injectable({ providedIn: 'root' })
export class GraphqlApiService {
  private readonly endpoint = 'http://localhost:8080/wirc';

  async runQuery<T>(query: string, variables: Record<string, string | boolean | string[]> = {}): Promise<GraphqlResponse<T>> {
    const raw = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables })
    });

    if (!raw.ok) {
      throw this.createError(`Falha HTTP ${raw.status}: ${raw.statusText}`);
    }

    const payload = (await raw.json()) as GraphqlResponse<T>;
    if (payload.errors?.length) {
      const firstError = payload.errors[0];
      throw this.createError(firstError.message, firstError.extensions?.code, firstError.extensions?.details);
    }

    return payload;
  }

  createError(message: string, code?: string, details?: AppError['details']): AppError {
    return { message, code, details };
  }
}
