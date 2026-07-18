package com.sky31.gongmultiplatform.network.resources

import io.ktor.resources.Resource

@Resource("/scores")
class MajorAcademicInfo(val refresh: Boolean = false)
