package date_time

import java.time.*
import java.time.format.DateTimeFormatter

/**
 * Hours are between 0 - 23, and minutes are between 0 - 59
 */
class Time (hour: UInt, minute: UInt, second: UInt) : Comparable<Time> {
    private val secondsInDay = 60u * 60u * 24u
    private val javaTime: LocalTime
    val hour: UInt
    val minute: UInt
    val second: UInt
    val seconds = (hour * 60u * 60u) + (minute * 60u) + second

    init {
        this.javaTime = LocalTime.of(hour.toInt(), minute.toInt())
        this.hour = javaTime.hour.toUInt()
        this.minute = javaTime.minute.toUInt()
        this.second = javaTime.second.toUInt()
    }

    constructor (stringToParse: String) : this (LocalTime.parse(stringToParse, DateTimeFormatter.ISO_TIME))

    private constructor (date: LocalTime): this (
        date.hour.toUInt(),
        date.minute.toUInt(),
        date.second.toUInt()
    )

    fun secondsTo (toTime: Time) = ((toTime.seconds + secondsInDay) - seconds) % secondsInDay

    override fun toString(): String = javaTime.toString() //"${leadingZeros(hour, 2u)}-${leadingZeros(minute, 2u)}"

    override fun compareTo (other: Time) = this.javaTime.compareTo(other.javaTime)

}


/*

/**
 * Add leading zeros to numbers based on the given number of digits
 */
private fun leadingZeros (number: UInt, digits: UInt): String {
    var numberString = ""
    for (decimalPlace in 1u until digits)
        if (number < Math.pow(10.0, (decimalPlace).toDouble()).toUInt())
            numberString += '0'
    return numberString + number
}

 */