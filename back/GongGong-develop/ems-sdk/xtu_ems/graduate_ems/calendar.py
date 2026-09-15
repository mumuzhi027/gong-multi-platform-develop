import datetime

from xtu_ems.common.model import TeachingCalendar
from xtu_ems.common.sess import HttpSessionHolder
from xtu_ems.common.term import get_current_term
from xtu_ems.graduate_ems.courses import get_current_week


async def get_calendar(session: HttpSessionHolder) -> TeachingCalendar:
    """Build the graduate calendar from the current week shown by GMS."""
    current_week = await get_current_week(session)
    today = datetime.datetime.now().date()
    current_monday = today - datetime.timedelta(days=today.weekday())
    term_start = current_monday - datetime.timedelta(weeks=current_week - 1)
    return TeachingCalendar(
        start=term_start,
        weeks=20,
        term_id=get_current_term(),
    )
