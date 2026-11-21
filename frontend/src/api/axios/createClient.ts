import axios,  { type AxiosInstance, type AxiosRequestConfig } from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
const DEFAULT_TIMEOUT = 15000;

const DEFAULT_HEADERS = {
  Accept: "application/json",
};

export function createClient(config?: AxiosRequestConfig): AxiosInstance {
  return axios.create({
    withCredentials: true,
    baseURL: API_BASE_URL,
    timeout: DEFAULT_TIMEOUT,
    headers: {
      ...DEFAULT_HEADERS,
      ...config?.headers,
    },
    ...config,
  });
}
