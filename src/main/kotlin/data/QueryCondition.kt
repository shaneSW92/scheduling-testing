package data

data class QueryCondition (val conditionField : String, val conditionValueType: ReadableTable.ValueTypeComparable,
                           val conditionType: QueryConditionType) {

    interface QueryConditionType {

        enum class NullConditionType : QueryConditionType {
            IS_NULL,
            IS_NOT_NULL
        }

        enum class CompareAllConditionType : QueryConditionType {
            MINIMUM,
            MAXIMUM
        }

        enum class CompareConditionType {
            GREATER_THAN,
            LESS_THAN,
            EQUAL_TO
        }

        data class CompareQuery (val toCompare: String, val valueType: ReadableTable.StringToValueComparable,
                                 val compareType: CompareConditionType) : QueryConditionType

    }

}