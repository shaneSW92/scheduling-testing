package data.settings

import data.DataTable.DataTableType.SingularRootTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueTypeNotComparable.*
import data.SQL

enum class ScheduleSettingsData (override val fieldName: String, override val valueType: ValueType,
                                 override val nullable: Boolean) : SingularRootTable {

    SCHEDULE_SETTING_PK ("id", ULONG, false),
    STARTING_DAY_OF_WEEK ("schedule_start_day", WEEKDAY, false);

    override val sqlTable = SQL.SqlTable("schedule_settings")
    override fun primaryKey() = SCHEDULE_SETTING_PK

}
