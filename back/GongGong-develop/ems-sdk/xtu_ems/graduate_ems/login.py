import logging
import asyncio
from urllib.parse import urlparse

from xtu_ems.common.config import app_list_url
from xtu_ems.common.exception import *
from xtu_ems.common.sess import HttpSessionHolder

logger = logging.getLogger(__name__)


def _pick_app(app_list: list[dict]):
    """
    从应用列表中选择合适的应用

    目前的策略是选择第一个名称包含“教务”的应用

    :param app_list: 应用列表
    :return: 选中的应用字典，如果没有找到合适的应用则返回 None
    """
    for app in app_list:
        if "研究生系统（学生端）" in app.get("mc", ""):
            return app
    return None


async def sso_auth(session: HttpSessionHolder) -> HttpSessionHolder:
    """
    使用统一身份认证系统进行登录验证

    :param session: HttpSessionHolder 对象，用于存储会话信息
    :return: 更新后的 HttpSessionHolder 对象
    """
    async with session.to_aiohttp_session() as http_session:
        async with http_session.post(app_list_url) as response:
            logger.debug(f"SSO auth response status: {response.status}")
            if response.status != 200:
                raise SessionInvalidException(f"SSO Authentication Failed, status code: {response.status}")
            # 这里可以根据需要处理返回的数据
            data = await response.json()
        app = _pick_app(data.get("data", []))
        if not app:
            raise ServiceUnavailableException("No suitable application found")
        target_url = app.get("ywxturl")
        if not target_url:
            raise ServiceUnavailableException("Application URL not found")
        reached_gms = False
        for attempt in range(3):
            async with http_session.get(target_url) as response:
                final_url = str(response.url)
                parsed_url = urlparse(final_url)
                reached_gms = reached_gms or parsed_url.hostname == "gmsstu.xtu.edu.cn"
                logger.info("研究生系统 SSO 跳转完成：host=%s，status=%s，attempt=%s", parsed_url.hostname, response.status, attempt + 1)
                # 登录后可能落到 /index，也可能恢复到上次访问的 /tab 或业务页。
                if response.status == 200 and parsed_url.hostname == "gmsstu.xtu.edu.cn" \
                        and parsed_url.path != "/oauthLogon":
                    authenticated = HttpSessionHolder.from_aiohttp_session(http_session)
                    authenticated.metadata.update(session.metadata)
                    return authenticated
            if attempt < 2:
                await asyncio.sleep(0.2)
        if reached_gms:
            raise ServiceUnavailableException("GMS SSO", "Graduate SSO did not finish")
        raise GmsAccountNotFoundException("GMS account not found")
