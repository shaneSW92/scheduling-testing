package data

import data.ReadableTable.ValueType

interface DataTable : ReadableTable, SQL.SqlReadable {

    /**
     * This interface describes how the data table should be handled
     */
    interface DataTableType : DataTable {

        /**
         * This type of data table is an inner
         */
        interface ListTable : DataTableType, InnerClass

        interface SingularRootTable : DataTableType, BaseClass

        interface SingularSecondaryTable : DataTableType, InnerClass

        interface IrregularTable : DataTableType, InnerClass {
            val activeRecordConditions: List<QueryCondition>
        }

        interface DataClassType

        interface BaseClass : DataClassType

        interface InnerClass : DataClassType {
            fun secondaryKey () : DataTable
        }

    }

    //override fun getColumn (fieldName: String, valueType: ValueType) =
      //  sqlTable.getColumn(this.fieldName, this.valueType)

}