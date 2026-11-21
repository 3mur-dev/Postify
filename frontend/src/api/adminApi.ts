import type {
  AdminUser,
  AdminUserDetails,
  PagedResponse,
  UserSortField,
  AdminDashboardResponse,
  AdminDashboardRequest,
  ApiResponse
} from "./types";
import { privateApi } from "./axios";

export interface AdminUserParams {
  page?: number;
  size?: number;
  sortField?: UserSortField;
  direction?: "ASC" | "DESC";
  search?: string;
}

export async function getUsers({
    page = 0,
    size = 10,
    sortField = "CREATED_AT",
    direction = "DESC",
    search = "",

}: AdminUserParams  = {}): Promise<PagedResponse<AdminUser>> {
  const response = await privateApi.get<PagedResponse<AdminUser>>(
    "/admin/users",
    {
      params: {
        page,
        size,
        sortField,
        direction,
        search: search || undefined,

      },
    }
  );

  return response.data;
}

export async function getUserDetails(publicId: string): Promise<AdminUserDetails> {
  const response = await privateApi.get<ApiResponse<AdminUserDetails>>(`/admin/users/${publicId}`);

  return response.data.data;
}

export async function getAdminDashboard(
  params: AdminDashboardRequest
): Promise<AdminDashboardResponse> {
  const searchParams = new URLSearchParams();

  searchParams.append("rangePreset", params.rangePreset);
  searchParams.append("timezone", params.timezone);

  if (params.fromDate) {
    searchParams.append("fromDate", params.fromDate);
  }

  if (params.toDate) {
    searchParams.append("toDate", params.toDate);
  }

  params.sections.forEach((section) => {
    searchParams.append("sections", section);
  });

  const response = await privateApi.get<ApiResponse<AdminDashboardResponse>>(
    "/admin/dashboard",
    {
      params: searchParams,
    }
  );

  return response.data.data;
}