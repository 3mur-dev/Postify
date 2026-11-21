import { useQuery } from "@tanstack/react-query";
import { getAdminDashboard } from "../../../api/adminApi";
import type { AdminDashboardRequest, AdminDashboardResponse } from "../../../api/types";

export function useGetAdminDashboard(params: AdminDashboardRequest) {
    return useQuery<AdminDashboardResponse>({
        queryKey: ["admin-dashboard", params],
        queryFn: () => getAdminDashboard(params),
        refetchInterval:1000,
        refetchIntervalInBackground: true,
        staleTime: 1000 * 60 * 5
    });
}