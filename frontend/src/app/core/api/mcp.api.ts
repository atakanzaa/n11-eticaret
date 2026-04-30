import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface McpTool {
  name: string;
  description?: string;
  inputSchema?: Record<string, unknown>;
}

export interface McpInvokeResponse {
  success: boolean;
  data?: unknown;
  error?: string;
}

@Injectable({ providedIn: 'root' })
export class McpApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/mcp`;

  listTools(): Observable<McpTool[]> {
    return this.http.get<McpTool[]>(`${this.base}/tools`);
  }

  invokeTool(name: string, args: Record<string, unknown>): Observable<McpInvokeResponse> {
    return this.http.post<McpInvokeResponse>(`${this.base}/tools/${name}/invoke`, args);
  }
}
