package data.clients.work_locations

import data.DataTable.DataTableType.ListTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.SQL



enum class WorkLocationData (override val fieldName: String, override val valueType: ValueType,
                             override val nullable: Boolean) : ListTable {

    WORK_LOCATION_PK ("post_location_id", ULONG, false),
    CLIENT_PK ("account_id", ULONG, false),
    STATE_INDEX ("post_location_state", ULONG, false),
    //MINIMUM_WAGE ("minimum_pay_rate", FLOAT, false),
    //BILL_RATE ("bill_rate", FLOAT, false),
    //SPECIALIZED_RATE ("unarmed_bill_rate", FLOAT, true)
    ;

    override val sqlTable = SQL.SqlTable("account_post_locations")
    override fun primaryKey () = WORK_LOCATION_PK
    override fun secondaryKey() = CLIENT_PK

}