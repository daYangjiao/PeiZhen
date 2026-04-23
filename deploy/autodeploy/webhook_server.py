#!/usr/bin/env python3
"""Gitee webhook receiver for PZ autodeploy.

The server intentionally uses only the Python standard library so it can run on
the production host without additional Python packages.
"""

from __future__ import annotations

from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import hmac
import json
import os
from pathlib import Path
import re
import subprocess
import threading
from datetime import datetime
from typing import Mapping


DEFAULT_PATH = "/gitee-webhook"
SHA_RE = re.compile(r"^[0-9a-fA-F]{7,40}$")


@dataclass(frozen=True)
class PushEvaluation:
    accepted: bool
    status_code: int
    message: str
    commit: str = ""


def _header(headers: Mapping[str, str], name: str) -> str:
    for key, value in headers.items():
        if key.lower() == name.lower():
            return value
    return ""


def evaluate_push(
    *,
    path: str,
    headers: Mapping[str, str],
    body: Mapping[str, object],
    expected_token: str,
    branch: str,
) -> PushEvaluation:
    if path != DEFAULT_PATH:
        return PushEvaluation(False, 404, "not found")

    if not expected_token:
        return PushEvaluation(False, 500, "webhook token is not configured")

    actual_token = _header(headers, "X-Gitee-Token")
    if not hmac.compare_digest(actual_token, expected_token):
        return PushEvaluation(False, 403, "invalid token")

    expected_ref = f"refs/heads/{branch}"
    ref = str(body.get("ref", ""))
    if ref != expected_ref:
        return PushEvaluation(False, 202, f"ignored ref {ref}")

    commit = str(body.get("after") or body.get("checkout_sha") or "")
    if not SHA_RE.match(commit) or set(commit) == {"0"}:
        return PushEvaluation(False, 400, "invalid commit")

    return PushEvaluation(True, 202, "deployment accepted", commit.lower())


class DeploymentRunner:
    def __init__(self, script: str, log_dir: str) -> None:
        self.script = script
        self.log_dir = Path(log_dir)
        self._lock = threading.Lock()

    def start(self, commit: str) -> bool:
        if not self._lock.acquire(blocking=False):
            return False
        thread = threading.Thread(target=self._run, args=(commit,), daemon=True)
        thread.start()
        return True

    def _run(self, commit: str) -> None:
        self.log_dir.mkdir(parents=True, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        log_path = self.log_dir / f"deploy-{timestamp}-{commit[:7]}.log"
        env = os.environ.copy()
        try:
            with log_path.open("ab") as log_file:
                log_file.write(f"Starting deploy for {commit}\n".encode())
                log_file.flush()
                subprocess.run(
                    [self.script, commit],
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                    env=env,
                    check=False,
                )
        finally:
            self._lock.release()


def make_handler(
    *,
    token: str,
    branch: str,
    runner: DeploymentRunner,
):
    class GiteeWebhookHandler(BaseHTTPRequestHandler):
        server_version = "PZAutodeployWebhook/1.0"

        def do_GET(self) -> None:
            if self.path == f"{DEFAULT_PATH}/healthz":
                self._write_json(200, {"status": "ok"})
                return
            self._write_json(404, {"message": "not found"})

        def do_POST(self) -> None:
            length = int(self.headers.get("Content-Length", "0"))
            raw_body = self.rfile.read(length)
            try:
                body = json.loads(raw_body.decode("utf-8") or "{}")
            except json.JSONDecodeError:
                self._write_json(400, {"message": "invalid json"})
                return

            result = evaluate_push(
                path=self.path,
                headers=self.headers,
                body=body,
                expected_token=token,
                branch=branch,
            )
            if not result.accepted:
                self._write_json(result.status_code, {"message": result.message})
                return

            if not runner.start(result.commit):
                self._write_json(409, {"message": "deployment already running"})
                return

            self._write_json(202, {"message": result.message, "commit": result.commit})

        def log_message(self, fmt: str, *args: object) -> None:
            print("%s - %s" % (self.log_date_time_string(), fmt % args), flush=True)

        def _write_json(self, status: int, payload: Mapping[str, object]) -> None:
            data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    return GiteeWebhookHandler


def main() -> None:
    host = os.environ.get("WEBHOOK_HOST", "127.0.0.1")
    port = int(os.environ.get("WEBHOOK_PORT", "9017"))
    token = os.environ.get("GITEE_WEBHOOK_TOKEN", "")
    branch = os.environ.get("AUTODEPLOY_BRANCH", "FF")
    script = os.environ.get("AUTODEPLOY_SCRIPT", "/opt/pz-autodeploy/bin/autodeploy-run.sh")
    log_dir = os.environ.get("AUTODEPLOY_LOG_DIR", "/var/log/pz-autodeploy")

    runner = DeploymentRunner(script=script, log_dir=log_dir)
    handler = make_handler(token=token, branch=branch, runner=runner)
    server = ThreadingHTTPServer((host, port), handler)
    print(f"Listening on {host}:{port} for {DEFAULT_PATH} branch={branch}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
