import { getToken } from "../utils/token";

export function getAccessToken() {
  return getToken();
}

export function hasAccessToken() {
  return !!getAccessToken();
}
