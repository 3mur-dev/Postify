import { getToken, removeToken } from "./token";

export const AUTH_SESSION_EXPIRED_EVENT = "postify:auth-session-expired";

export function expireAuthSession() {
  if (!getToken()) {
    return;
  }

  removeToken();
  window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT));
}
