import publicApi from "./axios/publicApi";
import type { ApiResponse, AuthTokenResponse, AuthLoginPayload, AuthRegisterPayload } from "./types";


export async function login(
  payload: AuthLoginPayload
): Promise<AuthTokenResponse> {
  const response = await publicApi.post<ApiResponse<AuthTokenResponse>>(
    "/auth/login",
    payload
  );

  return response.data.data;
}

export async function register(
  payload: AuthRegisterPayload
): Promise<void> {
  await publicApi.post<ApiResponse<null>>("/auth/register", payload);
}
