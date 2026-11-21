import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import Dialog from "./ui/Dialog";
import { DropdownMenu } from "./ui/DropdownMenu";
import AdminRoute from "./layout/adminRoute";
import { useAuth } from "../context/useAuth";
import type { ProfileResponse } from "../api/types";

vi.mock("../context/useAuth", () => ({ useAuth: vi.fn() }));
const mockUseAuth = vi.mocked(useAuth);

function LocationPath() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

function renderAdminRoute(auth: Partial<ReturnType<typeof useAuth>>) {
  mockUseAuth.mockReturnValue({
    user: null,
    loading: false,
    isAuthenticated: false,
    refreshUser: vi.fn(),
    logout: vi.fn(),
    ...auth,
  } as ReturnType<typeof useAuth>);

  return render(
    <MemoryRouter initialEntries={["/admin/users?search=ava#top"]}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin/users" element={<div>Admin content</div>} />
        </Route>
        <Route path="/login" element={<LocationPath />} />
        <Route path="/" element={<LocationPath />} />
      </Routes>
    </MemoryRouter>,
  );
}

const adminProfile = {
  role: "ADMIN",
  username: "admin",
} as ProfileResponse;

describe("Dialog", () => {
  it("renders an accessible modal and notifies when dismissed", () => {
    const onOpenChange = vi.fn();
    render(
      <Dialog open onOpenChange={onOpenChange} title="Edit post">
        <p>Dialog content</p>
      </Dialog>,
    );

    expect(screen.getByRole("dialog", { name: "Edit post" })).toBeInTheDocument();
    expect(screen.getByText("Dialog content")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button"));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("does not render closed dialog content", () => {
    render(
      <Dialog open={false} onOpenChange={vi.fn()} title="Hidden dialog">
        <p>Hidden content</p>
      </Dialog>,
    );

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});

describe("DropdownMenu", () => {
  it("toggles its menu, closes from an item, and responds to Escape", () => {
    render(
      <DropdownMenu label="More actions" trigger="Open">
        {({ close }) => (
          <button role="menuitem" type="button" onClick={close}>
            Close menu
          </button>
        )}
      </DropdownMenu>,
    );
    const trigger = screen.getByRole("button", { name: "More actions" });
    const menu = screen.getByRole("menu");

    expect(trigger).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(trigger);
    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(menu).toHaveClass("pointer-events-auto");

    fireEvent.keyDown(document, { key: "Escape" });
    expect(trigger).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(trigger);
    fireEvent.click(screen.getByRole("menuitem", { name: "Close menu" }));
    expect(trigger).toHaveAttribute("aria-expanded", "false");
  });

  it("closes on outside click and updates its position on viewport changes", () => {
    vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockReturnValue({
      x: 220,
      y: 100,
      width: 40,
      height: 40,
      top: 100,
      right: 260,
      bottom: 140,
      left: 220,
      toJSON: () => ({}),
    });
    render(
      <DropdownMenu label="More actions" trigger="Open">
        {() => <span>Menu item</span>}
      </DropdownMenu>,
    );
    const trigger = screen.getByRole("button", { name: "More actions" });
    const menu = screen.getByRole("menu");
    fireEvent.click(trigger);

    expect(menu).toHaveStyle({ left: "210px", top: "100px", position: "fixed" });
    fireEvent(window, new Event("resize"));
    fireEvent(window, new Event("scroll"));
    fireEvent.mouseDown(document.body);
    expect(trigger).toHaveAttribute("aria-expanded", "false");
  });

  it("places the menu above and to the left when there is not enough viewport space", () => {
    Object.defineProperty(window, "innerWidth", { configurable: true, value: 300 });
    Object.defineProperty(window, "innerHeight", { configurable: true, value: 200 });
    vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockReturnValue({
      x: 250,
      y: 180,
      width: 40,
      height: 20,
      top: 180,
      right: 290,
      bottom: 200,
      left: 250,
      toJSON: () => ({}),
    });
    render(
      <DropdownMenu label="More actions" trigger="Open">
        {() => <span>Menu item</span>}
      </DropdownMenu>,
    );
    fireEvent.click(screen.getByRole("button", { name: "More actions" }));

    expect(screen.getByRole("menu")).toHaveStyle({
      left: "30px",
      top: "3px",
      position: "fixed",
    });
  });
});

describe("AdminRoute", () => {
  it("shows a loading indicator while authentication is unresolved", () => {
    renderAdminRoute({ loading: true });
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("redirects guests to login while preserving the requested location", () => {
    renderAdminRoute({ loading: false, user: null });
    expect(screen.getByTestId("location")).toHaveTextContent("/login");
    expect(mockUseAuth).toHaveBeenCalled();
  });

  it("redirects non-admin users to the home route", () => {
    renderAdminRoute({
      loading: false,
      user: { ...adminProfile, role: "USER" },
    });
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });

  it("renders the protected page for administrators", () => {
    renderAdminRoute({ loading: false, user: adminProfile });
    expect(screen.getByText("Admin content")).toBeInTheDocument();
  });
});
