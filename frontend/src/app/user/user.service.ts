import { Injectable, inject } from '@angular/core';
import { Observable, defer, from, of } from 'rxjs';
import { AppUser } from '../shared/chat.types';
import { GraphqlApiService } from '../shared/graphql-api.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly graphqlApi = inject(GraphqlApiService);

  loadUsers(activeUser: string): Observable<AppUser[]> {
    if (!activeUser) {
      return of([]);
    }

    return defer(() => from(
      this.graphqlApi.runQuery<{ users: AppUser[] }>('query { users { username displayName } }')
        .then((response) => response.data?.users ?? [])
    ));
  }

  createUser(displayName: string, password: string): Observable<AppUser> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ createUser: AppUser }>(
        'mutation($displayName: String!, $password: String!) { createUser(displayName: $displayName, password: $password) { username displayName } }',
        { displayName, password }
      ).then((response) => {
        if (!response.data?.createUser) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para createUser.');
        }
        return response.data.createUser;
      })
    ));
  }

  signIn(user: string, password: string): Observable<AppUser> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ signIn: AppUser }>(
        'mutation($user: String!, $password: String!) { signIn(user: $user, password: $password) { username displayName } }',
        { user, password }
      ).then((response) => {
        if (!response.data?.signIn) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para signIn.');
        }
        return response.data.signIn;
      })
    ));
  }
}
