package data.clients

import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueType
import data.SQL
import data.DataTable.DataTableType.SingularRootTable

enum class ClientData (override val fieldName: String, override val valueType: ValueType,
                       override val nullable: Boolean) : SingularRootTable {

    CLIENT_PK ("id", ULONG, false);

    override val sqlTable = SQL.SqlTable("account_general_info")
    override fun primaryKey () = CLIENT_PK

}


