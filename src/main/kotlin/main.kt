import data.clients.work_locations.shifts.assignShiftsToEmployee
import data.clients.work_locations.shifts.changePayTimes
import data.clients.work_locations.shifts.clockIn
import data.clients.work_locations.shifts.clockOut
import date_time.DateTime
import java.time.ZoneId

fun main () {

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