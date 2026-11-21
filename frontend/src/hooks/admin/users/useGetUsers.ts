import { useQuery } from "@tanstack/react-query";

import { getUsers } from "../../../api/adminApi";
import { type AdminUserParams } from "../../../api/adminApi";

export function useGetUsers(params: AdminUserParams = {}) {
    return useQuery({
        queryKey: ["admin-users", params],
        queryFn: () => getUsers(params),
 });
}
