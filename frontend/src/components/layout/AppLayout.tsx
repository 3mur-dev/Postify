import { Outlet } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import Sidebar from "./Sidebar";
import { useAuth } from "../../context/useAuth";
import { MessagingProvider } from "../../context/MessagingContext";
import { getMyProfile } from "../../api/profileApi";
import { hasToken } from "../../utils/token";

export default function AppLayout() {

  const { user, logout } = useAuth();
  const { data: profile } = useQuery({
    queryKey: ["myProfile"],
    queryFn: getMyProfile,
    enabled: hasToken(),
  });

  return (
    <MessagingProvider
      key={user?.username ?? "guest"}
      authenticated={!!user}
    >
      <div className="flex min-h-screen bg-slate-950 text-white">
        <Sidebar
          avatarUrl={profile?.avatarUrl}
          isAuthenticated={!!user}
          onLogout={logout}
        />

        <main className="min-w-0 flex-1 overflow-x-hidden px-2 py-2 pb-20 sm:px-4 sm:py-4 sm:pb-6 lg:px-5 lg:py-5">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </MessagingProvider>
  );
}