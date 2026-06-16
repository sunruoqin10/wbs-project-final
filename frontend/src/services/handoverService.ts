// PM / Dept-PM 交接 Service(2026-06 PM/Dept-PM 变更方案 实施)
//
// 本文件独立封装一组 typed HTTP verbs(put/get),用于访问后端
// /users/{id}/handover、/orgs/dept-merge、/users/{id}/handover-preview、
// /users/{id}/project-handover-history、/projects/needs-handover 等交接相关接口。
//
// 注: 本仓库的 services/api.ts 只导出 apiService(class 形式,无通用 .put/.get verbs),
// 而本任务的 plan spec 要求 put/get 形态的 service 入口,故在文件内自建一个最小
// request 包装,与 apiService 保持同样的 Result 解包、Authorization / X-User-Id /
// X-User-Role 头注入语义,以便后续切换到统一的底层 client 时只改一处。

import { useUserStore } from '@/stores/user';
import type {
  HandoverRequest,
  DeptPmHandoverRequest,
  DeptMergeRequest,
  PmHandoverResponse,
  DeptPmHandoverResponse,
  DeptMergeResponse,
  HandoverPreviewResponse,
  ProjectHandoverLogDTO,
  PageResult,
  HandoverType,
} from '@/types/handover';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export class HandoverApiError extends Error {
  status: number;
  data: unknown;

  constructor(message: string, status: number, data: unknown = null) {
    super(message);
    this.name = 'HandoverApiError';
    this.status = status;
    this.data = data;
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  body?: unknown;
  params?: Record<string, string | number | undefined | null>;
  silent?: boolean;
}

async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, silent } = options;

  // 拼 query string
  let url = `${API_BASE_URL}${endpoint}`;
  if (params) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        search.append(k, String(v));
      }
    });
    const qs = search.toString();
    if (qs) url += `?${qs}`;
  }

  // 注入鉴权头(对齐 apiService.request 的语义)
  const userStore = useUserStore();
  const isLoginEndpoint = endpoint === '/users/login';
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (userStore.token && !isLoginEndpoint) {
    headers['Authorization'] = `Bearer ${userStore.token}`;
  }
  if (!isLoginEndpoint) {
    if (userStore.currentUserId) {
      headers['X-User-Id'] = userStore.currentUserId;
    }
    if (userStore.currentUser?.role) {
      headers['X-User-Role'] = userStore.currentUser.role;
    }
  }

  const fetchOptions: RequestInit = { method, headers };
  if (body !== undefined) {
    fetchOptions.body = JSON.stringify(body);
  }

  const response = await fetch(url, fetchOptions);
  if (!response.ok) {
    let errorMessage = `HTTP error! status: ${response.status}`;
    let errorData: unknown = null;
    try {
      const text = await response.text();
      try {
        const parsed = JSON.parse(text) as { message?: string; error?: string; data?: unknown };
        errorData = parsed;
        errorMessage = parsed.message || parsed.error || errorMessage;
      } catch (_) {
        // 非 JSON, 保留默认错误
      }
    } catch (_) {
      // ignore
    }
    if (!silent) {
      console.error(`handoverService request failed: ${endpoint}`, errorMessage);
    }
    throw new HandoverApiError(errorMessage, response.status, errorData);
  }

  const result = (await response.json()) as Result<T>;
  if (result.code !== 200) {
    throw new HandoverApiError(result.message || 'API request failed', response.status, result.data);
  }
  return result.data;
}

// 暴露 plan spec 要求的 verb 形式,供 service / store 调用
const handoverHttp = {
  get: <T>(endpoint: string, opts: { params?: RequestOptions['params']; silent?: boolean } = {}) =>
    request<T>(endpoint, { method: 'GET', params: opts.params, silent: opts.silent }),
  put: <T>(endpoint: string, body: unknown, opts: { params?: RequestOptions['params']; silent?: boolean } = {}) =>
    request<T>(endpoint, { method: 'PUT', body, params: opts.params, silent: opts.silent }),
  post: <T>(endpoint: string, body: unknown, opts: { params?: RequestOptions['params']; silent?: boolean } = {}) =>
    request<T>(endpoint, { method: 'POST', body, params: opts.params, silent: opts.silent }),
  delete: <T>(endpoint: string, opts: { params?: RequestOptions['params']; silent?: boolean } = {}) =>
    request<T>(endpoint, { method: 'DELETE', params: opts.params, silent: opts.silent }),
};

export interface HandoverHistoryOptions {
  page?: number;
  pageSize?: number;
  handoverType?: HandoverType;
  startDate?: string;
  endDate?: string;
}

export const handoverService = {
  /**
   * 通用交接入口(对 caller 不透明于具体类型时使用)。
   * 后端按 body.handoverType 分发到 PM / Dept-PM 处理。
   */
  handover(userId: string, req: HandoverRequest | DeptPmHandoverRequest): Promise<unknown> {
    return handoverHttp.put<unknown>(`/users/${userId}/handover`, req);
  },

  /** PM 交接(单用户 → 后继 PM) */
  handoverPm(userId: string, req: HandoverRequest): Promise<PmHandoverResponse> {
    return handoverHttp.put<PmHandoverResponse>(`/users/${userId}/handover`, req);
  },

  /** Dept-PM 交接(单用户 → 后继 Dept-PM, 批量部门) */
  handoverDeptPm(userId: string, req: DeptPmHandoverRequest): Promise<DeptPmHandoverResponse> {
    return handoverHttp.put<DeptPmHandoverResponse>(`/users/${userId}/handover`, req);
  },

  /** 部门合并(旧部门并入新部门, 项目重派) */
  mergeDept(req: DeptMergeRequest): Promise<DeptMergeResponse> {
    return handoverHttp.put<DeptMergeResponse>('/orgs/dept-merge', req);
  },

  /** 交接预览: 离任者信息 + 候选项目 / 部门 */
  preview(userId: string, type: HandoverType): Promise<HandoverPreviewResponse> {
    return handoverHttp.get<HandoverPreviewResponse>(`/users/${userId}/handover-preview`, {
      params: { type },
    });
  },

  /** 某用户的项目交接历史(分页) */
  history(userId: string, opts: HandoverHistoryOptions = {}): Promise<PageResult<ProjectHandoverLogDTO>> {
    return handoverHttp.get<PageResult<ProjectHandoverLogDTO>>(
      `/users/${userId}/project-handover-history`,
      { params: opts as Record<string, string | number | undefined | null> },
    );
  },

  /** 列出所有 needsHandover=true 的项目(供 dashboard / ProjectList 醒目提示) */
  listNeedsHandover(): Promise<unknown[]> {
    return handoverHttp.get<unknown[]>('/projects/needs-handover');
  },
};

export default handoverService;
