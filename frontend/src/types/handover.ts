// PM / Dept-PM 交接相关类型(2026-06 PM/Dept-PM 变更方案 实施)

/** 交接类型: PM 交接 或 Dept-PM 交接 */
export type HandoverType = 'PM_HANDOVER' | 'DEPT_PM_HANDOVER';

/** PM 交接请求: 单个用户把所辖项目交给他人 */
export interface HandoverRequest {
  handoverType: 'PM_HANDOVER';
  successorUserId: string;
  projectIds?: string[];
  reason?: string;
}

/** Dept-PM 交接请求: 单个用户把所辖部门(及部门下项目)交给他人 */
export interface DeptPmHandoverRequest {
  handoverType: 'DEPT_PM_HANDOVER';
  successorUserId: string;
  deptCodes: string[];
  reason?: string;
}

/** 部门合并请求: 旧部门并入新部门,旧部门项目重派给新部门负责人 */
export interface DeptMergeRequest {
  oldDeptCode: string;
  newDeptCode: string;
  successorDeptManagerId: string;
  reason?: string;
}

/** PM 交接响应 */
export interface PmHandoverResponse {
  handoveredCount: number;
  projectIds: string[];
}

/** Dept-PM 交接响应 */
export interface DeptPmHandoverResponse {
  handoveredDeptCount: number;
  deptCodes: string[];
}

/** 部门合并响应 */
export interface DeptMergeResponse {
  affectedProjectCount: number;
  newDeptCode: string;
}

/** 交接预览中的项目简述 */
export interface ProjectBrief {
  id: string;
  name: string;
  status: string;
  deptCode: string;
  previousOwnerId: string | null;
  needsHandover: boolean;
}

/** 交接预览响应: 离任者信息 + 可交接项目/部门列表 */
export interface HandoverPreviewResponse {
  outgoing: {
    userId: string;
    name: string;
    role: string;
    deptCode: string;
  };
  candidateProjects: ProjectBrief[];
  candidateDeptCodes: string[];
}

/** 项目交接历史日志 DTO(后端 project_handover_log 表 JOIN) */
export interface ProjectHandoverLogDTO {
  id: number;
  projectId: string;
  projectName: string;
  handoverType: HandoverType | string;
  fromUserId: string;
  fromUserName: string;
  toUserId: string;
  toUserName: string;
  fromDeptCode: string;
  toDeptCode: string;
  reason: string;
  operatorId: string;
  operatorName: string;
  createdAt: string;
}

/** 通用分页结果 */
export interface PageResult<T> {
  total: number;
  records: T[];
}
