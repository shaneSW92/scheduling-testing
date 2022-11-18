package data

import org.ktorm.schema.*
import data.ReadableTable.ValueType

interface SQL {

    class SqlTable (tableName: String): Table<Nothing>(tableName) {

        /*
        fun getColumn (fieldName: String, valueType: ValueType) = when (valueType) {
            ReadableTable.ValueTypeComparable.INT -> int(fieldName)
            ReadableTable.ValueTypeComparable.UINT -> int(fieldName)
            ReadableTable.ValueTypeComparable.LONG -> long(fieldName)
            ReadableTable.ValueTypeComparable.ULONG -> long(fieldName)
            ReadableTable.ValueTypeComparable.FLOAT -> float(fieldName)
            ReadableTable.ValueTypeNotComparable.TEXT -> text(fieldName)
            ReadableTable.ValueTypeNotComparable.VARCHAR -> varchar(fieldName)
            ReadableTable.ValueTypeNotComparable.BOOLEAN -> boolean(fieldName)
            ReadableTable.ValueTypeComparable.DATETIME -> datetime(fieldName)
            ReadableTable.ValueTypeComparable.DATE -> date(fieldName)
            ReadableTable.ValueTypeComparable.TIME -> time(fieldName)
            ReadableTable.ValueTypeNotComparable.WEEKDAY -> varchar(fieldName)
        }
         */

    }

    interface SqlReadable {
        val sqlTable: SqlTable
        //fun getColumn (fieldName: String, valueType: ValueType): Column<*>
    }

}