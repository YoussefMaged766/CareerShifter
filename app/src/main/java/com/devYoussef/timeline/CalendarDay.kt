package com.devYoussef.timeline

import kotlinx.serialization.Serializable

@Serializable
data class CalendarDay(
    var notePattern: String? = null,
    val dayOfMonth: Int? = null,
    val month : Int? = null

)

data class CalendarDayOfWeek(
    val dayName: String? = null,
    val dayOfMonth: Int? = null
)
