package data.clients.work_locations.shifts

import date_time.DateTime

data class Shift (val primaryKey: ULong?, var employeePk: ULong?, val workLocationPk: ULong, var startDateTime: DateTime,
                  var endDateTime: DateTime, var payStartDateTime: DateTime?, var payEndDateTime: DateTime?)
