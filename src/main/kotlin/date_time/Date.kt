package date_time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

/**
 * Months are between 1 - 12, and days are between 1 - 28/31
 */
class Date (year: UInt, month: UInt, day: UInt) : Comparable<Date> {

    private val javaDate: LocalDate
    val year: UInt
    val month: UInt
    val dayOfMonth: UInt
    val dayOfWeek: DayOfWeek

    init {
        javaDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        this.year = javaDate.year.toUInt()
        this.month = javaDate.month.value.toUInt()
        this.dayOfMonth = javaDate.dayOfMonth.toUInt()
        this.dayOfWeek = javaDate.dayOfWeek
    }

    constructor (stringToParse: String) : this (LocalDate.parse(stringToParse, DateTimeFormatter.ISO_DATE))

    override operator fun compareTo (other: Date) = this.javaDate.compareTo(other.javaDate)

    private constructor (date: LocalDate): this (
        date.year.toUInt(),
        date.month.value.toUInt(),
        date.dayOfMonth.toUInt()
    )

    private fun getDifferenceToFirstDayOfWeek (startingDayOfWeek: DayOfWeek): UInt {
        val startingDayOfWeekUInt = startingDayOfWeek.value.toUInt()
        return ((dayOfWeek.value.toUInt() + 7u) - startingDayOfWeekUInt) % 7u
    }

    fun offsetDays (offset: Int) =
        if (offset < 0)
            Date(this.javaDate.minusDays(offset.absoluteValue.toLong()))
        else
            Date(this.javaDate.plusDays(offset.absoluteValue.toLong()))

    /**
     * Get the date of the first day of the week in the period that the date object is in based on the given starting
     * day of the week, which could be itself
     */
    fun getFirstDayOfWeek (startingDayOfWeek: DayOfWeek): Date {
        val newDate = javaDate.minusDays(getDifferenceToFirstDayOfWeek(startingDayOfWeek).toLong())
        return Date(newDate.year.toUInt(), newDate.month.value.toUInt(), newDate.dayOfMonth.toUInt())
    }

    /**
     * The string of the Date class uses the yyyy-mm-dd format and adds leading zeros including the year
     */
    override fun toString (): String = javaDate.toString()
        //"${leadingZeros(year, 4u)}-${leadingZeros(month, 2u)}-${leadingZeros(dayOfMonth, 2u)}"

    override fun equals (other: Any?): Boolean = when (other) {
        is Date -> other.javaDate == javaDate
        is LocalDate -> other == javaDate
        else -> false
    }

    // IntelliJ automatically generated this. I guess 31 is an important prime number
    override fun hashCode(): Int {
        var result = javaDate.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + month.hashCode()
        result = 31 * result + dayOfMonth.hashCode()
        result = 31 * result + dayOfWeek.hashCode()
        return result
    }


}