import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { login } from "../api/authApi";
import { useAuth } from "../context/useAuth";
import { setToken } from "../utils/token";
import LoginPage from "./LoginPage";
import RegisterPage from "./RegisterPage";
import { register } from "../api/authApi";
import { toast } from "sonner";

vi.mock("../api/authApi", () => ({
  login: vi.fn(),
  register: vi.fn(),
}));
vi.mock("../context/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("../utils/token", () => ({ setToken: vi.fn() }));
vi.mock("sonner", () => ({ toast: { error: vi.fn() } }));

const mockLogin = vi.mocked(login);
const mockRegister = vi.mocked(register);
const mockUseAuth = vi.mocked(useAuth);
const mockSetToken = vi.mocked(setToken);

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{`${location.pathname}${location.search}${location.hash}`}</output>;
}

function renderWithRoutes(
  page: React.ReactNode,
  initialEntry: string | { pathname: string; state?: unknown },
  pagePath = "/login",
) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path={pagePath} element={page} />
        <Route path="*" element={<LocationDisplay />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  const refreshUser = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    refreshUser.mockResolvedValue(undefined);
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
      isAuthenticated: false,
      refreshUser,
      logout: vi.fn(),
    });
  });

  it("authenticates, saves the token, refreshes the profile, and returns to the requested URL", async () => {
    mockLogin.mockResolvedValue({ token: "jwt-token" });
    renderWithRoutes(
      <LoginPage />,
      {
        pathname: "/login",
        state: { from: { pathname: "/messages/thread", search: "?page=2", hash: "#latest" } },
      },
    );

    fireEvent.change(screen.getByPlaceholderText("Enter username"), {
      target: { value: "ava" },
    });
    fireEvent.change(screen.getByPlaceholderText("Enter password"), {
      target: { value: "password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({ username: "ava", password: "password" });
      expect(mockSetToken).toHaveBeenCalledWith("jwt-token");
      expect(refreshUser).toHaveBeenCalledOnce();
      expect(screen.getByTestId("location")).toHaveTextContent(
        "/messages/thread?page=2#latest",
      );
    });
  });

  it.each([
    ["/", undefined],
    ["/", { from: { pathname: "//attacker.example" } }],
    ["/", { from: { pathname: "/login" } }],
    ["/", { from: { pathname: "/register" } }],
    ["/", { from: { pathname: "https://attacker.example" } }],
    ["/profile", { from: { pathname: "/profile", search: 1, hash: null } }],
  ])("validates redirect state and navigates to %s", async (expected, state) => {
    mockLogin.mockResolvedValue({ token: "jwt-token" });
    renderWithRoutes(<LoginPage />, { pathname: "/login", state });

    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent(expected),
    );
  });

  it("reports login errors and forwards state when opening registration", async () => {
    mockLogin.mockRejectedValue(new Error("Invalid credentials"));
    const state = { from: { pathname: "/profile" } };
    const { unmount } = renderWithRoutes(<LoginPage />, { pathname: "/login", state });

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));

    expect(screen.getByTestId("location")).toHaveTextContent("/register");
    unmount();

    renderWithRoutes(<LoginPage />, "/login");
    fireEvent.click(screen.getAllByRole("button", { name: "Sign in" })[0]);
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Invalid credentials"));
  });
});

describe("RegisterPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("submits registration details and navigates to login with the prior state", async () => {
    mockRegister.mockResolvedValue(undefined);
    const state = { from: { pathname: "/saved" } };
    const { container } = renderWithRoutes(
      <RegisterPage />,
      { pathname: "/register", state },
      "/register",
    );

    const fields = screen.getAllByRole("textbox");
    fireEvent.change(fields[0], { target: { value: "ava" } });
    fireEvent.change(fields[1], { target: { value: "ava@example.com" } });
    const passwordFields = container.querySelectorAll('input[type="password"]');
    fireEvent.change(passwordFields[0], {
      target: { value: "password" },
    });
    fireEvent.change(passwordFields[1], {
      target: { value: "password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        username: "ava",
        email: "ava@example.com",
        password: "password",
        confirmPassword: "password",
      });
      expect(screen.getByTestId("location")).toHaveTextContent("/login");
    });
  });

  it("reports registration failures and supports navigation to login", async () => {
    mockRegister.mockRejectedValue(new Error("Email already exists"));
    const { unmount } = renderWithRoutes(<RegisterPage />, "/register", "/register");

    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/login");
    unmount();

    const { container } = renderWithRoutes(<RegisterPage />, "/register", "/register");
    fireEvent.change(screen.getAllByRole("textbox")[0], {
      target: { value: "ava" },
    });
    fireEvent.change(screen.getAllByRole("textbox")[1], {
      target: { value: "ava@example.com" },
    });
    const passwordFields = container.querySelectorAll('input[type="password"]');
    fireEvent.change(passwordFields[0], {
      target: { value: "password" },
    });
    fireEvent.change(passwordFields[1], {
      target: { value: "password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Email already exists"),
    );
  });
});
