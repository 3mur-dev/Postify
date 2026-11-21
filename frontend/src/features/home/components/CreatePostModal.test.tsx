import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useCreatePost } from "../../../hooks/usePost";
import { CreatePostModal } from "./CreatePostModal";

vi.mock("../../../hooks/usePost", () => ({ useCreatePost: vi.fn() }));

const mockUseCreatePost = vi.mocked(useCreatePost);
const mutate = vi.fn();
const user = { username: "ava", avatarUrl: null } as never;
const createObjectURL = vi.fn();
const revokeObjectURL = vi.fn();

function renderModal(open = true, onOpenChange = vi.fn()) {
  return render(
    <CreatePostModal open={open} onOpenChange={onOpenChange} user={user} />,
  );
}

describe("CreatePostModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseCreatePost.mockReturnValue({ mutate, isPending: false } as never);
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: createObjectURL.mockImplementation(
        (file: File) => `blob:${file.name}`,
      ),
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: revokeObjectURL,
    });
  });

  afterEach(() => {
    Reflect.deleteProperty(URL, "createObjectURL");
    Reflect.deleteProperty(URL, "revokeObjectURL");
  });

  it("does not show closed dialogs and disables posting when there is no content", () => {
    const onOpenChange = vi.fn();
    const { rerender } = renderModal(false, onOpenChange);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    rerender(<CreatePostModal open onOpenChange={onOpenChange} user={user} />);
    expect(screen.getByRole("dialog", { name: "Create Post" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();

    fireEvent.change(screen.getByPlaceholderText("What's happening?"), {
      target: { value: "   " },
    });
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();
  });

  it("trims and submits post content, then closes and resets after success", async () => {
    const onOpenChange = vi.fn();
    renderModal(true, onOpenChange);
    const textarea = screen.getByPlaceholderText("What's happening?");
    fireEvent.change(textarea, { target: { value: "  Hello Postify  " } });
    fireEvent.click(screen.getByRole("button", { name: "Post" }));

    expect(mutate).toHaveBeenCalledWith(
      { content: "  Hello Postify  ", media: [] },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
    const onSuccess = mutate.mock.calls[0][1]?.onSuccess;
    act(() => onSuccess?.());
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(textarea).toHaveValue("");
  });

  it("limits text length and renders the character warning counter", () => {
    renderModal();
    const textarea = screen.getByPlaceholderText("What's happening?");
    fireEvent.change(textarea, { target: { value: "x".repeat(1100) } });

    expect(textarea).toHaveValue("x".repeat(1000));
    expect(screen.getByText("0")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post" })).toBeEnabled();
  });

  it("selects multiple image and video files, caps media at ten, removes previews, and revokes URLs", () => {
    const { unmount } = renderModal();
    const fileInput = document.querySelector<HTMLInputElement>('input[type="file"]');
    expect(fileInput).not.toBeNull();
    if (!fileInput) {
      throw new Error("Expected the post media input to be rendered.");
    }
    const files = Array.from({ length: 11 }, (_, index) =>
      new File([`file-${index}`], `media-${index}.${index === 0 ? "mp4" : "png"}`, {
        type: index === 0 ? "video/mp4" : "image/png",
      }),
    );
    fireEvent.change(fileInput, { target: { files } });

    expect(screen.getByText("10/10")).toBeInTheDocument();
    expect(document.querySelector("video")).toHaveAttribute("src", "blob:media-0.mp4");
    expect(screen.getAllByAltText("Post preview")).toHaveLength(9);
    expect(fileInput.value).toBe("");

    fireEvent.click(screen.getAllByRole("button", { name: "Remove" })[0]);
    expect(screen.getByText("9/10")).toBeInTheDocument();
    expect(revokeObjectURL).toHaveBeenCalled();

    unmount();
    expect(revokeObjectURL).toHaveBeenCalledTimes(19);
  });

  it("forwards pending state and lets the dialog close without submitting", () => {
    mockUseCreatePost.mockReturnValue({ mutate, isPending: true } as never);
    const onOpenChange = vi.fn();
    renderModal(true, onOpenChange);
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
