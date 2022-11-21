package data.clients.work_locations.shifts

import Messages.printShiftCalculationResults
import data.SheetReader
import data.clients.work_locations.shifts.breaks.Break
import date_time.DateTime
import java.time.ZoneId


fun clockIn (shiftPk: ULong, startDateTime: DateTime, payStartDateTime: DateTime) {
    updatePayStartTimeData (
        shiftPk = shiftPk,
        payStartDateTime = payStartDateTime,
        needsReconciliation = payStartDateTime > startDateTime.offsetSeconds(59)
    )
}

fun clockOut (shiftPk: ULong, workLocationPk: ULong, dateTimes: ClosedRange<DateTime>,
              payDateTimes: ClosedRange<DateTime>, employeePk: ULong) {

    if (payDateTimes.endInclusive.secondsBetween(dateTimes.endInclusive) > 59u) {

        updatePayEndTimeData (
            shiftPk = shiftPk,
            payEndDateTime = payDateTimes.endInclusive,
            needsReconciliation = true
        )

    } else {

        val shift = Shift (
            shiftPk,
            employeePk,
            workLocationPk,
            dateTimes.start,
            dateTimes.endInclusive,
            payDateTimes.start,
            payDateTimes.endInclusive,
            needsReconciliation = false
        )

        updateEmployeeWorkHoursWithinPeriod (
            shifts = listOf(shift),
            newBreaks = listOf(null),
            forBilling = false,
            forPay = true,
            needsReconciliation = listOf(false)
        )

    }

}

fun changePayTimes (shiftPk: ULong, workLocationPk: ULong, dateTimes: ClosedRange<DateTime>,
                    payDateTimes: ClosedRange<DateTime>, employeePk: ULong, needsReconciliation: Boolean) {

    if (needsReconciliation) {

        updatePayTimesData (
            shiftPk = shiftPk,
            payDateTimes = payDateTimes.start.rangeTo(payDateTimes.endInclusive),
            needsReconciliation = true
        )

    } else {

        val shift = Shift (
            shiftPk,
            employeePk,
            workLocationPk,
            dateTimes.start,
            dateTimes.endInclusive,
            payDateTimes.start,
            payDateTimes.endInclusive,
            needsReconciliation = false
        )

        updateEmployeeWorkHoursWithinPeriod (
            shifts = listOf(shift),
            newBreaks = listOf(null),
            forBilling = false,
            forPay = true,
            needsReconciliation = listOf(false)
        )

    }

}

fun assignShiftToEmployee (shiftPk: ULong, dateTimes: ClosedRange<DateTime>, workLocationPk: ULong,
                           employeePk: ULong, branchTimeZone: ZoneId) = assignShiftsToEmployee (
    parallelShiftPksList = listOf(shiftPk),
    parallelDateTimesList = listOf(dateTimes),
    workLocationPk = workLocationPk,
    employeePk = employeePk,
    branchTimeZone = branchTimeZone
)

fun assignShiftsToEmployee (parallelShiftPksList: List<ULong>, parallelDateTimesList: List<ClosedRange<DateTime>>,
                            workLocationPk: ULong, employeePk: ULong, branchTimeZone: ZoneId) {

    val dateTimeNow = DateTime(branchTimeZone)

    val shifts = List (parallelShiftPksList.size) { index ->
        val startDateTime = parallelDateTimesList[index].start
        val endDateTime = parallelDateTimesList[index].endInclusive
        val inPast = startDateTime < dateTimeNow
        val payStartDateTime = if (inPast) startDateTime else null
        val payEndDateTime = if (inPast) endDateTime else null
        Shift (
            primaryKey =  parallelShiftPksList[index],
            employeePk = employeePk,
            workLocationPk = workLocationPk,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            payStartDateTime = payStartDateTime,
            payEndDateTime = payEndDateTime,
            needsReconciliation = false
        )
    }

    val anyPay = shifts.any { shift -> shift.payStartDateTime != null }
    val noNewBreaks = List (parallelShiftPksList.size) { null }
    val noReconciliation = List (parallelShiftPksList.size) { false }

    if (anyPay)
        updateEmployeeWorkHoursWithinPeriod (
            shifts = shifts,
            newBreaks = noNewBreaks,
            forBilling = true,
            forPay = true,
            needsReconciliation = noReconciliation
        )
    else
        updateEmployeeWorkHoursWithinPeriod (
            shifts = shifts,
            newBreaks = noNewBreaks,
            forBilling = true,
            forPay = false,
            needsReconciliation = noReconciliation
        )

}

fun createShift (workLocationPk: ULong, dateTimes: ClosedRange<DateTime>, employeePk: ULong?,
                 branchTimeZone: ZoneId, newBreaks: List<Break>?) = createShifts (
    workLocationPk = workLocationPk,
    parallelDateTimesList = listOf(dateTimes),
    employeePk = employeePk,
    branchTimeZone = branchTimeZone,
    parallelNewBreaksList = listOf(newBreaks)
)

fun createShifts (workLocationPk: ULong, parallelDateTimesList: List<ClosedRange<DateTime>>,
                  employeePk: ULong?, branchTimeZone: ZoneId, parallelNewBreaksList: List<List<Break>?>) {

    if (employeePk == null) {

        createOpenShiftRecords (
            workLocationPk = workLocationPk,
            dateTimes = parallelDateTimesList,
            newBreaks = parallelNewBreaksList
        )

    } else {

        val dateTimeNow = DateTime(branchTimeZone)

        val newShifts = List (parallelDateTimesList.size) { parallelIndex ->
            val startDateTime = parallelDateTimesList[parallelIndex].start
            val endDateTime = parallelDateTimesList[parallelIndex].endInclusive
            val inPast = startDateTime < dateTimeNow
            val payStartDateTime = if (inPast) startDateTime else null
            val payEndDateTime = if (inPast) endDateTime else null
            Shift (
                primaryKey = null,
                employeePk = employeePk,
                workLocationPk = workLocationPk,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                payStartDateTime = payStartDateTime,
                payEndDateTime = payEndDateTime,
                needsReconciliation = false
            )
        }

        val anyPay = newShifts.any { shift -> shift.payStartDateTime != null }

        if (anyPay)
            createAndUpdateEmployeeShiftsWithinPeriods (
                newShifts = newShifts,
                newBreaks = parallelNewBreaksList,
                alsoPay = true
            )
        else
            createAndUpdateEmployeeShiftsWithinPeriods (
                newShifts = newShifts,
                newBreaks = parallelNewBreaksList,
                alsoPay = false
            )

    }

}

fun editShiftHours (parallelShiftPksList: List<ULong>, parallelDateTimesList: List<ClosedRange<DateTime>>,
                    parallelPayDateTimesList: List<ClosedRange<DateTime>?>, workLocationPk: ULong,
                    parallelNewBreaks: List<List<Break>?>, parallelNeedsReconciliation: List<Boolean>,
                    employeePk: ULong?) {

    if (employeePk == null) {

        updateOpenShiftRecords(parallelShiftPksList, parallelDateTimesList, parallelNewBreaks)

    } else {

        val shifts = List (parallelShiftPksList.size) { index ->
            Shift (
                primaryKey = parallelShiftPksList[index],
                employeePk = employeePk,
                workLocationPk = workLocationPk,
                startDateTime = parallelDateTimesList[index].start,
                endDateTime = parallelDateTimesList[index].endInclusive,
                payStartDateTime = parallelPayDateTimesList[index]?.start,
                payEndDateTime = parallelPayDateTimesList[index]?.endInclusive,
                needsReconciliation = parallelNeedsReconciliation[index]
            )
        }

        if (parallelNewBreaks.any { breaks -> breaks != null})
            updateEmployeeWorkHoursWithinPeriod (
                shifts = shifts,
                newBreaks = parallelNewBreaks,
                forBilling = true,
                forPay = true,
                needsReconciliation = parallelNeedsReconciliation
            )
        else
            updateEmployeeWorkHoursWithinPeriod (
                shifts = shifts,
                newBreaks = parallelNewBreaks,
                forBilling = true,
                forPay = false,
                needsReconciliation = parallelNeedsReconciliation
            )

    }

}

fun reconcileShift (shiftPk: ULong, employeePk: ULong, workLocationPk: ULong, dateTimes: ClosedRange<DateTime>,
                    payDateTimes: ClosedRange<DateTime>) {

    val shift = Shift (
        primaryKey = shiftPk,
        employeePk = employeePk,
        workLocationPk = workLocationPk,
        startDateTime = dateTimes.start,
        endDateTime = dateTimes.endInclusive,
        payStartDateTime = payDateTimes.start,
        payEndDateTime = payDateTimes.endInclusive,
        needsReconciliation = false
    )

    updateEmployeeWorkHoursWithinPeriod (
        shifts = listOf(shift),
        newBreaks = listOf(null),
        forBilling = true,
        forPay = true,
        needsReconciliation = listOf(false)
    )

}

data class UnprocessedShifts (val newShiftsInsertedAt: List<Int>, val shiftHours: List<WorkHours?>,
                              val shifts: List<Shift>)

// This class is mainly used for printing and debugging
data class ShiftCalculationResults (val dataQuery: SheetReader.ShiftCalculationDataQuery,
                                    val shifts: UnprocessedShifts, val forPay: Boolean)

private fun updateEmployeeWorkHoursWithinPeriod (shifts: List<Shift>, newBreaks: List<List<Break>?>, forPay: Boolean,
                                                 forBilling: Boolean, needsReconciliation: List<Boolean>) {

    val calculatedShiftsWithinPeriodForPay =
        if (forPay)
            getNewWorkHours(shifts, newBreaks, forPay = true)
        else
            null

    val calculatedShiftsWithinPeriod =
        if (forBilling)
            getNewWorkHours(shifts, newBreaks, forPay = false)
        else
            null

    if (calculatedShiftsWithinPeriod != null) {
        if (calculatedShiftsWithinPeriodForPay != null) {
            updateBothHoursData (
                shifts = calculatedShiftsWithinPeriod.shifts,
                shiftWorkHours = calculatedShiftsWithinPeriod.shiftHours,
                shiftPayWorkHours = calculatedShiftsWithinPeriodForPay.shiftHours,
                newBreaks = newBreaks,
                needsReconciliation = needsReconciliation
            )
        } else {
            updateHoursData (
                shifts = calculatedShiftsWithinPeriod.shifts,
                shiftWorkHours = calculatedShiftsWithinPeriod.shiftHours,
                newBreaks = newBreaks
            )
        }
    } else if (calculatedShiftsWithinPeriodForPay != null) {
        updatePayHoursData (
            shifts = calculatedShiftsWithinPeriodForPay.shifts,
            shiftPayWorkHours = calculatedShiftsWithinPeriodForPay.shiftHours,
            needsReconciliation = needsReconciliation
        )
    }

}

private fun createAndUpdateEmployeeShiftsWithinPeriods (newShifts: List<Shift>, newBreaks: List<List<Break>?>,
                                                        alsoPay: Boolean) {

    val calculatedShiftsWithinPeriod = getNewWorkHours (
        newShifts,
        newBreaks,
        false,
    )


    if (alsoPay) {
        val calculatedShiftsWithinPeriodForPay = getNewWorkHours(newShifts, newBreaks, true)
        createAndUpdateBothShiftHoursData (
            calculatedShiftsWithinPeriod.newShiftsInsertedAt,
            calculatedShiftsWithinPeriod.shifts,
            calculatedShiftsWithinPeriod.shiftHours,
            calculatedShiftsWithinPeriodForPay.shiftHours,
            newBreaks
        )
    } else {
        createAndUpdateShiftHoursData (
            calculatedShiftsWithinPeriod.newShiftsInsertedAt,
            calculatedShiftsWithinPeriod.shifts,
            calculatedShiftsWithinPeriod.shiftHours,
            newBreaks
        )
    }

}

private fun getNewWorkHours (shifts: List<Shift>, newBreaks: List<List<Break>?>,
                             forPay: Boolean) : UnprocessedShifts {

    val dataQuery = SheetReader.getShiftCalculationData(shifts, newBreaks, forPay)

    val shiftsHours = getShiftsWorkHours (
        dataQuery.shiftsDateTimes,
        dataQuery.shiftsStateEnum,
        dataQuery.shiftsBreaks,
        dataQuery.shiftsClient,
        dataQuery.statesOtDefinitions,
        dataQuery.clientsHolidays,
        dataQuery.startingDayOfWeek,
        forPay
    )

    val unprocessedShifts = UnprocessedShifts(dataQuery.insertedAt, shiftsHours, dataQuery.shifts)

    printShiftCalculationResults(ShiftCalculationResults(dataQuery, unprocessedShifts, forPay))

    return unprocessedShifts

}