import { createClient } from "./createClient";
import { attachAccessToken } from "./interceptors/authRequest";
import { redirectOnUnauthorized } from "./interceptors/authResponse";

const privateApi = createClient();

privateApi.interceptors.request.use(
  attachAccessToken,
  (error) => Promise.reject(error)
);

privateApi.interceptors.response.use(
  (response) => response,
  redirectOnUnauthorized
);

export default privateApi;
