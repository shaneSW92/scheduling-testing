package data.clients.work_locations.shifts.breaks

import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueTypeNotComparable.*
import data.SQL
import data.DataTable.DataTableType.ListTable

enum class BreakData (override val fieldName: String, override val valueType: ValueType,
                      override val nullable: Boolean): ListTable {

    BREAK_PK ("id", ULONG, false),
    SHIFT_PK ("shift_id", ULONG, false),
    START_DATE_TIME ("break_start_date_Date_time", DATETIME, false),
    DURATION_MINUTES ("break_duration", UINT, false),
    //PAID_BREAK ("is_paid_break", UINT, true),
    PREMIUM ("is_premium_break", BOOLEAN, true),
    MEAL ("is_meal", BOOLEAN, true),
    //CLOCK_TYPE (TODO(), TODO(), false)
    ;

    override val sqlTable = SQL.SqlTable("scheduler_shifts_break")
    override fun primaryKey () = BREAK_PK
    override fun secondaryKey() = SHIFT_PK

    enum class BreakType {
        MEAL,
        REST
    }

    enum class BreakClockType {
        FLEXIBLE,
        FIXED
    }

}