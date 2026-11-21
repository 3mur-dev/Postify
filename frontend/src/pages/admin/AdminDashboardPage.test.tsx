import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useGetAdminDashboard } from "../../hooks/admin/dashboard/useGetAdminDashboard";
import { adminDashboardFixture } from "../../test/adminDashboardFixture";
import AdminDashboardPage from "./AdminDashboardPage";

vi.mock("../../hooks/admin/dashboard/useGetAdminDashboard", () => ({
  useGetAdminDashboard: vi.fn(),
}));

vi.mock("../../features/admin/dashboard/AdminChart", () => ({
  default: ({ title }: { title: string }) => (
    <section aria-label={title}>{title}</section>
  ),
}));

const mockUseGetAdminDashboard = vi.mocked(useGetAdminDashboard);

function setDashboardQuery(
  result: Partial<ReturnType<typeof useGetAdminDashboard>>,
) {
  mockUseGetAdminDashboard.mockReturnValue({
    data: adminDashboardFixture,
    isLoading: false,
    isError: false,
    ...result,
  } as ReturnType<typeof useGetAdminDashboard>);
}

describe("AdminDashboardPage", () => {
  beforeEach(() => {
    setDashboardQuery({});
  });

  it("shows a loading state while dashboard data is being fetched", () => {
    setDashboardQuery({ data: undefined, isLoading: true });

    render(<AdminDashboardPage />);

    expect(screen.getByText("Loading dashboard...")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("shows an error state when the request fails", () => {
    setDashboardQuery({ data: undefined, isError: true });

    render(<AdminDashboardPage />);

    expect(screen.getByText("Failed to load dashboard.")).toBeInTheDocument();
  });

  it("shows an error state when no dashboard data is available", () => {
    setDashboardQuery({ data: undefined });

    render(<AdminDashboardPage />);

    expect(screen.getByText("Failed to load dashboard.")).toBeInTheDocument();
  });

  it("requests the default range with the viewer timezone and all dashboard sections", () => {
    render(<AdminDashboardPage />);

    expect(mockUseGetAdminDashboard).toHaveBeenCalledWith({
      rangePreset: "LAST_7_DAYS",
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      sections: [
        "OVERVIEW",
        "CHARTS",
        "SYSTEM_METRICS",
        "USER_METRICS",
        "ENGAGEMENT_METRICS",
      ],
    });
  });

  it("renders dashboard metrics and analytics when data is available", () => {
    render(<AdminDashboardPage />);

    expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByText("12,345")).toBeInTheDocument();
    expect(screen.getByText("User Activity")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Engagement", level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText("System Health")).toBeInTheDocument();
    expect(screen.getByLabelText("User Growth")).toBeInTheDocument();
    expect(screen.getByLabelText("Post Growth")).toBeInTheDocument();
  });

  it("reloads dashboard data when the selected range changes", () => {
    render(<AdminDashboardPage />);

    fireEvent.change(screen.getByRole("combobox", { name: "Dashboard range" }), {
      target: { value: "LAST_30_DAYS" },
    });

    expect(mockUseGetAdminDashboard).toHaveBeenLastCalledWith(
      expect.objectContaining({ rangePreset: "LAST_30_DAYS" }),
    );
  });
});
