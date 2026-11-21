import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import AdminChart from "./AdminChart";
import AdminEngagementMetrics from "./AdminEngagementMetrics";
import AdminOverviewCards from "./AdminOverviewCard";
import AdminSystemMetrics from "./AdminSystemMetrics";
import AdminUserMetrics from "./AdminUserMetrics";
import { adminDashboardFixture } from "../../../test/adminDashboardFixture";

vi.mock("recharts", () => {
  const passthrough = ({ children }: { children?: ReactNode }) => (
    <div>{children}</div>
  );
  const tooltip = ({
    formatter,
  }: {
    formatter?: (value: number | string) => [number | string, string];
  }) => <div>{formatter?.(17).join(" - ")}</div>;

  return {
    ResponsiveContainer: passthrough,
    LineChart: passthrough,
    Line: passthrough,
    XAxis: passthrough,
    YAxis: passthrough,
    Tooltip: tooltip,
    CartesianGrid: passthrough,
  };
});

describe("admin dashboard metric components", () => {
  it("renders every overview total using localized numbers", () => {
    render(<AdminOverviewCards overview={adminDashboardFixture.overview} />);

    expect(screen.getByText("Total Users")).toBeInTheDocument();
    expect(screen.getByText((12_345).toLocaleString())).toBeInTheDocument();
    expect(screen.getByText((8_765).toLocaleString())).toBeInTheDocument();
    expect(screen.getByText((4_321).toLocaleString())).toBeInTheDocument();
    expect(screen.getByText((9_876).toLocaleString())).toBeInTheDocument();
    expect(screen.getByText((54_321).toLocaleString())).toBeInTheDocument();
  });

  it("renders user activity counts and percentage precision", () => {
    render(<AdminUserMetrics users={adminDashboardFixture.users} />);

    expect(screen.getByText("New Users")).toBeInTheDocument();
    expect(screen.getByText("567")).toBeInTheDocument();
    expect(screen.getByText("Banned Users")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("Suspended Users")).toBeInTheDocument();
    expect(screen.getByText("34")).toBeInTheDocument();
    expect(screen.getByText("12.3%")).toBeInTheDocument();
  });

  it("renders engagement counts, averages, and rate", () => {
    render(<AdminEngagementMetrics engagement={adminDashboardFixture.engagement} />);

    expect(screen.getByText("Likes Received")).toBeInTheDocument();
    expect(screen.getByText("1,234")).toBeInTheDocument();
    expect(screen.getByText("Comments Received")).toBeInTheDocument();
    expect(screen.getByText("567")).toBeInTheDocument();
    expect(screen.getByText("8.25")).toBeInTheDocument();
    expect(screen.getByText("2.50")).toBeInTheDocument();
    expect(screen.getByText("6.75%")).toBeInTheDocument();
  });

  it("renders system usage, threshold colors, and formatted uptime", () => {
    const { container } = render(
      <AdminSystemMetrics system={adminDashboardFixture.system} />,
    );

    expect(screen.getByText("42.5%")).toBeInTheDocument();
    expect(screen.getByText("72.2%")).toBeInTheDocument();
    expect(screen.getByText("91.5%")).toBeInTheDocument();
    expect(screen.getByText("2d 3h 4m")).toBeInTheDocument();

    const progressBars = container.querySelectorAll("div[style]");
    expect(progressBars[0]).toHaveStyle({ width: "42.5%" });
    expect(progressBars[0]).toHaveClass("bg-emerald-400");
    expect(progressBars[1]).toHaveClass("bg-amber-500");
    expect(progressBars[2]).toHaveClass("bg-rose-500");
  });

  it("clamps system progress bars to the 0-100 range", () => {
    const { container } = render(
      <AdminSystemMetrics
        system={{ ...adminDashboardFixture.system, cpuUsage: 105, memoryUsage: -5 }}
      />,
    );

    const progressBars = container.querySelectorAll("div[style]");
    expect(progressBars[0]).toHaveStyle({ width: "100%" });
    expect(progressBars[1]).toHaveStyle({ width: "0%" });
  });
});

describe("AdminChart", () => {
  it("renders its title, description, and empty-range message", () => {
    render(
      <AdminChart
        title="Weekly activity"
        description="Activity over the selected period."
        data={[]}
        valueLabel="Events"
      />,
    );

    expect(screen.getByRole("heading", { name: "Weekly activity" })).toBeInTheDocument();
    expect(screen.getByText("Activity over the selected period.")).toBeInTheDocument();
    expect(screen.getByText("No data for the selected range.")).toBeInTheDocument();
  });

  it("formats chart values with the provided label", () => {
    render(
      <AdminChart
        title="Weekly activity"
        data={[{ label: "Mon", value: 4 }]}
        valueLabel="Posts"
      />,
    );

    expect(screen.getByRole("heading", { name: "Weekly activity" })).toBeInTheDocument();
    expect(screen.queryByText("No data for the selected range.")).not.toBeInTheDocument();
    expect(screen.getByText("17 - Posts")).toBeInTheDocument();
  });

  it("uses the default value label when none is provided", () => {
    render(<AdminChart title="Weekly activity" data={[{ label: "Mon", value: 4 }]} />);

    expect(screen.getByText("17 - Value")).toBeInTheDocument();
  });
});
