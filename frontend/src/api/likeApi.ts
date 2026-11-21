import { privateApi } from "./axios";
import type { InteractionSource } from "./types";

export async function togglePostLike({
  publicId,
  source,
}: {
  publicId: string;
  source: InteractionSource;
}): Promise<void> {
  await privateApi.post(`/posts/${publicId}/like`, null, {
    params: {
      source,
    },
  });
}
