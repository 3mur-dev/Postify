import { useState } from "react";
import {
  LayoutDashboard,
  Users,
  FileText,
  Flag,
  ArrowLeft,
  X,
  Menu,
} from "lucide-react";
import { NavLink } from "react-router-dom";

const navItems = [
  {
    label: "Dashboard",
    to: "/admin",
    icon: LayoutDashboard,
  },
  {
    label: "Users",
    to: "/admin/users",
    icon: Users,
  },
  {
    label: "Posts",
    to: "/admin/posts",
    icon: FileText,
  },
  {
    label: "Reports",
    to: "/admin/reports",
    icon: Flag,
  },
];

export default function AdminSidebar() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <div className="lg:hidden">
        <div className="relative z-50 flex items-center justify-between border-b border-slate-800 bg-slate-950 px-4 py-4 text-white">
          <div>
            <h1 className="text-lg font-semibold">Postify Admin</h1>
            <p className="text-xs text-slate-400">Manage users, posts, and reports</p>
          </div>

          <button
            type="button"
            onClick={() => setIsOpen((current) => !current)}
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-800 bg-slate-900 text-slate-300 transition hover:bg-slate-800"
            aria-label="Toggle admin navigation"
            aria-expanded={isOpen}
          >
            {isOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        <div
          className={`fixed inset-0 z-40 bg-slate-950/70 transition duration-200 ${
            isOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0"
          }`}
          onClick={() => setIsOpen(false)}
          aria-hidden={!isOpen}
        />

        <aside
          className={`fixed inset-y-0 left-0 w-full max-w-[22rem] transform overflow-y-auto border-r border-slate-800 bg-slate-950 p-4 transition duration-300 ${
            isOpen
              ? "z-50 translate-x-0 pointer-events-auto"
              : "z-0 -translate-x-full pointer-events-none"
          }`}
        >
          <div className="mb-6 px-3">
            <h1 className="text-xl font-bold text-white">Postify</h1>
            <p className="mt-1 text-xs text-slate-500">Admin Panel</p>
          </div>

          <nav className="flex-1 space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;

              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/admin"}
                  onClick={() => setIsOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                      isActive
                        ? "bg-fuchsia-600/15 text-fuchsia-400"
                        : "text-slate-400 hover:bg-slate-800/60 hover:text-white"
                    }`
                  }
                >
                  <Icon size={18} />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>

          <div className="mt-4 border-t border-slate-800 pt-4">
            <NavLink
              to="/"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-400 transition hover:bg-slate-800/60 hover:text-white"
            >
              <ArrowLeft size={18} />
              <span>Back to Postify</span>
            </NavLink>
          </div>
        </aside>
      </div>

      <aside className="hidden lg:flex lg:h-screen lg:w-64 lg:flex-col lg:border-r lg:border-slate-800 lg:bg-slate-950 lg:p-4">
        <div className="mb-8 px-3 lg:mb-8">
          <h1 className="text-xl font-bold text-white">Postify</h1>
          <p className="mt-1 text-xs text-slate-500">Admin Panel</p>
        </div>

        <nav className="flex-1 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/admin"}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                    isActive
                      ? "bg-fuchsia-600/15 text-fuchsia-400"
                      : "text-slate-400 hover:bg-slate-800/60 hover:text-white"
                  }`
                }
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="mt-4 border-t border-slate-800 pt-4 lg:mt-auto">
          <NavLink
            to="/"
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-400 transition hover:bg-slate-800/60 hover:text-white"
          >
            <ArrowLeft size={18} />
            <span>Back to Postify</span>
          </NavLink>
        </div>
      </aside>
    </>
  );
}
