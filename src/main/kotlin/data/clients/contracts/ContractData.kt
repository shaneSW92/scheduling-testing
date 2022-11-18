package data.clients.contracts

import data.DataTable.DataTableType.IrregularTable
import data.QueryCondition
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.SQL
import data.QueryCondition.QueryConditionType.CompareAllConditionType

enum class ContractData (override val fieldName: String, override val valueType: ValueType,
                         override val nullable: Boolean) : IrregularTable {

    CONTRACT_PK ("id", ULONG, false),
    CLIENT_PK ("account_id", ULONG, false),
    CREATED_AT ("created_at", DATETIME, false);

    override fun primaryKey () = CONTRACT_PK
    override fun secondaryKey () = CLIENT_PK
    override val sqlTable = SQL.SqlTable("account_contracts")
    override val activeRecordConditions = listOf (
        QueryCondition (
            "created_at",
            DATETIME,
            CompareAllConditionType.MAXIMUM
        )
    )

}