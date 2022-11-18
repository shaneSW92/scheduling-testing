package data.states

import data.SQL
import data.DataTable.DataTableType.ListTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueTypeNotComparable.*

enum class StateOtDefinitionData (override val fieldName: String, override val valueType: ValueType,
                                  override val nullable: Boolean) : ListTable {

    STATE_OT_DEFINITION_PK ("id", ULONG, false),
    STATE_PK ("state_id", ULONG, false),
    PERIOD ("period", TEXT, false),
    MINIMUM_HOURS ("minimum_hours", FLOAT, false);

    override val sqlTable = SQL.SqlTable("state_overtime_earning")
    override fun primaryKey () = STATE_OT_DEFINITION_PK
    override fun secondaryKey() = STATE_PK

}