import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useMessaging } from "../../hooks/useMessaging";
import MessageComposer from "./MessageComposer";

vi.mock("../../hooks/useMessaging", () => ({ useMessaging: vi.fn() }));

const mockUseMessaging = vi.mocked(useMessaging);

describe("MessageComposer", () => {
  const sendMessage = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseMessaging.mockReturnValue({
      sendMessage,
      connectionState: "connected",
      connectionError: null,
      pendingMessages: [],
      participantPresence: {},
      requestPresence: vi.fn(),
      retryMessage: vi.fn(),
      resolvePendingMessage: vi.fn(),
    });
  });

  it("sends on Enter and supports Shift+Enter without submitting", () => {
    render(<MessageComposer conversationPublicId="thread-1" />);
    const composer = screen.getByRole("textbox", { name: "Write a message" });
    fireEvent.change(composer, { target: { value: "Hello" } });
    fireEvent.keyDown(composer, { key: "Enter", shiftKey: true });
    expect(sendMessage).not.toHaveBeenCalled();

    fireEvent.keyDown(composer, { key: "Enter", shiftKey: false });
    expect(sendMessage).toHaveBeenCalledWith("thread-1", "Hello");
    expect(composer).toHaveValue("");
  });

  it("keeps the draft and reports queue errors until the user edits it", () => {
    sendMessage.mockImplementationOnce(() => {
      throw new Error("Message exceeds the allowed length");
    });
    render(<MessageComposer conversationPublicId="thread-1" />);
    const composer = screen.getByRole("textbox", { name: "Write a message" });
    fireEvent.change(composer, { target: { value: "Not sent" } });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Message exceeds the allowed length",
    );
    expect(composer).toHaveValue("Not sent");

    fireEvent.change(composer, { target: { value: "Try again" } });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    expect(sendMessage).toHaveBeenLastCalledWith("thread-1", "Try again");
    expect(composer).toHaveValue("");
  });
});
