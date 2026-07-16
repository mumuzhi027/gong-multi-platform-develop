package com.sky31.gongmultiplatform

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sky31.gongmultiplatform.data.local.AppDatabase
import com.sky31.gongmultiplatform.data.repository.AcademicDataRepository
import com.sky31.gongmultiplatform.data.repository.AcademicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.RankData
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AppDatabaseTest {
    private lateinit var academicDataRepository: AcademicDataRepository

    @Before
    fun setup() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        academicDataRepository = AcademicDataRepositoryImpl(db.getAcademicDao())
    }

    @Test
    fun testAcademicDao() = runBlocking {
        val totalRank = RankData(
            averageScore = "89.5",
            gpa = "3.7",
            classRank = 1,
            majorRank = 3,
            terms = listOf("1", "2")
        )

        academicDataRepository.updateAcademicData(totalRank = totalRank)
        val result = academicDataRepository.getAcademicData()

        assert(result?.totalRank == totalRank)
    }
}