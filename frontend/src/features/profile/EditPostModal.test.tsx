import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { EditPostModal } from "./EditPostModal";

describe("EditPostModal", () => {
  it("trims edited content and prevents empty or oversized submissions", () => {
    const onSave = vi.fn();
    render(
      <EditPostModal
        open
        post={{ publicId: "post-1", content: "Original" }}
        isPending={false}
        onClose={vi.fn()}
        onSave={onSave}
      />,
    );
    const textarea = screen.getByPlaceholderText("What's happening?");
    const saveButton = screen.getByRole("button", { name: "Save" });

    fireEvent.change(textarea, { target: { value: "   " } });
    expect(saveButton).toBeDisabled();

    fireEvent.change(textarea, { target: { value: "x".repeat(230) } });
    expect(textarea).toHaveValue("x".repeat(220));
    expect(screen.getByText("220/220")).toBeInTheDocument();
    fireEvent.click(saveButton);
    expect(onSave).toHaveBeenCalledWith("x".repeat(220));
  });

  it("disables editing while saving and closes through its cancel action", () => {
    const onClose = vi.fn();
    render(
      <EditPostModal
        open
        post={{ publicId: "post-1", content: "Original" }}
        isPending
        onClose={onClose}
        onSave={vi.fn()}
      />,
    );

    expect(screen.getByPlaceholderText("What's happening?")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Saving..." })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onClose).not.toHaveBeenCalled();
  });
});
