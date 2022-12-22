import data.clients.work_locations.shifts.*
import data.clients.work_locations.shifts.breaks.Break
import data.states.StateEnum
import data.states.StateOtDefinitionEnum
import date_time.Date
import date_time.DateTime
import java.time.DayOfWeek
import kotlin.math.roundToLong

fun main () {

    val statesOtDefs = mapOf (
        StateEnum.TX to mapOf (
            StateOtDefinitionEnum.WEEKLY_OT to 40f,
            StateOtDefinitionEnum.WEEKLY_DBLOT to null,
            StateOtDefinitionEnum.DAILY_OT to null,
            StateOtDefinitionEnum.DAILY_DBLOT to null
        ),
        StateEnum.CA to mapOf (
            StateOtDefinitionEnum.WEEKLY_OT to 40f,
            StateOtDefinitionEnum.WEEKLY_DBLOT to null,
            StateOtDefinitionEnum.DAILY_OT to 8f,
            StateOtDefinitionEnum.DAILY_DBLOT to 12f
        )
    )

    val sharedHolidays = listOf (
        Date(2022u, 8u, 1u),
        Date(2022u, 8u, 3u),
        Date(2022u, 8u, 8u)
    )

    val clientsHolidays = mapOf (
        1UL to sharedHolidays,
        2UL to sharedHolidays
    )

    val startingDayOfWeek = DayOfWeek.MONDAY

    fun printPayAndBillHoursForEmployee (shiftsDateTimes: List<ClosedRange<DateTime>>, shiftsBreaks: List<List<Break>>) {


        val shiftsStates = List (shiftsDateTimes.size) { StateEnum.TX }

        val shiftsClients = List (shiftsDateTimes.size) { 1UL }

        val billableOt = List (shiftsDateTimes.size) { false }

        val billableTraining = List (shiftsDateTimes.size) { null }

        val payHours = getShiftsWorkHours (
            shiftsDateTimes,
            shiftsStates,
            shiftsBreaks,
            shiftsClients,
            billableOt = null,
            billableTraining = null,
            statesOtDefs,
            clientsHolidays,
            startingDayOfWeek
        )

        val billHours = getShiftsWorkHours (
            shiftsDateTimes,
            shiftsStates,
            shiftsBreaks,
            shiftsClients,
            billableOt,
            billableTraining,
            statesOtDefs,
            clientsHolidays,
            startingDayOfWeek
        )

        println()
        for (index in 0 until payHours.size) {
            println("Shift ${index + 1}:")
            println("\t${shiftsDateTimes[index].start} -> ${shiftsDateTimes[index].endInclusive}")
            println("\tBreaks:")
            if (shiftsBreaks[index].isEmpty())
                println("\t\tNone")
            else
                for (brk in shiftsBreaks[index]) {
                    val breakType =
                        if (brk.isPremium)
                            "PBRK (Premium break)"
                        else if (brk.isPaid)
                            "PABRK (Paid break)"
                        else
                            "BRK (Non-paid break)"
                    println("\t\t--- $breakType ---")
                    println("\t\t\t${brk.startDateTime}")
                    println("\t\t\t${brk.durationMinutes} min")
                    println("\t\t\t${((brk.durationMinutes.toFloat() / 60) * 100).roundToLong().toFloat() / 100} hr")
                }
            println("\tWorked hours: ${payHours[index]?.workedHours}")
            println("\tNormal hours:\n" +
                    "\t\tPay: ${payHours[index]?.normalHours}\n" +
                    "\t\tBill: ${billHours[index]?.normalHours}")
            println("\tOt hours:\n" +
                    "\t\tPay: ${payHours[index]?.otHours}\n" +
                    "\t\tBill: ${billHours[index]?.otHours}")
            println("\tDblot hours:\n" +
                    "\t\tPay: ${payHours[index]?.dblotHours}\n" +
                    "\t\tBill: ${billHours[index]?.dblotHours}")
            println("\tHoliday hours:\n" +
                    "\t\tPay: ${payHours[index]?.holidayHours}\n" +
                    "\t\tBill: ${billHours[index]?.holidayHours}")
            println()
        }

    }

    val shiftA1Start = DateTime(2022u, 8u, 2u, 0u, 0u, 0u)
    val shiftA1End = DateTime(2022u, 8u, 2u, 20u, 0u, 0u)
    val breaksA1 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 2u, 12u, 0u, 0u),
            20u,
            false,
            false,
            true
        )
    )
    val shiftA2Start = DateTime(2022u, 8u, 3u, 4u, 0u, 0u)
    val shiftA2End = DateTime(2022u, 8u, 4u, 3u, 0u, 0u)
    val breaksA2 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 12u, 0u, 0u),
            25u,
            false,
            false,
            true
        ),
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 21u, 0u, 0u),
            25u,
            false,
            false,
            true
        )
    )

    val employeeA = listOf (
        shiftA1Start.rangeTo(shiftA1End),
        shiftA2Start.rangeTo(shiftA2End)
    )
    val employeeABreaks = listOf (
        breaksA1,
        breaksA2
    )

    println("EMPLOYEE A")
    printPayAndBillHoursForEmployee(employeeA, employeeABreaks)

    val shiftB1Start = DateTime(2022u, 8u, 1u, 1u, 0u, 0u)
    val shiftB1End = DateTime(2022u, 8u, 1u, 21u, 0u, 0u)
    val breaksB1 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 1u, 12u, 0u, 0u),
            10u,
            false,
            false,
            true
        )
    )
    val shiftB2Start = DateTime(2022u, 8u, 2u, 5u, 0u, 0u)
    val shiftB2End = DateTime(2022u, 8u, 3u, 3u, 10u, 0u)
    val breaksB2 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 0u, 0u, 0u),
            5u,
            false,
            false,
            true
        )
    )
    val shiftB3Start = DateTime(2022u, 8u, 3u, 9u, 0u, 0u)
    val shiftB3End = DateTime(2022u, 8u, 3u, 10u, 40u, 0u)
    val breaksB3 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 9u, 20u, 0u),
            20u,
            false,
            false,
            true
        ),
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 10u, 0u, 0u),
            20u,
            false,
            false,
            true
        )
    )

    val employeeB = listOf (
        shiftB1Start.rangeTo(shiftB1End),
        shiftB2Start.rangeTo(shiftB2End),
        shiftB3Start.rangeTo(shiftB3End)
    )
    val employeeBBreaks = listOf (
        breaksB1,
        breaksB2,
        breaksB3
    )

    println("EMPLOYEE B")
    printPayAndBillHoursForEmployee(employeeB, employeeBBreaks)

    val shiftC1Start = DateTime(2022u, 8u, 2u, 22u, 10u, 0u)
    val shiftC1End = DateTime(2022u, 8u, 3u, 2u, 10u, 0u)
    val breaksC1 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 2u, 22u, 55u, 0u),
            10u,
            false,
            false,
            true
        ),
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 0u, 55u, 0u),
            10u,
            false,
            false,
            true
        )
    )
    val shiftC2Start = DateTime(2022u, 8u, 3u, 22u, 10u, 0u)
    val shiftC2End = DateTime(2022u, 8u, 4u, 2u, 10u, 0u)
    val breaksC2 = listOf (
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 3u, 22u, 55u, 0u),
            10u,
            false,
            false,
            true
        ),
        Break (
            0u,
            0u,
            DateTime(2022u, 8u, 4u, 0u, 55u, 0u),
            10u,
            false,
            false,
            true
        )
    )
    val shiftC3Start = DateTime(2022u, 8u, 4u, 23u, 0u, 0u)
    val shiftC3End = DateTime(2022u, 8u, 5u, 2u, 0u, 0u)
    val breaksC3 = listOf<Break>()
    val shiftC4Start = DateTime(2022u, 8u, 7u, 23u, 0u, 0u)
    val shiftC4End = DateTime(2022u, 8u, 8u, 2u, 0u, 0u)
    val breaksC4 = listOf<Break>()

    val employeeC = listOf (
        shiftC1Start.rangeTo(shiftC1End),
        shiftC2Start.rangeTo(shiftC2End),
        shiftC3Start.rangeTo(shiftC3End),
        shiftC4Start.rangeTo(shiftC4End)
    )
    val employeeCBreaks = listOf (
        breaksC1,
        breaksC2,
        breaksC3,
        breaksC4
    )

    println("EMPLOYEE C")
    printPayAndBillHoursForEmployee(employeeC, employeeCBreaks)

    val shiftD1Start = DateTime(2022u, 8u, 1u, 3u, 40u, 0u)
    val shiftD1End = DateTime(2022u, 8u, 2u, 0u, 0u, 0u)
    val breaksD1 = listOf<Break>()
    val shiftD2Start = DateTime(2022u, 8u, 2u, 0u, 0u, 0u)
    val shiftD2End = DateTime(2022u, 8u, 2u, 20u, 20u, 0u)
    val breaksD2 = listOf<Break>()
    val shiftD3Start = DateTime(2022u, 8u, 2u, 23u, 0u, 0u)
    val shiftD3End = DateTime(2022u, 8u, 3u, 2u, 0u ,0u)
    val breaksD3 = listOf<Break>()
    val shiftD4Start = DateTime(2022u, 8u, 3u, 12u, 0u, 0u)
    val shiftD4End = DateTime(2022u, 8u, 3u, 15u, 0u, 0u)
    val breaksD4 = listOf<Break>()
    val shiftD5Start = DateTime(2022u, 8u, 3u, 23u, 0u, 0u)
    val shiftD5End = DateTime(2022u, 8u, 4u, 2u, 0u, 0u)
    val breaksD5 = listOf<Break>()
    val shiftD6Start = DateTime(2022u, 8u, 7u, 23u, 0u, 0u)
    val shiftD6End = DateTime(2022u, 8u, 8u, 2u, 0u, 0u)
    val breaksD6 = listOf<Break>()

    val employeeD = listOf (
        shiftD1Start.rangeTo(shiftD1End),
        shiftD2Start.rangeTo(shiftD2End),
        shiftD3Start.rangeTo(shiftD3End),
        shiftD4Start.rangeTo(shiftD4End),
        shiftD5Start.rangeTo(shiftD5End),
        shiftD6Start.rangeTo(shiftD6End)
    )
    val employeeDBreaks = listOf (
        breaksD1,
        breaksD2,
        breaksD3,
        breaksD4,
        breaksD5,
        breaksD6
    )

    println("EMPLOYEE D")
    printPayAndBillHoursForEmployee(employeeD, employeeDBreaks)

    val shiftE1Start = DateTime(2022u, 8u, 1u, 0u, 0u, 0u)
    val shiftE1End = DateTime(2022u, 8u, 1u, 20u, 0u, 0u)
    val breaksE1 = listOf<Break>()
    val shiftE2Start = DateTime(2022u, 8u, 2u, 0u, 0u, 0u)
    val shiftE2End = DateTime(2022u, 8u, 2u, 20u, 0u, 0u)
    val breaksE2 = listOf<Break>()
    val shiftE3Start = DateTime(2022u, 8u, 2u, 20u, 0u, 0u)
    val shiftE3End = DateTime(2022u, 8u, 2u, 21u, 0u, 0u)
    val breaksE3 = listOf<Break>()

    val employeeE = listOf (
        shiftE1Start.rangeTo(shiftE1End),
        shiftE2Start.rangeTo(shiftE2End),
        shiftE3Start.rangeTo(shiftE3End)
    )
    val employeeEBreaks = listOf (
        breaksE1,
        breaksE2,
        breaksE3
    )

    println("EMPLOYEE E")
    printPayAndBillHoursForEmployee(employeeE, employeeEBreaks)

    val shiftF1Start = DateTime(2022u, 8u, 1u, 0u, 0u, 0u)
    val shiftF1End = DateTime(2022u, 8u, 1u, 20u, 0u, 0u)
    val breaksF1 = listOf<Break>()
    val shiftF2Start = DateTime(2022u, 8u, 2u, 0u, 0u, 0u)
    val shiftF2End = DateTime(2022u, 8u, 2u, 20u, 0u, 0u)
    val breaksF2 = listOf<Break>()
    val shiftF3Start = DateTime(2022u, 8u, 3u, 0u, 0u, 0u)
    val shiftF3End = DateTime(2022u, 8u, 3u, 1u, 0u, 0u)
    val breaksF3 = listOf<Break>()

    val employeeF = listOf (
        shiftF1Start.rangeTo(shiftF1End),
        shiftF2Start.rangeTo(shiftF2End),
        shiftF3Start.rangeTo(shiftF3End)
    )
    val employeeFBreaks = listOf (
        breaksF1,
        breaksF2,
        breaksF3
    )

    println("EMPLOYEE F")
    printPayAndBillHoursForEmployee(employeeF, employeeFBreaks)

    val shiftG1Start = DateTime(2022u, 8u, 1u, 0u, 0u, 0u)
    val shiftG1End = DateTime(2022u, 8u, 1u, 20u, 0u, 0u)
    val breaksG1 = listOf<Break>()
    val shiftG2Start = DateTime(2022u, 8u, 3u, 0u, 0u, 0u)
    val shiftG2End = DateTime(2022u, 8u, 3u, 21u, 0u, 0u)
    val breaksG2 = listOf<Break>()

    val employeeG = listOf (
        shiftG1Start.rangeTo(shiftG1End),
        shiftG2Start.rangeTo(shiftG2End)
    )
    val employeeGBreaks = listOf (
        breaksG1,
        breaksG2
    )

    println("EMPLOYEE G")
    printPayAndBillHoursForEmployee(employeeG, employeeGBreaks)

    val shiftH1Start = DateTime(2022u, 8u, 1u, 0u, 0u, 0u)
    val shiftH1End = DateTime(2022u, 8u, 1u, 20u, 20u, 0u)
    val breaksH1 = listOf<Break>()
    val shiftH2Start = DateTime(2022u, 8u, 2u, 4u, 20u, 0u)
    val shiftH2End = DateTime(2022u, 8u, 3u, 1u, 0u, 0u)
    val breaksH2 = listOf<Break>()

    val employeeH = listOf (
        shiftH1Start.rangeTo(shiftH1End),
        shiftH2Start.rangeTo(shiftH2End)
    )
    val employeeHBreaks = listOf (
        breaksH1,
        breaksH2
    )

    println("EMPLOYEE H")
    printPayAndBillHoursForEmployee(employeeH, employeeHBreaks)

    println("BREAK CHECK")
    printPayAndBillHoursForEmployee (
        listOf (
            DateTime(2022u, 1u, 1u, 0u, 0u, 0u)
                .rangeTo(DateTime(2022u, 1u, 1u, 1u, 0u, 0u))
        ),
        listOf (
            listOf (
                Break (
                    1u,
                    1u,
                    DateTime(2022u, 1u, 1u, 0u, 0u, 0u),
                    10u,
                    false,
                    false,
                    true
                ),
                Break (
                    1u,
                    1u,
                    DateTime(2022u, 1u, 1u, 0u, 10u, 0u),
                    10u,
                    false,
                    false,
                    true
                )
            )
        )
    )

    /*

    //clockIn(1UL, DateTime(), DateTime())

    val startDateTime1 = DateTime(2022u, 7u, 23u, 0u, 10u, 0u)
    val endDateTime1 = DateTime(2022u, 7u, 23u, 13u, 10u, 0u)
    val payStartDateTime1 = DateTime(2022u, 7u, 23u, 0u, 10u, 0u)
    val payEndDateTime1 = DateTime(2022u, 7u, 23u, 13u, 11u, 0u)

    val startDateTime2 = DateTime(2022u, 7u, 26u, 0u, 10u, 0u)
    val endDateTime2 = DateTime(2022u, 7u, 26u, 13u, 10u, 0u)
    val payStartDateTime2 = DateTime(2022u, 7u, 26u, 0u, 10u, 0u)
    val payEndDateTime2 = DateTime(2022u, 7u, 26u, 13u, 11u, 0u)

    assignShiftsToEmployee (
        listOf(10u, 11u),
        listOf (
            startDateTime1.rangeTo(endDateTime1),
            startDateTime2.rangeTo(endDateTime2)
        ),
        1u,
        1u,
        ZoneId.of("America/Chicago")
    )

    //clockOut(6u, 1u, startDateTime.rangeTo(endDateTime), payStartDateTime.rangeTo(payEndDateTime), 1u)

     */

    /*
    changePayTimes (
        1u,
        1u,
        startDateTime.rangeTo(endDateTime),
        payStartDateTime.rangeTo(payEndDateTime),
        1u,
        true
    )
     */

    //assignShiftToEmployee(8UL, DateTime().rangeTo(DateTime()), 1UL, 1UL, ZoneId.of("America/Chicago"))

}