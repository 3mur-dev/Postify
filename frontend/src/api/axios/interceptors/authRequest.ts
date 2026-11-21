import { AxiosHeaders } from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { getToken } from "../../../utils/token";

export function attachAccessToken(
  config: InternalAxiosRequestConfig
): InternalAxiosRequestConfig {
  const token = getToken();

  if (!token) {
    return config;
  }

  config.headers = AxiosHeaders.from(config.headers ?? {}).set(
    "Authorization",
    `Bearer ${token}`
  );

  return config;
}
