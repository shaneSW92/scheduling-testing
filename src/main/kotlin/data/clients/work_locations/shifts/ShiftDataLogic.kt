package data.clients.work_locations.shifts

import Messages.prettyError
import Messages.printShiftCalculationResults
import data.SheetReader
import data.SheetReader.createDataMapToValue
import data.SheetReader.createDataMapToValuesMap
import data.SheetReader.getClientPkToHolidaysMap
import data.SheetReader.getShift
import data.SheetReader.getShiftCalculationData
import data.SheetReader.getShiftPkToBreaksMap
import data.SheetReader.getSortedEmployeeShiftsWithinPeriod
import data.SheetReader.getStateEnumToStateOtDefinitionsMap
import data.SheetReader.getWorkLocationMaps
import data.SheetReader.readCsv
import data.clients.contracts.ContractData
import data.clients.contracts.holidays.ContractHolidayData
import data.clients.work_locations.WorkLocationData
import data.clients.work_locations.shifts.breaks.Break
import data.clients.work_locations.shifts.breaks.BreakData
import data.holidays.HolidayData
import data.settings.ScheduleSettingsData
import data.states.StateEnum
import data.states.StateOtDefinitionData
import data.states.StateOtDefinitionEnum
import date_time.Date
import date_time.DateTime
import date_time.Time
import java.time.DayOfWeek
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

object ShiftDataLogic {

    ////////////// SAVING A SHIFT //////////////

    // This is where all the logic starts: when a shift is saved

    fun clockIn (shiftPk: ULong, payStartDateTime: DateTime) {
        updatePayStartTimeData(shiftPk, payStartDateTime)
    }

    fun clockOut (shiftPk: ULong, payEndDateTime: DateTime) {

        val shift = getShift(shiftPk)
        if (shift.payStartDateTime == null)
            throw Exception("Tried to clock out, but the pay_start_date_time is null!")
        if (shift.employeePk == null)
            throw Exception("Tried to clock out, but the employee_id is null!")
        shift.payEndDateTime = payEndDateTime

        updateEmployeeWorkHoursWithinPeriod (shift, newBreaks = null, forBilling = false, forPay = true)

    }

    fun changePayTimes (shiftPk: ULong, payStartDateTime: DateTime, payEndDateTime: DateTime) {

        val shift = getShift(shiftPk)
        shift.payStartDateTime = payStartDateTime
        shift.payEndDateTime = payEndDateTime

        updateEmployeeWorkHoursWithinPeriod (shift, newBreaks = null, forBilling = false, forPay = true)

    }

    fun assignShiftToEmployee (shiftPk: ULong, employeePk: ULong) {

        val shift = getShift(shiftPk)
        shift.employeePk = employeePk

        if (shift.startDateTime < DateTime()) {
            shift.payStartDateTime = shift.startDateTime
            shift.payEndDateTime = shift.endDateTime
            updateEmployeeWorkHoursWithinPeriod(shift, newBreaks = null, forBilling = true, forPay = true)
        } else {
            updateEmployeeWorkHoursWithinPeriod(shift, newBreaks = null, forBilling = true, forPay = false)
        }

    }

    fun createShift (workLocationPk: ULong, startDateTime: DateTime, endDateTime: DateTime, employeePk: ULong?,
                     newBreaks: List<Break>?) {

        val newShift = Shift (
            null,
            employeePk,
            workLocationPk,
            startDateTime,
            endDateTime,
            null,
            null
        )

        if (employeePk == null)
            createOpenShiftRecord(workLocationPk, startDateTime, endDateTime, newBreaks)
        else
            createAndUpdateEmployeeShiftsWithinPeriod(newShift, newBreaks)

    }

    fun editShiftHours (shiftPk: ULong, newStartDateTime: DateTime?, newEndDateTime: DateTime?,
                        newBreaks: List<Break>?, employeePk: ULong?) {

        val shift = getShift(shiftPk)
        if (newStartDateTime != null)
            shift.startDateTime = newStartDateTime
        if (newEndDateTime != null)
            shift.endDateTime = newEndDateTime

        if (employeePk == null) {
            updateOpenShiftRecord(shiftPk, newStartDateTime, newEndDateTime, newBreaks)
        } else if (newBreaks != null) {
            updateEmployeeWorkHoursWithinPeriod(shift, newBreaks, forBilling = true, forPay = true)
        } else {
            updateEmployeeWorkHoursWithinPeriod(shift, null, forBilling = true, forPay = false)
        }

    }

    fun reconcileShift (shiftPk: ULong, startDateTime: DateTime, endDateTime: DateTime, payStartDateTime: DateTime,
                        payEndDateTime: DateTime) {

        val shift = getShift(shiftPk)
        shift.startDateTime = startDateTime
        shift.endDateTime = endDateTime
        shift.payStartDateTime = payStartDateTime
        shift.payEndDateTime = payEndDateTime

        updateEmployeeWorkHoursWithinPeriod (shift, null, forBilling = true, forPay = true)

    }

    ////////////// INTERMEDIARY LOGIC FUNCTIONS //////////////

    // The shift-saving functions share common functionality, so we combine them here

    data class UnprocessedShifts (val newShiftInsertedAt: Int, val shiftHours: List<WorkHours>,
                                  val shifts: List<Shift>)

    // This class is mainly used for printing and debugging
    data class ShiftCalculationResults (val dataQuery: SheetReader.ShiftCalculationDataQuery,
                                        val shifts: UnprocessedShifts, val forPay: Boolean)

    private fun updateEmployeeWorkHoursWithinPeriod (shift: Shift, newBreaks: List<Break>?,
                                                     forPay: Boolean, forBilling: Boolean) {

        val calculatedShiftsWithinPeriodForPay =
            if (forPay)
                getNewWorkHours (shift, newBreaks, forPay = true)
            else
                null

        val calculatedShiftsWithinPeriod =
            if (forBilling)
                getNewWorkHours (shift, newBreaks, forPay = false)
            else
                null

        if (calculatedShiftsWithinPeriod != null) {
            if (calculatedShiftsWithinPeriodForPay != null) {
                updateBothHoursData (
                    calculatedShiftsWithinPeriod.shifts,
                    calculatedShiftsWithinPeriod.shiftHours,
                    calculatedShiftsWithinPeriodForPay.shiftHours,
                    newBreaks
                )
            } else {
                updateHoursData (
                    calculatedShiftsWithinPeriod.shifts,
                    calculatedShiftsWithinPeriod.shiftHours,
                    newBreaks
                )
            }
        } else if (calculatedShiftsWithinPeriodForPay != null) {
            updatePayHoursData (
                calculatedShiftsWithinPeriodForPay.shifts,
                calculatedShiftsWithinPeriodForPay.shiftHours
            )
        }

    }

    private fun createAndUpdateEmployeeShiftsWithinPeriod (newShift: Shift, newBreaks: List<Break>?) {

        val calculatedShiftsWithinPeriod = getNewWorkHours (
            newShift,
            newBreaks,
            false,
        )

        if (newShift.startDateTime < DateTime()) {
            newShift.payStartDateTime = newShift.startDateTime
            newShift.payEndDateTime = newShift.endDateTime
            val calculatedShiftsWithinPeriodForPay = getNewWorkHours(newShift, newBreaks, true)
            createAndUpdateBothShiftHoursData (
                calculatedShiftsWithinPeriod.newShiftInsertedAt,
                calculatedShiftsWithinPeriod.shifts,
                calculatedShiftsWithinPeriod.shiftHours,
                calculatedShiftsWithinPeriodForPay.shiftHours,
                newBreaks
            )
        } else {
            createAndUpdateShiftHoursData (
                calculatedShiftsWithinPeriod.newShiftInsertedAt,
                calculatedShiftsWithinPeriod.shifts,
                calculatedShiftsWithinPeriod.shiftHours,
                newBreaks
            )
        }

    }

    private fun getNewWorkHours (shift: Shift, newBreaks: List<Break>?, forPay: Boolean): UnprocessedShifts {

        val dataQuery = getShiftCalculationData(shift, newBreaks, forPay)

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

    ////////////// SHIFT DATA WRITE FUNCTIONS //////////////

    // After calculating all the shifts, these functions receive the output and are used to write queries to save
    // the calculated information

    private fun updatePayStartTimeData (shiftPk: ULong, payStartDateTime: DateTime) {
        // Run SQL query to update the "pay_start_date_time" field for this shift
    }

    private fun updatePayHoursData (shifts: List<Shift>, shiftPayWorkHours: List<WorkHours>) {
        // Run SQL query to update all the "pay_???_hours" fields for each shift
        // The given lists are parallel
    }

    private fun updateHoursData (shifts: List<Shift>, shiftWorkHours: List<WorkHours>, newBreaks: List<Break>?) {
        // Run SQL query to update all the "???_hours" fields for each shift
        // The given lists are parallel
    }

    private fun updateBothHoursData (shifts: List<Shift>, shiftWorkHours: List<WorkHours>,
                                     shiftPayWorkHours: List<WorkHours>, newBreaks: List<Break>?) {
        // Run SQL query to update all the "???_hours" fields and "pay_???_hours" fields for each shift
        // The given lists are parallel
    }

    private fun createAndUpdateBothShiftHoursData (newShiftInsertedAt: Int, shifts: List<Shift>,
                                                   shiftsWorkHours: List<WorkHours>,
                                                   shiftsPayWorkHours: List<WorkHours>, newBreaks: List<Break>?) {
        // Run SQL query to create a new shift record with all the new hours, update the other shift hours, and add or
        // update break record(s) if not null.
        // The given lists are parallel
    }

    private fun createAndUpdateShiftHoursData (newShiftInsertedAt: Int, shifts: List<Shift>,
                                               shiftHours: List<WorkHours>, newBreaks: List<Break>?) {
        // Run SQL query to create a new shift record with the shift hours, update the other shift hours, and add or
        // update break record(s) if not null.
        // The given lists are parallel
    }

    private fun createOpenShiftRecord (workLocationPk: ULong, startDateTime: DateTime, endDateTime: DateTime,
                                       newBreaks: List<Break>?) {
        // Run a SQL query to create a new open shift record without any hours and add or
        // update break record(s) if not null
    }

    private fun updateOpenShiftRecord (shiftPk: ULong, newStartDateTime: DateTime?,
                                       newEndDateTime: DateTime?, newBreaks: List<Break>?) {
        // Run a SQL query to update an existing shift record without any hours and add or update break
        // record(s) if not null
    }

    ////////////// SHIFT HOURS CALCULATION FUNCTIONS //////////////

    // Here are all the functions to calculate the shift hours

    // Get the shift hours of each shift, which may be either for billing (datetime is from scheduled times) or
    // for pay (datetime is from pay times). The lists here are all parallel, meaning that each index represents
    // the same shift, and are ordered by the startDateTime from past to future. All inputs here are assumed to be
    // shift data for one employee. The maps are assumed to be filled with the necessary information for each shift.
    fun getShiftsWorkHours (shiftsDateTimes: List<ClosedRange<DateTime>>, shiftsStateEnum: List<StateEnum>,
                            shiftsBreaks: List<List<Break>?>, shiftsClient: List<ULong>,
                            statesOtDefinitions: Map<StateEnum, Map<StateOtDefinitionEnum, Float?>>,
                            clientsHolidays: Map<ULong, List<Date>>, startingDayOfWeek: DayOfWeek,
                            forPay: Boolean): MutableList<WorkHours> {

        // Initialize daily and weekly accumulators, with the key being a PK for the client that the shift's work
        // location is for, and the value being a number of accumulated seconds
        val dailyOtAccumulator = mutableMapOf<ULong, ULong>()
        val weeklyOtAccumulator = mutableMapOf<ULong, ULong>()

        // Initialize date variables to keep track of when to reset the accumulators
        var lastShiftDay: Date? = null
        var nextFirstDayOfWeek: Date? = null

        // Initialize a parallel list to insert hours for each shift
        val shiftHours = mutableListOf<WorkHours>()

        // For each shift (ordered by time), calculate the hours and insert the hours into the list of shift hours
        for ((parallelListIndex, shiftDateTimes) in shiftsDateTimes.withIndex()) {

            // Get the shift's startDateTimes, which may either be the scheduled times or pay times
            val startDateTime = shiftDateTimes.start
            val endDateTime = shiftDateTimes.endInclusive

            // Check whether this shift is on a different day than the last shift, and if so, reset the daily
            // accumulators
            val shiftDay = startDateTime.date
            if (lastShiftDay == null || lastShiftDay != shiftDay) {
                dailyOtAccumulator.clear()
                lastShiftDay = shiftDay
            }

            // Check whether this shift is in a new weekly period, and if so, reset the weekly accumulators
            if (nextFirstDayOfWeek == null) {
                nextFirstDayOfWeek = shiftDay.getFirstDayOfWeek(startingDayOfWeek).offsetDays(7)
            } else if (nextFirstDayOfWeek <= shiftDay) {
                weeklyOtAccumulator.clear()
                nextFirstDayOfWeek = shiftDay.getFirstDayOfWeek(startingDayOfWeek).offsetDays(7)
            }

            // Get the total amount of worked time as well as a list of worked time segments split up from breaks
            val breaks = shiftsBreaks[parallelListIndex]
            val workSeconds = getWorkSecondsAndSegments(startDateTime, endDateTime, breaks)

            // Accumulate the daily overtime by the total worked time for each client
            val clientId = shiftsClient[parallelListIndex]
            if (dailyOtAccumulator[clientId] == null)
                dailyOtAccumulator[clientId] = workSeconds.first
            else
                dailyOtAccumulator[clientId] = dailyOtAccumulator[clientId]!! + workSeconds.first

            // Retrieve the State overtime definitions
            val stateDefinitions = statesOtDefinitions[shiftsStateEnum[parallelListIndex]]

            // Assign temporary State overtime definition values for easy reading
            val dailyOtMin = stateDefinitions?.get(StateOtDefinitionEnum.DAILY_OT)?.toULong()?.times(3600UL)
            val dailyDblotMin = stateDefinitions?.get(StateOtDefinitionEnum.DAILY_DBLOT)?.toULong()?.times(3600UL)
            val weeklyOtMin = stateDefinitions?.get(StateOtDefinitionEnum.WEEKLY_OT)?.toULong()?.times(3600UL)
            val weeklyDblotMin = stateDefinitions?.get(StateOtDefinitionEnum.WEEKLY_DBLOT)?.toULong()?.times(3600UL)

            // If this is for pay, then sum all the acum values; otherwise use the specified client amount
            val dailyAcum =
                if (forPay)
                    dailyOtAccumulator.values.sum()
                else
                    dailyOtAccumulator[clientId]

            // Get the daily overtime amounts
            val dailySeconds = getOvertime (
                workSeconds = workSeconds.first.toLong(),
                otMin = dailyOtMin?.toInt(),
                dblotMin = dailyDblotMin?.toInt(),
                otAccumulated = dailyAcum!!.toLong()
            )

            // Accumulate the weekly overtime by the amount of Daily Normal time for each client
            val dailyNormal = dailySeconds.first.toULong()
            if (weeklyOtAccumulator[clientId] == null)
                weeklyOtAccumulator[clientId] = dailyNormal
            else
                weeklyOtAccumulator[clientId] = weeklyOtAccumulator[clientId]!! + dailyNormal

            // If this is for pay, then sum all the acum values; otherwise use the specified client amount
            val weeklyAcum =
                if (forPay)
                    weeklyOtAccumulator.values.sum()
                else
                    weeklyOtAccumulator[clientId]

            // Get the amounts for weekly overtime based on the amount of Daily Normal
            val weeklySeconds = getOvertime (
                workSeconds = dailyNormal.toLong(),
                otMin = weeklyOtMin?.toInt(),
                dblotMin = weeklyDblotMin?.toInt(),
                otAccumulated = weeklyAcum!!.toLong()
            )

            // Sum the amounts of daily overtime and weekly overtime to get the total amount of Normal time and overtime
            val totalSeconds = getTotalSeconds(dailySeconds, weeklySeconds)

            // Get an amount of holiday time that the shift may be on using the amount of Normal time and
            // the time segments acquired earlier. Holidays are determined by the client contract.
            val holidaySeconds = getHolidaySeconds (
                workedTimeSegments = workSeconds.second,
                normalDailySeconds = totalSeconds.first.toULong(),
                holidays = clientsHolidays[clientId]
            )

            // Convert seconds into hours in order of DBLOT -> OT -> HOL -> (remainder) Normal
            val workHours = getShiftWorkHours (
                workedSeconds = workSeconds.first.toLong(),
                otSeconds = totalSeconds.second.first,
                dblotSeconds = totalSeconds.second.second,
                holidaySeconds = holidaySeconds.toLong()
            )

            // Add work hours for the shift to the parallel list
            shiftHours.add(workHours)

        }

        // Return the list of shift hours
        return shiftHours

    }

    private fun getOvertime (workSeconds: Long, otMin: Int?, dblotMin: Int?,
                             otAccumulated: Long) : Pair<Long, Pair<Long, Long>> {

        // Initialize daily OT/DBLOT variables to 0
        var ot = 0L
        var dblot = 0L

        // If there is a daily DBLOT definition, assign the daily DBLOT variable by using clamps
        if (dblotMin != null)
            dblot = min(workSeconds, max(0L, otAccumulated - dblotMin))

        // If there is a daily OT definition, assign the daily OT variable by using clamps
        if (otMin != null)
            ot = min(workSeconds, max(0L, otAccumulated - otMin)) - dblot

        // The remainder is the amount of Normal time
        val normal = workSeconds - (ot + dblot)

        return Pair(normal, Pair(ot, dblot))

    }

    private fun getTotalSeconds (dailySeconds: Pair<Long, Pair<Long, Long>>,
                                 weeklySeconds: Pair<Long, Pair<Long, Long>>): Pair<Long, Pair<Long, Long>> {

        val dailyOt = dailySeconds.second.first
        val dailyDblot = dailySeconds.second.second
        val weeklyNormal = weeklySeconds.first
        val weeklyOt = weeklySeconds.second.first
        val weeklyDblot = weeklySeconds.second.second

        return Pair (
            weeklyNormal,
            Pair (
                dailyOt+ weeklyOt,
                dailyDblot + weeklyDblot
            )
        )

    }


    private fun getHolidaySecondsSegment (period: ClosedRange<DateTime>, holidays: List<Date>): ULong {

        // Check whether the start time and end time land on a holiday
        val startsOnHoliday = holidays.any { it == period.start.date }
        val endsOnHoliday = holidays.any { it == period.endInclusive.date }

        // Check if the period starts on a holiday
        return if (startsOnHoliday) {
            // Return the full amount if the period starts and ends on a holiday
            if (endsOnHoliday)
                period.start.secondsBetween(period.endInclusive)
            // Otherwise, get the number of hours to the end of the first day
            else
                period.start.secondsToEndOfDay()
        // Otherwise, if the period ends on a holiday, then get the number of hours on the second day
        } else if (endsOnHoliday) {
            period.endInclusive.secondsToStartOfDay()
        // Otherwise, return zero (the code shouldn't reach here since the calling function would already know this)
        } else 0u

    }

    private fun getHolidaySeconds (workedTimeSegments: List<ClosedRange<DateTime>>, normalDailySeconds: ULong,
                                   holidays: List<Date>?): ULong {

        // If there are no holidays then don't bother
        if (holidays == null)
            return 0UL
        else {

            // Initialize an accumulator variable for total holiday time and a consumer variable for allotted time
            var totalHolidaySeconds = 0UL
            var secondsConsumed = 0UL

            // For each segment of worked time (separated by breaks), accumulate the holiday time to a maximum of
            // the allotted Daily Normal time
            for (period in workedTimeSegments.sortedBy { it.start }) {
                // Get the total amount of time of the period
                val secondsOfPeriod = period.start.secondsBetween(period.endInclusive)
                // Get the amount of holiday the period covers
                val holidaySeconds = getHolidaySecondsSegment(period, holidays)
                // If the amount of holiday will go over the allotted time, then clamp and return the maximum amount
                if (secondsConsumed + secondsOfPeriod >= normalDailySeconds) {
                    return totalHolidaySeconds + min (
                        normalDailySeconds - secondsConsumed,
                        holidaySeconds
                    )
                // Otherwise, continue accumulating the amount of holiday
                } else {
                    secondsConsumed += secondsOfPeriod
                    totalHolidaySeconds += holidaySeconds
                }
            }
            // The code should never reach here, assuming the daily normal
            // seconds is less or equal to the sum of time segments
            throw Exception(
                "Tried to get holiday seconds but the time periods provided appear to be " +
                        "larger than the allotted normal daily seconds!"
            )

        }

    }

    private fun getShiftWorkHours (workedSeconds: Long, otSeconds: Long, dblotSeconds: Long,
                                   holidaySeconds: Long): WorkHours {

        // First get the total worked Hours as the base
        val workedHours = ((workedSeconds.toDouble() / 60 / 60) * 100).roundToInt().toFloat() / 100

        // Then get the DBLOT hours
        val dblotHours = ((dblotSeconds.toDouble() / 60 / 60) * 100).roundToInt().toFloat() / 100

        // Get the OT hours, clamping the remainder to make sure the sum of hours is not greater than the worked hours
        val otHours = min (
            ((otSeconds.toDouble() / 60 / 60) * 100).roundToInt().toFloat() / 100,
            workedHours - dblotHours
        )

        // Continue as before for the holiday hours
        val holidayHours = min (
            ((holidaySeconds.toDouble() / 60 / 60) * 100).roundToInt().toFloat() / 100,
            workedHours - dblotHours - otHours
        )

        // The remainder is what the Normal hours will equate to, which may be 0, but never negative, assuming the
        // clamping was successful
        val normalHours = workedHours - (dblotHours + otHours + holidayHours)

        return WorkHours (
            workedHours,
            normalHours,
            otHours,
            dblotHours,
            holidayHours
        )

    }

    private fun getWorkSecondsAndSegments (startDateTime: DateTime, endDateTime: DateTime,
                                           breaks: List<Break>?) : Pair<ULong, MutableList<ClosedRange<DateTime>>> {

        // Initialize a list of worked time segments to insert datetime ranges into
        val workedTimeSegments = mutableListOf<ClosedRange<DateTime>>()

        // Get the shift's unworked breaks sorted by datetime, where unworked breaks are breaks that aren't
        // premium breaks (and are meal breaks, but that has not been implemented yet, so the meal check can be
        // ignored for now)
        val unworkedBreaks = breaks?.filter { brk ->
            !(brk.isPremium) && (brk.isMeal)
        }?.sortedBy { breakData ->
            breakData.startDateTime
        }

        // If there are any breaks, for each break, insert a period of worked time into the list
        var workStart = startDateTime
        if (!unworkedBreaks.isNullOrEmpty()) {
            for (brk in unworkedBreaks) {

                // Add the range from the start of work to the start of a break to the list
                workedTimeSegments.add(workStart.rangeTo(brk.startDateTime))

                // Convert minutes to seconds
                val breakDurationSeconds = brk.durationMinutes.toLong() * 60

                // Get the datetime of the end of the break
                val breakEnd = brk.startDateTime.offsetSeconds(breakDurationSeconds)

                // Reassign the start of work to the end of the break
                workStart = breakEnd

            }
        }

        // Insert the remaining amount of worked time into the list, which would be the full time
        // if there were no breaks
        workedTimeSegments.add(workStart.rangeTo(endDateTime))

        // Return the sum of total worked time and the ordered list of worked time segments
        return Pair (
            workedTimeSegments.sumOf { range ->
                range.start.secondsBetween(range.endInclusive)
            },
            workedTimeSegments
        )

    }

}