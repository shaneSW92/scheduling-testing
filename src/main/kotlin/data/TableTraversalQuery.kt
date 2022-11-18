package data

data class TableTraversalQuery (val tableName: String, val pkField: String, val desiredField: String,
                           val conditionList: List<QueryCondition>?)