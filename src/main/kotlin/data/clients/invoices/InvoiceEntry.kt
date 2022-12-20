package data.clients.invoices

import date_time.Date

data class InvoiceEntry (val shiftDate: Date, val employeePk: ULong, val normalHours: Float?, val normalRate: Float,
                         val otHours: Float?, val otRate: Float, val dblotHours: Float?, val dblotRate: Float,
                         val holidayHours: Float?, val holidayRate: Float)