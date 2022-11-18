import data.clients.work_locations.shifts.ShiftDataLogic
import data.clients.work_locations.shifts.ShiftDataLogic.clockOut
import date_time.DateTime
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.utils.SmartSet.Companion

fun main () {

    clockOut(6u, DateTime(2022u, 7u, 23u, 13u, 10u, 0u))

}