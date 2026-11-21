import { Outlet } from "react-router-dom";
import AdminSidebar from "./adminSidebar";

export default function AdminLayout() {
  return (
    <div className="flex flex-col lg:flex-row min-h-screen bg-slate-950 text-white">
      <AdminSidebar />
      <main className="min-w-0 flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-7xl p-6 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}