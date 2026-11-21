import { useQuery } from "@tanstack/react-query";
import { getUserDetails } from "../../../api/adminApi";

export function useGetUserDetails(publicId: string) {
    return useQuery({
        queryKey: ["admin-user", publicId],
        queryFn: () => getUserDetails(publicId),
        enabled: !!publicId,
 });
}