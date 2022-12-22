package data.clients.work_locations.shifts

import data.clients.work_locations.shifts.breaks.Break
import data.states.StateEnum
import data.states.StateOtDefinitionEnum
import date_time.Date
import date_time.DateTime
import java.time.DayOfWeek
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

// Get the shift hours of each shift, which may be either for billing (datetime is from scheduled times) or
// for pay (datetime is from pay times). The lists here are all parallel, meaning that each index represents
// the same shift, and are ordered by the startDateTime from past to future. All inputs here are assumed to be
// shift data for one employee. The maps are assumed to be filled with the necessary information for each shift.
fun getShiftsWorkHours (shiftsDateTimes: List<ClosedRange<DateTime>?>, shiftsStateEnum: List<StateEnum>,
                        shiftsBreaks: List<List<Break>?>, shiftsClient: List<ULong>,
                        billableOt: List<Boolean>?, billableTraining: List<Boolean?>?,
                        statesOtDefinitions: Map<StateEnum, Map<StateOtDefinitionEnum, Float?>>,
                        clientsHolidays: Map<ULong, List<Date>?>,
                        startingDayOfWeek: DayOfWeek) : MutableList<WorkHours?> {

    // Initialize daily and weekly accumulators, with the key being a PK for the client that the shift's work
    // location is for, and the value being a number of accumulated seconds
    val dailyOtAccumulator = mutableMapOf<ULong, ULong>()
    val weeklyOtAccumulator = mutableMapOf<ULong, ULong>()

    // Initialize date variables to keep track of when to reset the accumulators
    var lastShiftDay: Date? = null
    var nextFirstDayOfWeek: Date? = null

    // Initialize a parallel list to insert hours for each shift
    val shiftHours = mutableListOf<WorkHours?>()

    // For each shift (ordered by time), calculate the hours and insert the hours into the list of shift hours
    for ((parallelListIndex, shiftDateTimes) in shiftsDateTimes.withIndex()) {

        // The datetimes will be null if this shift does not apply, which may be the case if multiple shifts were
        // being edited and only some of them have pay times (this calculation would be for pay).
        // If so, add a null value to the list and skip.
        if (shiftDateTimes == null) {
            shiftHours.add(null)
            continue
        }

        // If this is a billing calculation (billable training list is not null), the shift is
        // a training shift (value in the list is not null), and the shift is not billable (value is false), then set
        // all hours to 0 and skip (do not accumulate anything)
        if (billableTraining?.get(parallelListIndex) == false) {
            shiftHours.add(WorkHours(0f, 0f, 0f, 0f, 0f))
            continue
        }

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
        val clientId = shiftsClient[parallelListIndex]

        // Accumulate the daily overtime by the total worked time for each client
        if (dailyOtAccumulator[clientId] == null)
            dailyOtAccumulator[clientId] = workSeconds.first
        else
            dailyOtAccumulator[clientId] = dailyOtAccumulator[clientId]!! + workSeconds.first

        // Calculate and combine overtime and split the hours between normal and overtime, if applicable
        val totalSeconds =

            // If this calculation is for billing (billable OT list is not null) and overtime is not billable, then
            // accumulate the weekly overtime variable by the full worked time and set overtime to 0
            if (billableOt?.get(parallelListIndex) == false) {

                if (weeklyOtAccumulator[clientId] == null)
                    weeklyOtAccumulator[clientId] = workSeconds.first
                else
                    weeklyOtAccumulator[clientId] = weeklyOtAccumulator[clientId]!! + workSeconds.first

                Pair(workSeconds.first.toLong(), Pair(0L, 0L))

            // Otherwise, the shift is for pay or overtime is billable and overtime must be calculated
            } else {

                // Retrieve the State overtime definitions
                val stateDefinitions = statesOtDefinitions[shiftsStateEnum[parallelListIndex]]

                // Convert State overtime definition values to seconds, if not null
                val dailyOtMin = stateDefinitions?.get(StateOtDefinitionEnum.DAILY_OT)?.times(3600f)?.toULong()
                val dailyDblotMin = stateDefinitions?.get(StateOtDefinitionEnum.DAILY_DBLOT)?.times(3600f)?.toULong()
                val weeklyOtMin = stateDefinitions?.get(StateOtDefinitionEnum.WEEKLY_OT)?.times(3600f)?.toULong()
                val weeklyDblotMin = stateDefinitions?.get(StateOtDefinitionEnum.WEEKLY_DBLOT)?.times(3600f)?.toULong()

                // If this is for pay, then sum all the acum values; otherwise use the specified client amount
                val dailyAcum =
                    if (billableOt.isNullOrEmpty())
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
                    if (billableOt.isNullOrEmpty())
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

                getTotalSeconds(dailySeconds, weeklySeconds)

            }

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
                             weeklySeconds: Pair<Long, Pair<Long, Long>>) : Pair<Long, Pair<Long, Long>> {

    val dailyOt = dailySeconds.second.first
    val dailyDblot = dailySeconds.second.second
    val weeklyNormal = weeklySeconds.first
    val weeklyOt = weeklySeconds.second.first
    val weeklyDblot = weeklySeconds.second.second

    return Pair (
        weeklyNormal,
        Pair (
            dailyOt + weeklyOt,
            dailyDblot + weeklyDblot
        )
    )

}


private fun getHolidaySecondsSegment (period: ClosedRange<DateTime>, holidays: List<Date>) : ULong {

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
                               holidays: List<Date>?) : ULong {

    // If there are no holidays then don't bother
    if (holidays == null)
        return 0UL
    else {

        // Initialize an accumulator variable for total holiday time and a consumer variable for allotted time
        var totalHolidaySeconds = 0UL
        var secondsConsumed = 0UL

        // For each segment of worked time (separated by breaks), accumulate the holiday time to a maximum of
        // the allotted Daily Normal time
        for (period in workedTimeSegments.sortedBy { period -> period.start }) {
            // Get the total amount of time of the period
            val secondsOfPeriod = period.start.secondsBetween(period.endInclusive)
            // If the total worked time plus the seconds consumed is more than the allotted time, accumulate with a new
            // range using the remainder and break out of the loop to return the total
            if (secondsOfPeriod + secondsConsumed >= normalDailySeconds) {
                val newEnd = period.start.offsetSeconds(normalDailySeconds.toLong() - secondsConsumed.toLong())
                totalHolidaySeconds += getHolidaySecondsSegment (
                    period = period.start.rangeTo (
                        newEnd
                    ),
                    holidays = holidays
                )
                break
            // Otherwise, accumulate the consumer and the amount of holiday in the period
            } else {
                secondsConsumed += secondsOfPeriod
                totalHolidaySeconds += getHolidaySecondsSegment(period, holidays)
            }
        }

        return totalHolidaySeconds

    }

}

// Since we want to avoid math operations between floats, we keep a rounded int and shift
//      the decimal place later.
// Here we divide by the number of seconds in an hour, shift 2 decimal places to the left, and
//      round to a truncated int.
// For example, if the seconds to hours results in 1.1875, the result of this equation would be 119.
// With this result, we can use it to compare whether this would result in more or less time than
//      the total worked hours due to rounding multiple times.
fun secondsToHoursAndShiftRoundTruncate (seconds: Long) = ((seconds.toFloat() / 3600) * 100).roundToInt()

fun getShiftWorkHours (workedSeconds: Long, otSeconds: Long, dblotSeconds: Long, holidaySeconds: Long) : WorkHours {

    // First get the total normal seconds, which is the remainder of worked time
    val normalSeconds = workedSeconds - (dblotSeconds + otSeconds + holidaySeconds)

    // Now get the total worked Hours as the base for the operations
    val workedHours = secondsToHoursAndShiftRoundTruncate(workedSeconds)

    // Then get the DBLOT hours, which may equal the full worked hours
    val dblotHours = secondsToHoursAndShiftRoundTruncate(dblotSeconds)

    // Now we begin comparing. If the sum of Normal and Holiday is less than 0.01 hours, then use the remainder.
    // Otherwise, round the value as before, which could be 0.
    val otHours =
        if (normalSeconds + holidaySeconds < 36L)
            workedHours - dblotHours
        else
            secondsToHoursAndShiftRoundTruncate(otSeconds)

    // Compare again for Holiday against the Normal
    val holidayHours =
        if (normalSeconds < 36L)
            workedHours - (dblotHours + otHours)
        else
            secondsToHoursAndShiftRoundTruncate(holidaySeconds)

    // The remainder is what the Normal hours will equate to, which could be 0
    val normalHours = workedHours - (dblotHours + otHours + holidayHours)

    // Finally, we convert the results back to a float and shift the decimal back, to the right, 2 times
    return WorkHours (
        workedHours.toFloat() / 100,
        normalHours.toFloat() / 100,
        otHours.toFloat() / 100,
        dblotHours.toFloat() / 100,
        holidayHours.toFloat() / 100
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

    val workedSecondsSum = workedTimeSegments.sumOf { range -> range.start.secondsBetween(range.endInclusive) }
    // We go ahead and round the total worked time early because we want the overtime to be calculated based on the
    // previous worked hours (not seconds) rounded, because the rounded hours is what is actually being paid to
    // the employee, and the result may not add up to 40 (or 8 / 12 if daily overtime) if the base isn't rounded
    // and used throughout the calculation. Here we convert the seconds to hours, round it, and then convert it back
    // to seconds.
    val sumRounded = ((secondsToHoursAndShiftRoundTruncate(workedSecondsSum.toLong()).toFloat() / 100) * 3600).toULong()

    // Return the sum of total worked time and the ordered list of worked time segments
    return Pair (
        sumRounded,
        workedTimeSegments
    )

}