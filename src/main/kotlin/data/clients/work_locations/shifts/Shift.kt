package data.clients.work_locations.shifts

import date_time.DateTime

data class Shift (val primaryKey: ULong?, val employeePk: ULong?, val workLocationPk: ULong, val startDateTime: DateTime,
                  val endDateTime: DateTime, val payStartDateTime: DateTime?, val payEndDateTime: DateTime?,
                  val needsReconciliation: Boolean)
