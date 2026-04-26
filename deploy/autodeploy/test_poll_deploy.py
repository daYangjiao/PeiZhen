import tempfile
import unittest
from pathlib import Path

import poll_deploy


class PollDeployTest(unittest.TestCase):
    def test_parse_ls_remote_commit_for_branch(self):
        output = (
            "1111111111111111111111111111111111111111\trefs/heads/main\n"
            "2222222222222222222222222222222222222222\trefs/heads/FF\n"
        )

        commit = poll_deploy.parse_ls_remote_commit(output, "FF")

        self.assertEqual(commit, "2222222222222222222222222222222222222222")

    def test_should_skip_when_state_matches_remote(self):
        with tempfile.TemporaryDirectory() as tmp:
            state = Path(tmp) / "last-ff-commit"
            state.write_text("abc123\n", encoding="utf-8")

            decision = poll_deploy.evaluate_deploy_decision(
                remote_commit="abc123",
                state_file=state,
                release_files=[],
            )

        self.assertFalse(decision.should_deploy)
        self.assertEqual(decision.reason, "unchanged")

    def test_deploys_when_state_matches_but_release_records_do_not(self):
        with tempfile.TemporaryDirectory() as tmp:
            state = Path(tmp) / "last-ff-commit"
            state.write_text("abc123\n", encoding="utf-8")
            backend_release = Path(tmp) / "backend-release.env"
            frontend_release = Path(tmp) / "frontend-release.env"
            wgt_release = Path(tmp) / "wgt-release.env"
            backend_release.write_text("RELEASE_GIT_COMMIT=abc123\n", encoding="utf-8")
            frontend_release.write_text("RELEASE_GIT_COMMIT=abc123\n", encoding="utf-8")
            wgt_release.write_text("RELEASE_GIT_COMMIT=old123\n", encoding="utf-8")

            decision = poll_deploy.evaluate_deploy_decision(
                remote_commit="abc123",
                state_file=state,
                release_files=[backend_release, frontend_release, wgt_release],
            )

        self.assertTrue(decision.should_deploy)
        self.assertEqual(decision.reason, "incomplete release")

    def test_initializes_state_when_release_records_match(self):
        with tempfile.TemporaryDirectory() as tmp:
            state = Path(tmp) / "last-ff-commit"
            release = Path(tmp) / "backend-release.env"
            release.write_text("RELEASE_GIT_COMMIT=abc123\n", encoding="utf-8")

            decision = poll_deploy.evaluate_deploy_decision(
                remote_commit="abc123",
                state_file=state,
                release_files=[release],
            )

            self.assertFalse(decision.should_deploy)
            self.assertEqual(state.read_text(encoding="utf-8"), "abc123\n")
            self.assertEqual(decision.reason, "already deployed")

    def test_deploys_when_remote_differs_from_state(self):
        with tempfile.TemporaryDirectory() as tmp:
            state = Path(tmp) / "last-ff-commit"
            state.write_text("old123\n", encoding="utf-8")

            decision = poll_deploy.evaluate_deploy_decision(
                remote_commit="new123",
                state_file=state,
                release_files=[],
            )

        self.assertTrue(decision.should_deploy)
        self.assertEqual(decision.reason, "new commit")


if __name__ == "__main__":
    unittest.main()
