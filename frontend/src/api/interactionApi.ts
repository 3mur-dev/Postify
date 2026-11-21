import { privateApi } from "./axios";
import type {
  TrackInteractionRequest
} from "./types";

export async function trackInteraction(
  request: TrackInteractionRequest
): Promise<void> {
  await privateApi.post("/interactions", request);
}