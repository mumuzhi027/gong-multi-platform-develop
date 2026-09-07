package com.sky31.gongmultiplatform.network.resources

import io.ktor.resources.Resource

@Resource("/activities")
class Activities(val limit: Int? = null)

@Resource("/activity-filters")
class ActivityFilters
