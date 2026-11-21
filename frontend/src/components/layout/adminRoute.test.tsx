import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAuth } from "../../context/useAuth";
import AdminRoute from "./adminRoute";

vi.mock("../../context/useAuth", () => ({ useAuth: vi.fn() }));

const mockUseAuth = vi.mocked(useAuth);

function LoginLocation() {
  const location = useLocation();
  return (
    <output data-testid="login-state">
      {JSON.stringify(location.state)}
    </output>
  );
}

function renderProtectedRoute(path = "/admin") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin/*" element={<div>Admin dashboard</div>} />
        </Route>
        <Route path="/" element={<div>Public home</div>} />
        <Route path="/login" element={<LoginLocation />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("AdminRoute", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows a loading state while authentication is unresolved", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: true,
      isAuthenticated: false,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderProtectedRoute();
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("redirects guests to login while preserving the requested admin URL", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
      isAuthenticated: false,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderProtectedRoute("/admin/users?filter=active#top");

    expect(JSON.parse(screen.getByTestId("login-state").textContent ?? "{}"))
      .toMatchObject({
        from: {
          pathname: "/admin/users",
          search: "?filter=active",
          hash: "#top",
        },
      });
  });

  it("redirects authenticated non-admin users and renders the route for admins", () => {
    mockUseAuth.mockReturnValue({
      user: { publicId: "user-1", username: "ava", role: "USER" } as never,
      loading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    const { unmount } = renderProtectedRoute();
    expect(screen.getByText("Public home")).toBeInTheDocument();
    unmount();

    mockUseAuth.mockReturnValue({
      user: { publicId: "admin-1", username: "admin", role: "ADMIN" } as never,
      loading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderProtectedRoute();
    expect(screen.getByText("Admin dashboard")).toBeInTheDocument();
  });
});
