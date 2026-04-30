export type RoleName = 'CUSTOMER' | 'SELLER' | 'ADMIN' | 'SUPPORT';

export interface UserDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: RoleName[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserDto;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: RoleName[];
}

export interface RefreshRequest {
  refreshToken: string;
}
