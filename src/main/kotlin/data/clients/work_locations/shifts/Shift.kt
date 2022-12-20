package data.clients.work_locations.shifts

import data.clients.invoices.InvoiceEntry
import data.clients.work_locations.shifts.breaks.Break
import date_time.DateTime
import kotlin.math.roundToInt

class Shift (val primaryKey: ULong?, val employeePk: ULong?, val workLocationPk: ULong, val startDateTime: DateTime,
             val endDateTime: DateTime, val payStartDateTime: DateTime?, val payEndDateTime: DateTime?,
             val billableOt: Boolean, val needsReconciliation: Boolean, val rate: Float?, val payRate: Float?) {
    fun generateInvoiceEntry (workHours: WorkHours, breaks: List<Break>, normalRate: Float,
                              otModifier: Float, dblotModifier: Float, holidayModifier: Float) : InvoiceEntry? {

        // Make sure there is an employee assigned to this shift
        if (employeePk == null)
            return null

        // Determine the normal hours based on breaks
        val paidBreaks = breaks.filter { brk -> brk.isMeal && brk.isPaid }
        val premiumBreaks = breaks.filter { brk -> brk.isPremium }
        val normalHoursPlusPaidBreaks =
            // First see if there are any payable breaks
            if (paidBreaks.isNotEmpty() || premiumBreaks.isNotEmpty()) {
                // If all the breaks are payable, then we can just use the remainder of the total
                // shift hours plus any premium breaks as the normal hours
                if (paidBreaks.isNotEmpty() && breaks.size == paidBreaks.size + premiumBreaks.size) {
                    val shiftHoursShifted = (
                        startDateTime.secondsBetween(endDateTime).toFloat() / 60 / 60 * 100
                    ).roundToInt().toUInt()
                    val otHoursShifted = (workHours.otHours * 100).toUInt()
                    val dblotHoursShifted = (workHours.dblotHours * 100).toUInt()
                    val holidayHoursShifted = (workHours.holidayHours * 100).toUInt()
                    (shiftHoursShifted - otHoursShifted - dblotHoursShifted - holidayHoursShifted).toFloat() / 100 + premiumBreaks.size
                // Otherwise, if there are only premium breaks, then simply add the number of
                // premium breaks (1 hour) to the current normal time
                } else if (paidBreaks.isEmpty()) {
                    workHours.normalHours + premiumBreaks.size
                // Otherwise, there are some non-paid breaks, so calculate the time and add both paid and premium
                } else {
                    val normalHoursToMinutesShiftedInt = (workHours.normalHours * 100 * 60).toUInt()
                    val normalHoursShiftedPlusPaidBreaks = ((normalHoursToMinutesShiftedInt + paidBreaks.sumOf { brk ->
                        brk.durationMinutes * 100u
                    }).toFloat() / 60f).roundToInt()
                    normalHoursShiftedPlusPaidBreaks.toFloat() / 100 + premiumBreaks.size
                }
            // Otherwise, don't bother and use what is given
            } else {
                workHours.normalHours
            }

        return InvoiceEntry (
            shiftDate = startDateTime.date,
            employeePk = employeePk,
            normalHours = if (normalHoursPlusPaidBreaks != 0f) normalHoursPlusPaidBreaks else null,
            normalRate = normalRate,
            otHours = if (workHours.otHours != 0f) workHours.otHours else null,
            otRate = (normalRate * otModifier * 100).roundToInt().toFloat() / 100,
            dblotHours = if (workHours.dblotHours != 0f) workHours.dblotHours else null,
            dblotRate = (normalRate * dblotModifier * 100).roundToInt().toFloat() / 100,
            holidayHours = if (workHours.holidayHours != 0f) workHours.holidayHours else null,
            holidayRate = (normalRate * holidayModifier * 100).roundToInt().toFloat() / 100
        )

    }
    /*
    fun generatePayrollApiEntries (workHours: WorkHours, breaks: List<Break>) : List<PayrollApiEntry> {
        val apiEntries = mutableMapOf<GlobalEdmCode, Float>()
    }

     */

}
