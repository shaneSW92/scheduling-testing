package data

import date_time.Date
import date_time.DateTime
import date_time.Time
import java.time.DayOfWeek

interface ReadableTable {

    val fieldName: String
    val valueType: ValueType
    val nullable: Boolean

    fun primaryKey () : DataTable

    interface ValueType {
        fun stringToType (stringValue: String?) : Any?
    }

    interface StringToValueComparable : ValueType {
        override fun stringToType (stringValue: String?) : Comparable<*>?
    }

    enum class ValueTypeComparable : StringToValueComparable {
        INT { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringValue.toInt() },
        UINT { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringValue.toUInt() },
        LONG { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringValue.toLong() },
        ULONG { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringValue.toULong() },
        FLOAT { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringValue.toFloat() },
        DATETIME { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else DateTime(stringValue) },
        DATE { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else Date(stringValue) },
        TIME { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else Time(stringValue) }

    }

    enum class ValueTypeNotComparable : ValueType {

        TEXT { override fun stringToType (stringValue: String?) = stringValue },
        VARCHAR { override fun stringToType (stringValue: String?) = stringValue },
        BOOLEAN { override fun stringToType (stringValue: String?) = stringToBool(stringValue) },
        WEEKDAY { override fun stringToType (stringValue: String?) = if (stringValue.isNullOrEmpty()) null else stringToWeekday(stringValue) };

        fun stringToBool (value: String?): Boolean = !value.isNullOrEmpty() && (value == "1" || value.contains('T', true))

        fun stringToWeekday (stringValue: String) : DayOfWeek {
            val lowercase = stringValue.lowercase()
            return if (lowercase.contains("mon")) {
                DayOfWeek.MONDAY
            } else if (lowercase.contains("tue")) {
                DayOfWeek.TUESDAY
            } else if (lowercase.contains("wed")) {
                DayOfWeek.WEDNESDAY
            } else if (lowercase.contains("thu")) {
                DayOfWeek.THURSDAY
            } else if (lowercase.contains("fri")) {
                DayOfWeek.FRIDAY
            } else if (lowercase.contains("sat")) {
                DayOfWeek.SATURDAY
            } else if (lowercase.contains("sun")) {
                DayOfWeek.SUNDAY
            } else throw Exception ("Failed to convert string text \"$stringValue\" to a day of the week!")
        }

    }

}