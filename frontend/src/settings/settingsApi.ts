import { api } from '../auth/api';

/** Private account data: only the owner sees it, on the settings page. */
export interface AccountInfo {
  email: string;
  emailVerified: boolean;
  provider: 'LOCAL' | 'GOOGLE' | string;
}

export const getAccount = () => api<AccountInfo>('/auth/account');

export const sendPasswordEmail = () => api<{ message: string }>('/auth/account/password-email', {});

export const deleteAccount = (confirmation: string) =>
  api<{ message: string }>('/auth/account', { confirmation }, 'DELETE');
