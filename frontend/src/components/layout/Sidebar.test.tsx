import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import Sidebar from "./Sidebar";

function LocationDisplay() {
  const location = useLocation();
  return (
    <output data-testid="location">
      {JSON.stringify({
        pathname: location.pathname,
        state: location.state,
      })}
    </output>
  );
}

function renderSidebar(
  isAuthenticated: boolean,
  path = "/search?term=posts#feed",
  onLogout = vi.fn(),
) {
  return {
    onLogout,
    ...render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="*"
            element={
              <>
                <Sidebar
                  isAuthenticated={isAuthenticated}
                  onLogout={onLogout}
                />
                <LocationDisplay />
              </>
            }
          />
          <Route path="/login" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>,
    ),
  };
}

describe("Sidebar", () => {
  it("keeps private navigation out of guest desktop and mobile navigation", () => {
    renderSidebar(false);

    expect(screen.getAllByRole("link", { name: "Home" })).toHaveLength(2);
    expect(screen.getAllByRole("link", { name: "Search" })).toHaveLength(2);
    expect(screen.getAllByRole("link", { name: "Notifications" })).toHaveLength(2);
    expect(screen.queryByRole("link", { name: "Messages" })).not.toBeInTheDocument();
  });

  it("shows authenticated mobile links and logs out to the login route", () => {
    const onLogout = vi.fn();
    renderSidebar(true, "/messages", onLogout);

    expect(screen.getAllByRole("link", { name: "Messages" })).toHaveLength(2);
    expect(screen.getAllByRole("link", { name: "Profile" })).toHaveLength(2);
    fireEvent.click(screen.getAllByRole("button", { name: "More" })[0]);
    fireEvent.click(screen.getAllByRole("button", { name: "Logout" })[0]);

    expect(onLogout).toHaveBeenCalledOnce();
    expect(JSON.parse(screen.getByTestId("location").textContent ?? "{}"))
      .toMatchObject({ pathname: "/login" });
  });

  it("preserves the current URL when a guest chooses Login", () => {
    renderSidebar(false);
    fireEvent.click(screen.getAllByRole("button", { name: "More" })[0]);
    fireEvent.click(screen.getAllByRole("button", { name: "Login" })[0]);

    expect(JSON.parse(screen.getByTestId("location").textContent ?? "{}"))
      .toMatchObject({
        pathname: "/login",
        state: {
          from: {
            pathname: "/search",
            search: "?term=posts",
            hash: "#feed",
          },
        },
      });
  });
});
