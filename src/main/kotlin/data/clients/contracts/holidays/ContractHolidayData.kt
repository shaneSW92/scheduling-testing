package data.clients.contracts.holidays

import data.DataTable.DataTableType.ListTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.SQL

enum class ContractHolidayData (override val fieldName: String, override val valueType: ValueType,
                                override val nullable: Boolean) : ListTable {

    CONTRACT_HOLIDAY_PK ("id", ULONG, false),
    CONTRACT_PK ("account_contract_id", ULONG, false),
    HOLIDAY_PK ("holiday_id", ULONG, false);

    override val sqlTable = SQL.SqlTable("account_contract_holiday")
    override fun primaryKey () = CONTRACT_HOLIDAY_PK
    override fun secondaryKey () = CONTRACT_PK

}