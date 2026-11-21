import { AxiosHeaders, type AxiosError } from "axios";
import { expireAuthSession } from "../../../utils/authSession";
import { getToken } from "../../../utils/token";

export function redirectOnUnauthorized(error: AxiosError): Promise<AxiosError> {
  if (error.response?.status === 401) {
    const authorization = AxiosHeaders.from(
      error.config?.headers ?? {},
    ).get("Authorization");
    const requestToken =
      typeof authorization === "string" && authorization.startsWith("Bearer ")
        ? authorization.slice("Bearer ".length)
        : null;

    if (requestToken && requestToken === getToken()) {
      expireAuthSession();
    }
  }

  return Promise.reject(error);
}
