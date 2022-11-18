package data.employees

import data.DataTable.DataTableType.SingularRootTable
import data.SQL
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*



enum class EmployeeData (override val fieldName: String, override val valueType: ValueType,
                         override val nullable: Boolean) : SingularRootTable {
    EMPLOYEE_PK ("id", ULONG, false),
    //FIRST_NAME ("first_name", VARCHAR, false),
    //LAST_NAME ("last_name", VARCHAR, false),
    //BASE_PAY ("base_pay", FLOAT, true)
    ;

    override val sqlTable = SQL.SqlTable("employee_general_info")
    override fun primaryKey() = EMPLOYEE_PK
}


