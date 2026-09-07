import unittest

from xtu_ems.common.exception import ServiceUnavailableException
from xtu_ems.zf_sso.login import _resolve_login_redirect


class LoginRedirectTest(unittest.TestCase):
    def test_accepts_relative_application_center_redirect(self):
        resolved = _resolve_login_redirect("/application-center?ticket=example")
        self.assertEqual(
            resolved,
            "https://portal2020.xtu.edu.cn/application-center?ticket=example",
        )

    def test_accepts_configured_absolute_redirect(self):
        resolved = _resolve_login_redirect(
            "https://portal2020.xtu.edu.cn/application-center/?ticket=example"
        )
        self.assertIn("/application-center/", resolved)

    def test_rejects_external_redirect(self):
        with self.assertRaises(ServiceUnavailableException):
            _resolve_login_redirect("https://example.com/application-center?ticket=secret")


if __name__ == "__main__":
    unittest.main()
