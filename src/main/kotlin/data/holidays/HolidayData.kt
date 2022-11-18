package data.holidays

import data.DataTable.DataTableType.SingularRootTable
import data.ReadableTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueTypeNotComparable.*
import data.SQL



enum class HolidayData (override val fieldName: String, override val valueType: ValueType,
                        override val nullable: Boolean): SingularRootTable {

    HOLIDAY_PK ("id", ULONG, false),
    NAME ("holiday_name", VARCHAR, false),
    DATE ("holiday_date", ReadableTable.ValueTypeComparable.DATE, false);

    override val sqlTable = SQL.SqlTable("account_holiday")
    override fun primaryKey () = HOLIDAY_PK

}

