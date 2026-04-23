#!/usr/bin/env python3
"""Poll Gitee FF branch and run autodeploy when the commit changes."""

from __future__ import annotations

from dataclasses import dataclass
import os
from pathlib import Path
import re
import subprocess
import sys
from typing import Iterable


SHA_RE = re.compile(r"^[0-9a-fA-F]{7,40}$")


@dataclass(frozen=True)
class DeployDecision:
    should_deploy: bool
    reason: str


def load_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip("\"'")
    return values


def parse_ls_remote_commit(output: str, branch: str) -> str:
    expected_ref = f"refs/heads/{branch}"
    for line in output.splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[1] == expected_ref and SHA_RE.match(parts[0]):
            return parts[0].lower()
    raise ValueError(f"Could not find commit for {expected_ref}")


def read_release_commit(path: Path) -> str:
    if not path.exists():
        return ""
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("RELEASE_GIT_COMMIT="):
            return line.split("=", 1)[1].strip().lower()
    return ""


def evaluate_deploy_decision(
    *,
    remote_commit: str,
    state_file: Path,
    release_files: Iterable[Path],
) -> DeployDecision:
    remote_commit = remote_commit.strip().lower()
    if state_file.exists() and state_file.read_text(encoding="utf-8").strip().lower() == remote_commit:
        return DeployDecision(False, "unchanged")

    release_commits = [read_release_commit(path) for path in release_files]
    if release_commits and all(commit == remote_commit for commit in release_commits):
        state_file.parent.mkdir(parents=True, exist_ok=True)
        state_file.write_text(f"{remote_commit}\n", encoding="utf-8")
        return DeployDecision(False, "already deployed")

    return DeployDecision(True, "new commit")


def get_remote_commit(repo_url: str, branch: str) -> str:
    output = subprocess.check_output(
        ["git", "ls-remote", repo_url, f"refs/heads/{branch}"],
        text=True,
        stderr=subprocess.STDOUT,
    )
    return parse_ls_remote_commit(output, branch)


def main() -> int:
    env_path = Path(os.environ.get("AUTODEPLOY_ENV_FILE", "/etc/pz-autodeploy/autodeploy.env"))
    file_env = load_env_file(env_path)
    config = {**file_env, **os.environ}

    repo_url = config.get("GITEE_REPO_URL", "https://gitee.com/wkq-karry/master.git")
    branch = config.get("AUTODEPLOY_BRANCH", "FF")
    root = Path(config.get("AUTODEPLOY_ROOT", "/opt/pz-autodeploy"))
    state_file = Path(config.get("AUTODEPLOY_POLL_STATE_FILE", str(root / "last-ff-commit")))
    deploy_script = config.get("AUTODEPLOY_SCRIPT", str(root / "bin/autodeploy-run.sh"))
    release_dir = Path(config.get("RELEASE_META_DIR", "/var/lib/pz-deploy/releases"))
    release_files = [
        release_dir / "backend-release.env",
        release_dir / "frontend-release.env",
        release_dir / "wgt-release.env",
    ]

    remote_commit = get_remote_commit(repo_url, branch)
    decision = evaluate_deploy_decision(
        remote_commit=remote_commit,
        state_file=state_file,
        release_files=release_files,
    )
    print(f"poll branch={branch} commit={remote_commit[:7]} decision={decision.reason}", flush=True)
    if not decision.should_deploy:
        return 0

    env = os.environ.copy()
    env.update(file_env)
    env["RELEASE_ACTOR"] = "gitee-poll"
    result = subprocess.run([deploy_script, remote_commit], env=env, check=False)
    if result.returncode == 0:
        state_file.parent.mkdir(parents=True, exist_ok=True)
        state_file.write_text(f"{remote_commit}\n", encoding="utf-8")
    return result.returncode


if __name__ == "__main__":
    sys.exit(main())
