import axios from "axios";

interface ApiErrorResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  timestamp?: string;
  errors?: {
    field: string;
    message: string;
  }[];
}

export function getApiErrorMessage(
  error: unknown,
  fallback = "Something went wrong. Please try again."
): string {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return error instanceof Error ? error.message : fallback;
  }

  const data = error.response?.data;

  if (data?.detail) {
    return data.detail;
  }

  return fallback;
}