package com.sky31.gongmultiplatform.ui.screen.academicScreen

import com.sky31.gongmultiplatform.model.ScoreData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeightedAverageTest {
    @Test
    fun calculatesWeightedAverageFromNumericAndGradedCompulsoryCourses() {
        val scores = listOf(
            score(name = "数学", score = "88", credit = "6", type = "必修"),
            score(name = "英语", score = "66", credit = "3", type = "必修"),
            score(name = "体育", score = "优秀", credit = "1", type = "必修"),
            score(name = "选修课", score = "100", credit = "10", type = "选修")
        )

        assertEquals(82.1, calculateCompulsoryWeightedAverage(scores))
        assertEquals("82.10", formatWeightedAverage(calculateCompulsoryWeightedAverage(scores)!!))
    }

    @Test
    fun convertsAllSupportedGradeValues() {
        assertEquals(95.0, scoreValue("优"))
        assertEquals(93.0, scoreValue("良"))
        assertEquals(73.0, scoreValue("中"))
        assertEquals(62.0, scoreValue("及格"))
        assertEquals(30.0, scoreValue("不及格"))
    }

    @Test
    fun calculatesAcrossAllTermsWhenGivenTheCompleteTranscript() {
        val scores = listOf(
            score(name = "大一数学", score = "80", credit = "4", type = "必修", term = 1),
            score(name = "大二英语", score = "优", credit = "2", type = "必修", term = 3)
        )

        assertEquals(85.0, calculateCompulsoryWeightedAverage(scores))
    }

    @Test
    fun returnsNullWhenThereAreNoSupportedCompulsoryCredits() {
        val scores = listOf(
            score(name = "体育", score = "免修", credit = "1", type = "必修"),
            score(name = "无学分课程", score = "90", credit = "0", type = "必修")
        )

        assertNull(calculateCompulsoryWeightedAverage(scores))
    }

    private fun score(
        name: String,
        score: String,
        credit: String,
        type: String,
        term: Int = 1
    ) = ScoreData.ScoreElem(
        name = name,
        score = score,
        credit = credit,
        type = type,
        term = term
    )
}
