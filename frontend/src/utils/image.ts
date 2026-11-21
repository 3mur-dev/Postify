const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ??
  "http://localhost:8080/api"
).replace(/\/api\/?$/, "");

export function resolveImageUrl(
  url: string | null | undefined
): string | undefined {
  if (!url) {
    return undefined;
  }

  if (/^https?:\/\//i.test(url)) {
    return url;
  }

  if (url.startsWith("/")) {
    return `${API_BASE_URL}${url}`;
  }

  return url;
}