package com.sky31.gongmultiplatform.network.resources

import io.ktor.resources.Resource

@Resource("/classroom")
class Classroom() {
    @Resource("today")
    class TodayClassroom(val parent: Classroom = Classroom())

    @Resource("tomorrow")
    class TomorrowClassroom(val parent: Classroom = Classroom())
}