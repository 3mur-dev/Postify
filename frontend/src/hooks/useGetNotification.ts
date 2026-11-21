import {useQuery} from "@tanstack/react-query";
import {getNotifications} from "../api/notification";

export function useGetNotifications() {
    return useQuery({
        queryKey: ["notifications"],
        queryFn: () => getNotifications(),
    });

}
