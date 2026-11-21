from __future__ import annotations

import os
import unittest
from unittest.mock import patch

from fastapi import Request
from fastapi.responses import Response

from api.main import (
    SERVICE_TOKEN_ENV,
    authorize_rank_request,
    validate_service_auth_configuration,
)


def make_request(
    path: str,
    method: str = "POST",
    headers: dict[str, str] | None = None,
) -> Request:
    encoded_headers = [
        (key.lower().encode("latin-1"), value.encode("latin-1"))
        for key, value in (headers or {}).items()
    ]
    return Request(
        {
            "type": "http",
            "asgi": {"version": "3.0"},
            "http_version": "1.1",
            "method": method,
            "scheme": "http",
            "path": path,
            "raw_path": path.encode(),
            "query_string": b"",
            "root_path": "",
            "headers": encoded_headers,
            "client": ("127.0.0.1", 12345),
            "server": ("127.0.0.1", 8000),
        }
    )


class RankAuthenticationTests(unittest.IsolatedAsyncioTestCase):
    async def test_rejects_missing_or_invalid_credentials_before_ranking(self) -> None:
        for headers, expected_status, environment in (
            ({}, 401, {SERVICE_TOKEN_ENV: "shared-service-secret"}),
            (
                {"X-ML-Service-Token": "wrong-secret"},
                401,
                {SERVICE_TOKEN_ENV: "shared-service-secret"},
            ),
            ({}, 503, {}),
        ):
            with self.subTest(expected_status=expected_status), patch.dict(
                os.environ,
                {"APP_ENV": "development", **environment},
                clear=True,
            ):
                called = False

                async def next_handler(_request: Request) -> Response:
                    nonlocal called
                    called = True
                    return Response(status_code=200)

                response = await authorize_rank_request(
                    make_request("/rank", headers=headers),
                    next_handler,
                )

                self.assertEqual(response.status_code, expected_status)
                self.assertFalse(called)

    async def test_valid_service_credential_reaches_rank_handler(self) -> None:
        with patch.dict(
            os.environ,
            {"APP_ENV": "development", SERVICE_TOKEN_ENV: "shared-service-secret"},
            clear=True,
        ):
            async def next_handler(_request: Request) -> Response:
                return Response(status_code=204)

            response = await authorize_rank_request(
                make_request(
                    "/rank",
                    headers={"X-ML-Service-Token": "shared-service-secret"},
                ),
                next_handler,
            )

        self.assertEqual(response.status_code, 204)

    async def test_health_check_does_not_require_service_credentials(self) -> None:
        with patch.dict(os.environ, {"APP_ENV": "development"}, clear=True):
            async def next_handler(_request: Request) -> Response:
                return Response(status_code=200)

            response = await authorize_rank_request(
                make_request("/health", method="GET"),
                next_handler,
            )

        self.assertEqual(response.status_code, 200)

    def test_production_fails_closed_when_service_secret_is_missing(self) -> None:
        with patch.dict(os.environ, {"APP_ENV": "production"}, clear=True):
            with self.assertRaisesRegex(
                RuntimeError,
                "authentication must be configured",
            ):
                validate_service_auth_configuration()

    def test_production_accepts_configured_service_secret(self) -> None:
        with patch.dict(
            os.environ,
            {"APP_ENV": "production", SERVICE_TOKEN_ENV: "shared-service-secret"},
            clear=True,
        ):
            validate_service_auth_configuration()


if __name__ == "__main__":
    unittest.main()
