package data.clients.work_locations.shifts.breaks

import date_time.DateTime

data class Break (val primaryKey: ULong, val shiftPk: ULong, val startDateTime: DateTime, val durationMinutes: UInt,
                  val isPaid: Boolean, val isPremium: Boolean, val isMeal: Boolean)
