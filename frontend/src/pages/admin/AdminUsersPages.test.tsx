import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AdminUser, AdminUserDetails, PagedResponse } from "../../api/types";
import { useGetUsers } from "../../hooks/admin/users/useGetUsers";
import { useGetUserDetails } from "../../hooks/admin/users/useGetUsersDetails";
import AdminUserTable from "../../features/admin/users/AdminUserTable";
import AdminUsersPage from "./AdminUsersPage";
import AdminUserDetailsPage from "./AdminUsersDetailsPage";

vi.mock("../../hooks/admin/users/useGetUsers", () => ({ useGetUsers: vi.fn() }));
vi.mock("../../hooks/admin/users/useGetUsersDetails", () => ({
  useGetUserDetails: vi.fn(),
}));

const mockUseGetUsers = vi.mocked(useGetUsers);
const mockUseGetUserDetails = vi.mocked(useGetUserDetails);
const users: AdminUser[] = [
  {
    publicId: "user-1",
    username: "ava",
    email: "ava@example.com",
    status: "ACTIVE",
    createdAt: "2025-01-01T00:00:00Z",
  },
  {
    publicId: "user-2",
    username: "sam",
    email: "sam@example.com",
    status: "SUSPENDED",
    createdAt: "2025-02-01T00:00:00Z",
  },
  {
    publicId: "user-3",
    username: "lee",
    email: "lee@example.com",
    status: "BANNED",
    createdAt: "2025-03-01T00:00:00Z",
  },
];

function renderUsersPage() {
  return render(
    <MemoryRouter initialEntries={["/admin/users"]}>
      <Routes>
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/users/:publicId" element={<div>User details route</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function renderDetailsPage() {
  return render(
    <MemoryRouter initialEntries={["/admin/users/user-1"]}>
      <Routes>
        <Route path="/admin/users/:publicId" element={<AdminUserDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function setUsersResult(
  result: Partial<ReturnType<typeof useGetUsers>>,
) {
  mockUseGetUsers.mockReturnValue({
    data: {
      content: users,
      totalElements: 3,
      totalPages: 2,
      last: false,
    } as PagedResponse<AdminUser>,
    isLoading: false,
    isError: false,
    ...result,
  } as ReturnType<typeof useGetUsers>);
}

const userDetails = {
  ...users[0],
  bio: "Hello from Ava",
  avatarUrl: null,
  followersCount: 1234,
  followingCount: 25,
  postsCount: 14,
  role: "USER",
} as AdminUserDetails;

describe("AdminUsersPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setUsersResult({});
  });

  it("renders loading and error states without a user table", () => {
    setUsersResult({ isLoading: true });
    const { rerender } = renderUsersPage();
    expect(screen.getByText("Loading users...")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();

    setUsersResult({ isError: true });
    rerender(
      <MemoryRouter initialEntries={["/admin/users"]}>
        <Routes>
          <Route path="/admin/users" element={<AdminUsersPage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Failed to load users.")).toBeInTheDocument();
  });

  it("searches from the first page, paginates, and opens selected users", async () => {
    renderUsersPage();
    expect(screen.getByText("3 users")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() =>
      expect(mockUseGetUsers).toHaveBeenLastCalledWith({
        search: "",
        page: 1,
        size: 10,
      }),
    );

    fireEvent.change(screen.getByPlaceholderText("Search users..."), {
      target: { value: "ava" },
    });
    await waitFor(() =>
      expect(mockUseGetUsers).toHaveBeenLastCalledWith({
        search: "ava",
        page: 0,
        size: 10,
      }),
    );

    fireEvent.click(screen.getAllByRole("button", { name: "View" })[0]);
    expect(screen.getByText("User details route")).toBeInTheDocument();
  });

  it("renders an empty table when user data is not available", () => {
    setUsersResult({ data: undefined });
    renderUsersPage();
    expect(screen.getByText("0 users")).toBeInTheDocument();
    expect(screen.getAllByText("No users found.")).toHaveLength(2);
    expect(screen.queryByRole("button", { name: "Next" })).not.toBeInTheDocument();
  });
});

describe("AdminUserTable", () => {
  it("renders users, status labels, localized dates, and row actions", () => {
    const onSelectUser = vi.fn();
    render(<AdminUserTable users={users} onSelectUser={onSelectUser} />);

    expect(screen.getAllByText("@ava")).toHaveLength(2);
    expect(screen.getAllByText("ava@example.com")).toHaveLength(2);
    expect(screen.getAllByText("ACTIVE")).toHaveLength(2);
    expect(screen.getAllByText("SUSPENDED")).toHaveLength(2);
    expect(screen.getAllByText("BANNED")).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: "View" })).toHaveLength(6);
    fireEvent.click(screen.getAllByRole("button", { name: "View" })[0]);
    expect(onSelectUser).toHaveBeenCalledWith("user-1");
  });

  it("shows the empty state in both responsive table layouts", () => {
    render(<AdminUserTable users={[]} onSelectUser={vi.fn()} />);
    expect(screen.getAllByText("No users found.")).toHaveLength(2);
  });
});

describe("AdminUserDetailsPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows loading and error states", () => {
    mockUseGetUserDetails.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    } as ReturnType<typeof useGetUserDetails>);
    const { rerender } = renderDetailsPage();
    expect(screen.getByText("Loading user...")).toBeInTheDocument();
    expect(mockUseGetUserDetails).toHaveBeenCalledWith("user-1");

    mockUseGetUserDetails.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as ReturnType<typeof useGetUserDetails>);
    rerender(
      <MemoryRouter initialEntries={["/admin/users/user-1"]}>
        <Routes>
          <Route path="/admin/users/:publicId" element={<AdminUserDetailsPage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Failed to load user.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Users" })).toHaveAttribute(
      "href",
      "/admin/users",
    );
  });

  it("renders active user details, stats, profile actions, and optional bio fallback", () => {
    mockUseGetUserDetails.mockReturnValue({
      data: userDetails,
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useGetUserDetails>);
    const { rerender } = renderDetailsPage();

    expect(screen.getByRole("heading", { name: "User Details" })).toBeInTheDocument();
    expect(screen.getAllByText("ava@example.com")).toHaveLength(2);
    expect(screen.getByText("1,234")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Suspend" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Ban" })).toBeInTheDocument();
    expect(screen.getByText("Hello from Ava")).toBeInTheDocument();

    mockUseGetUserDetails.mockReturnValue({
      data: { ...userDetails, bio: null, status: "BANNED" },
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useGetUserDetails>);
    rerender(
      <MemoryRouter initialEntries={["/admin/users/user-1"]}>
        <Routes>
          <Route path="/admin/users/:publicId" element={<AdminUserDetailsPage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("This user has not added a bio.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Suspend" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ban" })).not.toBeInTheDocument();
  });
});
