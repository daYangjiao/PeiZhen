import unittest

import webhook_server


class WebhookServerTest(unittest.TestCase):
    def test_accepts_matching_token_and_ff_push(self):
        payload = {
            "ref": "refs/heads/FF",
            "after": "803c9583d6ae916f2540e455fc033e18757c3007",
        }

        result = webhook_server.evaluate_push(
            path="/gitee-webhook",
            headers={"X-Gitee-Token": "secret"},
            body=payload,
            expected_token="secret",
            branch="FF",
        )

        self.assertTrue(result.accepted)
        self.assertEqual(result.commit, payload["after"])
        self.assertEqual(result.status_code, 202)

    def test_rejects_wrong_token(self):
        result = webhook_server.evaluate_push(
            path="/gitee-webhook",
            headers={"X-Gitee-Token": "wrong"},
            body={"ref": "refs/heads/FF", "after": "abc123"},
            expected_token="secret",
            branch="FF",
        )

        self.assertFalse(result.accepted)
        self.assertEqual(result.status_code, 403)

    def test_ignores_non_production_branch(self):
        result = webhook_server.evaluate_push(
            path="/gitee-webhook",
            headers={"X-Gitee-Token": "secret"},
            body={"ref": "refs/heads/main", "after": "abc123"},
            expected_token="secret",
            branch="FF",
        )

        self.assertFalse(result.accepted)
        self.assertEqual(result.status_code, 202)
        self.assertIn("ignored", result.message)

    def test_rejects_invalid_commit(self):
        result = webhook_server.evaluate_push(
            path="/gitee-webhook",
            headers={"X-Gitee-Token": "secret"},
            body={"ref": "refs/heads/FF", "after": "not a sha"},
            expected_token="secret",
            branch="FF",
        )

        self.assertFalse(result.accepted)
        self.assertEqual(result.status_code, 400)


if __name__ == "__main__":
    unittest.main()
