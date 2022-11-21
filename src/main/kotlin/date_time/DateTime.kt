package date_time

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

class DateTime (year: UInt, month: UInt, dayOfMonth: UInt, hour: UInt,
                minute: UInt, second: UInt): Comparable<DateTime> {

    private val javaDateTime: LocalDateTime
    val date: Date
    val time: Time

    init {
        javaDateTime = LocalDateTime.of(
            year.toInt(),
            month.toInt(),
            dayOfMonth.toInt(),
            hour.toInt(),
            minute.toInt(),
            second.toInt()
        )
        date = Date(javaDateTime.year.toUInt(), javaDateTime.month.value.toUInt(), javaDateTime.dayOfMonth.toUInt())
        time = Time(javaDateTime.hour.toUInt(), javaDateTime.minute.toUInt(), javaDateTime.second.toUInt())
    }

    constructor (date: Date, time: Time) : this (date.year, date.month, date.dayOfMonth, time.hour, time.minute, time.second)

    constructor (stringToParse: String) : this (LocalDateTime.parse(stringToParse, DateTimeFormatter.ISO_DATE_TIME))

    constructor (zone: ZoneId) : this (LocalDateTime.now(zone))

    constructor () : this (LocalDateTime.now())

    private constructor (localDateTime: LocalDateTime) : this (
        Date(localDateTime.year.toUInt(), localDateTime.month.value.toUInt(), localDateTime.dayOfMonth.toUInt()),
        Time(localDateTime.hour.toUInt(), localDateTime.minute.toUInt(), localDateTime.second.toUInt())
    )

    override operator fun compareTo (other: DateTime): Int = this.javaDateTime.compareTo(other.javaDateTime)

    override fun toString() = javaDateTime.toString() //"${date}T$time"

    fun secondsBetween (dateTime2: DateTime) =
        Duration.between(javaDateTime, dateTime2.javaDateTime).toSeconds().absoluteValue.toULong()

    fun secondsToEndOfDay () = this.secondsBetween(DateTime(date.offsetDays(1), Time(0u, 0u, 0u)))

    fun secondsToStartOfDay () = this.secondsBetween(DateTime(date, Time(0u, 0u, 0u)))

    fun offsetSeconds (offset: Long) =
        if (offset < 0)
            DateTime(this.javaDateTime.minusSeconds(offset.absoluteValue))
        else
            DateTime(this.javaDateTime.plusSeconds(offset.absoluteValue))

    fun duration (other: DateTime) =
        if (this < other)
            this.rangeTo(other)
        else
            other.rangeTo(this)

}