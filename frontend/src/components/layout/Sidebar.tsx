import {
  House,
  Search,
  Bell,
  Bookmark,
  Settings,
  LogIn,
  LogOut,
  MoreHorizontal,
  MessageCircle,
} from "lucide-react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { DropdownMenu } from "../ui/DropdownMenu";
import Avatar from "../../features/home/components/Avatar";

type SidebarProps = {
  avatarUrl?: string | null;
  isAuthenticated: boolean;
  onLogout: () => void;
};

const navButtonBaseClass =
  "flex h-10 w-10 items-center justify-center rounded-full border border-slate-800 bg-slate-950/90 text-slate-300 shadow-sm transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-700 hover:bg-slate-900 hover:text-white focus:outline-none focus:ring-2 focus:ring-fuchsia-500/60 active:scale-[0.98]";
const navButtonActiveClass =
  "border-slate-700 bg-white text-slate-950 shadow-lg shadow-fuchsia-950/20";
const tooltipClass =
  "pointer-events-none absolute left-full top-1/2 ml-3 hidden -translate-y-1/2 whitespace-nowrap rounded-full bg-slate-900/95 px-2.5 py-1 text-xs font-medium text-white shadow-lg ring-1 ring-white/10 opacity-0 transition-opacity duration-200 group-hover:visible group-hover:opacity-100 group-focus-within:opacity-100 lg:invisible";

const getNavButtonClasses = (isActive = false) =>
  `${navButtonBaseClass} ${isActive ? navButtonActiveClass : ""}`.trim();

export default function Sidebar({ avatarUrl, isAuthenticated, onLogout }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogin = () => {
    navigate("/login", {
      state: {
        from: {
          pathname: location.pathname,
          search: location.search,
          hash: location.hash,
        },
      },
    });
  };

  const primaryLinks = [
    { to: "/", label: "Home", icon: <House size={18} /> },
    { to: "/search", label: "Search", icon: <Search size={18} /> },
    ...(isAuthenticated
      ? [{ to: "/messages", label: "Messages", icon: <MessageCircle size={18} /> }]
      : []),
  ];

  const profileLink = {
    to: "/profile",
    label: "Profile",
    icon: (
      <Avatar
        imageUrl={avatarUrl}
        name="Profile"
        className="h-6 w-6 border border-slate-600"
      />
    ),
  };

  const renderMobileNavItem = (to: string, label: string, icon: React.ReactNode) => (
    <NavLink
      key={label}
      to={to}
      aria-label={label}
      className={({ isActive: linkActive }) =>
        `flex h-11 w-11 items-center justify-center rounded-full border border-slate-800 bg-slate-900/80 text-slate-300 transition-all duration-200 hover:border-slate-700 hover:bg-slate-800 hover:text-white ${linkActive ? navButtonActiveClass : ""}`.trim()
      }
    >
      {icon}
    </NavLink>
  );

  return (
    <>
      <aside className="sticky top-0 z-40 hidden h-screen w-20 min-w-[5rem] shrink-0 flex-col justify-between border-r border-slate-800 bg-slate-950/95 px-2.5 py-4 text-white backdrop-blur md:flex">
        <div className="flex flex-col items-center gap-4">
          <div className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-800 bg-slate-900/80 text-sm font-semibold text-white shadow-sm">
            Postify
          </div>

          <nav className="flex flex-col items-center gap-2 overflow-visible pb-0">
            {primaryLinks.map(({ to, label, icon }) => (
              <div key={label} className="group relative shrink-0">
                <NavLink
                  to={to}
                  aria-label={label}
                  className={({ isActive }) => getNavButtonClasses(isActive)}
                >
                  {icon}
                </NavLink>
                <span className={tooltipClass}>{label}</span>
              </div>
            ))}

            <div className="group relative shrink-0">
              <NavLink
                to="/notifications"
                aria-label="Notifications"
                className={({ isActive }) => getNavButtonClasses(isActive)}
              >
                <Bell size={18} />
              </NavLink>
              <span className={tooltipClass}>Notifications</span>
            </div>

            {isAuthenticated && (
              <div className="group relative shrink-0">
                <NavLink
                  to={profileLink.to}
                  aria-label={profileLink.label}
                  className={({ isActive }) => getNavButtonClasses(isActive)}
                >
                  {profileLink.icon}
                </NavLink>
                <span className={tooltipClass}>{profileLink.label}</span>
              </div>
            )}
          </nav>
        </div>

        <div className="mt-auto flex justify-center">
          <div className="group relative">
            <DropdownMenu label="More" trigger={<MoreHorizontal size={20} />}>
              {({ close }) => (
                <div className="flex flex-col py-2">
                  {isAuthenticated ? (
                    <>
                      <NavLink
                        to="/profile/me/saved"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Bookmark size={18} />
                        Saved Posts
                      </NavLink>

                      <NavLink
                        to="/settings"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Settings size={18} />
                        Settings
                      </NavLink>

                      <button
                        type="button"
                        onClick={() => {
                          onLogout();
                          close();
                          navigate("/login", { replace: true });
                        }}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-red-400 transition hover:bg-slate-900"
                      >
                        <LogOut size={18} />
                        Logout
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={() => {
                          handleLogin();
                          close();
                        }}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <LogIn size={18} />
                        Login
                      </button>

                      <NavLink
                        to="/settings"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Settings size={18} />
                        Settings
                      </NavLink>
                    </>
                  )}
                </div>
              )}
            </DropdownMenu>

            <span className={tooltipClass}>More</span>
          </div>
        </div>
      </aside>

      <div className="fixed inset-x-0 bottom-0 z-40 block border-t border-slate-800 bg-slate-950/95 px-2 py-2 backdrop-blur md:hidden">
        <div className="mx-auto flex max-w-lg items-center justify-between gap-1">
          {primaryLinks.map(({ to, label, icon }) => renderMobileNavItem(to, label, icon))}
          <NavLink
            to="/notifications"
            aria-label="Notifications"
            className={({ isActive }) =>
              `flex h-10 w-10 items-center justify-center rounded-full border border-slate-800 bg-slate-900/80 text-slate-300 transition-all duration-200 hover:border-slate-700 hover:bg-slate-800 hover:text-white ${isActive ? navButtonActiveClass : ""}`.trim()
            }
          >
            <Bell size={18} />
          </NavLink>
          {isAuthenticated ? renderMobileNavItem(profileLink.to, profileLink.label, profileLink.icon) : null}
          <div className="group relative">
            <DropdownMenu label="More" trigger={<MoreHorizontal size={18} />}>
              {({ close }) => (
                <div className="flex flex-col py-2">
                  {isAuthenticated ? (
                    <>
                      <NavLink
                        to="/profile/me/saved"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Bookmark size={18} />
                        Saved Posts
                      </NavLink>

                      <NavLink
                        to="/settings"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Settings size={18} />
                        Settings
                      </NavLink>

                      <button
                        type="button"
                        onClick={() => {
                          onLogout();
                          close();
                          navigate("/login", { replace: true });
                        }}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-red-400 transition hover:bg-slate-900"
                      >
                        <LogOut size={18} />
                        Logout
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={() => {
                          handleLogin();
                          close();
                        }}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <LogIn size={18} />
                        Login
                      </button>

                      <NavLink
                        to="/settings"
                        onClick={close}
                        className="flex items-center gap-3 px-4 py-3 text-sm text-slate-200 transition hover:bg-slate-900"
                      >
                        <Settings size={18} />
                        Settings
                      </NavLink>
                    </>
                  )}
                </div>
              )}
            </DropdownMenu>
          </div>
        </div>
      </div>
    </>
  );
}