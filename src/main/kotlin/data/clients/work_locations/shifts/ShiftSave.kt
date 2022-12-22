package data.clients.work_locations.shifts

import Messages.printShiftCalculationResults
import data.SheetReader
import data.clients.work_locations.shifts.breaks.Break
import date_time.Date
import date_time.DateTime
import date_time.Time
import java.time.ZoneId


fun clockIn (shiftPk: ULong, startDateTime: DateTime, payStartDateTime: DateTime) {
    updatePayStartTimeData (
        shiftPk = shiftPk,
        payStartDateTime = payStartDateTime,
        // If the clock-in time is more than 1 minute past the scheduled start time, then the shift must be reconciled
        // Note: This may change if a setting is added to customize this
        needsReconciliation = payStartDateTime > startDateTime.offsetSeconds(59)
    )
}

fun clockOut (shiftPk: ULong, workLocationPk: ULong, dateTimes: ClosedRange<DateTime>,
              payDateTimes: ClosedRange<DateTime>, employeePk: ULong, rate: Float, payRate: Float,
              billableOt: Boolean, billableTraining: Boolean?) {

    if (payDateTimes.endInclusive.secondsBetween(dateTimes.endInclusive) > 59u) {

        updatePayEndTimeAndUnreconcileShiftData (
            shiftPk = shiftPk,
            payEndDateTime = payDateTimes.endInclusive
        )

    } else {

        val shift = Shift (
            primaryKey = shiftPk,
            employeePk = employeePk,
            workLocationPk = workLocationPk,
            startDateTime = dateTimes.start,
            endDateTime = dateTimes.endInclusive,
            payStartDateTime = payDateTimes.start,
            payEndDateTime = payDateTimes.endInclusive,
            billableOt = billableOt,
            billableTraining = billableTraining,
            needsReconciliation = false,
            rate = rate,
            payRate = payRate
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
                    payDateTimes: ClosedRange<DateTime>, employeePk: ULong, needsReconciliation: Boolean,
                    rate: Float, payRate: Float) {

    if (needsReconciliation) {

        updatePayTimesData (
            shiftPk = shiftPk,
            payDateTimes = payDateTimes.start.rangeTo(payDateTimes.endInclusive)
        )

    } else {

        val shift = Shift (
            primaryKey = shiftPk,
            employeePk = employeePk,
            workLocationPk = workLocationPk,
            startDateTime = dateTimes.start,
            endDateTime = dateTimes.endInclusive,
            payStartDateTime = payDateTimes.start,
            payEndDateTime = payDateTimes.endInclusive,
            billableOt = false,
            billableTraining = null,
            needsReconciliation = false,
            rate = rate,
            payRate = payRate
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
                           employeePk: ULong, billableOt: Boolean, billableTraining: Boolean?,
                           branchTimeZone: ZoneId) = assignShiftsToEmployee (
    parallelShiftPksList = listOf(shiftPk),
    parallelDateTimesList = listOf(dateTimes),
    workLocationPk = workLocationPk,
    employeePk = employeePk,
    parallelBillableOtList = listOf(billableOt),
    parallelBillableTrainingList = listOf(billableTraining),
    branchTimeZone = branchTimeZone
)

fun assignShiftsToEmployee (parallelShiftPksList: List<ULong>, parallelDateTimesList: List<ClosedRange<DateTime>>,
                            workLocationPk: ULong, employeePk: ULong, parallelBillableOtList: List<Boolean>,
                            parallelBillableTrainingList: List<Boolean?>, branchTimeZone: ZoneId) {

    val dateTimeNow = DateTime(branchTimeZone)

    val shifts = List (parallelShiftPksList.size) { parallelIndex ->
        val startDateTime = parallelDateTimesList[parallelIndex].start
        val endDateTime = parallelDateTimesList[parallelIndex].endInclusive
        val inPast = endDateTime <= dateTimeNow
        val payStartDateTime = if (inPast) startDateTime else null
        val payEndDateTime = if (inPast) endDateTime else null
        Shift (
            primaryKey =  parallelShiftPksList[parallelIndex],
            employeePk = employeePk,
            workLocationPk = workLocationPk,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            payStartDateTime = payStartDateTime,
            payEndDateTime = payEndDateTime,
            billableOt = parallelBillableOtList[parallelIndex],
            billableTraining = parallelBillableTrainingList[parallelIndex],
            needsReconciliation = false,
            rate = TODO(),
            payRate = TODO()
        )
    }

    val anyPay = shifts.any { shift -> shift.payStartDateTime != null }
    val noReconciliation = List (parallelShiftPksList.size) { false }
    val noNewBreaks = List (parallelShiftPksList.size) { null }

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
                 branchTimeZone: ZoneId, newBreaks: List<Break>?, billableOt: Boolean,
                 billableTraining: Boolean?) = createShifts (
    workLocationPk = workLocationPk,
    dateTimesList = listOf(dateTimes),
    employeePk = employeePk,
    billableOt = billableOt,
    billableTraining = billableTraining,
    branchTimeZone = branchTimeZone,
    newBreaks = newBreaks
)

fun createShifts (workLocationPk: ULong, dateTimesList: List<ClosedRange<DateTime>>,
                  employeePk: ULong?, billableOt: Boolean, billableTraining: Boolean?, branchTimeZone: ZoneId,
                  newBreaks: List<Break>?) {

    if (employeePk == null) {

        createOpenShiftRecords (
            workLocationPk = workLocationPk,
            dateTimesList = dateTimesList,
            billableOt = billableOt,
            billableTraining = billableTraining,
            newBreaks = newBreaks
        )

    } else {

        val dateTimeNow = DateTime(branchTimeZone)

        val newShifts = List (dateTimesList.size) { index ->
            val startDateTime = dateTimesList[index].start
            val endDateTime = dateTimesList[index].endInclusive
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
                billableOt = billableOt,
                billableTraining = billableTraining,
                needsReconciliation = false,
                rate = TODO(),
                payRate = TODO()
            )
        }

        val newBreaksList = List (dateTimesList.size) { newBreaks }

        val anyPay = newShifts.any { shift -> shift.payStartDateTime != null }

        createAndUpdateEmployeeShiftsWithinPeriods (
            newShifts = newShifts,
            newBreaks = newBreaksList,
            alsoPay = anyPay
        )

    }

}

fun editShifts (parallelShiftPksList: List<ULong>, parallelDates: List<ClosedRange<Date>>,
                parallelPayDateTimesList: List<ClosedRange<DateTime>?>, parallelNeedsReconciliation: List<Boolean>,
                times: ClosedRange<Time>, workLocationPk: ULong, breaks: List<Break>?, employeePk: ULong?,
                billableOt: Boolean, billableTraining: Boolean?) {

    val parallelDateTimesList = List (parallelShiftPksList.size) { index ->
        DateTime(parallelDates[index].start, times.start)
            .rangeTo(DateTime(parallelDates[index].endInclusive, times.endInclusive))
    }

    if (employeePk == null) {

        updateOpenShiftRecords(parallelShiftPksList, parallelDateTimesList, breaks, billableOt, billableTraining)

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
                billableOt = billableOt,
                billableTraining = billableTraining,
                needsReconciliation = parallelNeedsReconciliation[index],
                rate = TODO(),
                payRate = TODO()
            )
        }

        updateEmployeeWorkHoursWithinPeriod (
            shifts = shifts,
            newBreaks = List (parallelShiftPksList.size) { breaks },
            forBilling = true,
            forPay = false,
            needsReconciliation = parallelNeedsReconciliation
        )

    }

}

fun reconcileShift (shiftPk: ULong, employeePk: ULong, workLocationPk: ULong, dateTimes: ClosedRange<DateTime>,
                    payDateTimes: ClosedRange<DateTime>, rate: Float, payRate: Float,
                    billableOt: Boolean, billableTraining: Boolean?) {

    val shift = Shift (
        primaryKey = shiftPk,
        employeePk = employeePk,
        workLocationPk = workLocationPk,
        startDateTime = dateTimes.start,
        endDateTime = dateTimes.endInclusive,
        payStartDateTime = payDateTimes.start,
        payEndDateTime = payDateTimes.endInclusive,
        billableOt = billableOt,
        billableTraining = billableTraining,
        needsReconciliation = false,
        rate = rate,
        payRate = payRate
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

private fun getNewWorkHours (shifts: List<Shift>, breaks: List<List<Break>?>,
                             forPay: Boolean) : UnprocessedShifts {

    val dataQuery = SheetReader.getShiftCalculationData(shifts, breaks, forPay)

    val shiftsHours = getShiftsWorkHours (
        dataQuery.shiftsDateTimes,
        dataQuery.shiftsStateEnum,
        dataQuery.shiftsBreaks,
        dataQuery.shiftsClient,
        dataQuery.shiftsBillableOt,
        dataQuery.shiftsBillableTraining,
        dataQuery.statesOtDefinitions,
        dataQuery.clientsHolidays,
        dataQuery.startingDayOfWeek
    )

    val unprocessedShifts = UnprocessedShifts(dataQuery.insertedAt, shiftsHours, dataQuery.shifts)

    printShiftCalculationResults(ShiftCalculationResults(dataQuery, unprocessedShifts, forPay))

    return unprocessedShifts

}