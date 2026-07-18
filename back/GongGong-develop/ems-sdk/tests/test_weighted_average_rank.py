from xtu_ems.zf_ems.transcript import parse_weighted_average_rank


def test_parse_weighted_average_rank():
    page_html = """
    <div class="col-sm-3">
        <label class="control-label">加权平均成绩排名：</label>
        <span class="form-control-static">40</span>
    </div>
    """

    assert parse_weighted_average_rank(page_html) == 40
