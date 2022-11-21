package data.clients.work_locations.shifts

import data.SQL
import data.DataTable
import data.ReadableTable.ValueType
import data.ReadableTable.ValueTypeComparable.*
import data.ReadableTable.ValueTypeNotComparable.*

enum class ShiftData (override val fieldName: String, override val valueType: ValueType,
                      override val nullable: Boolean) : DataTable.DataTableType.ListTable {

        SHIFT_PK ("id", ULONG, false),
        WORK_LOCATION_PK ("post_location_id", ULONG, false),
        EMPLOYEE_PK ("employee_id", ULONG, true),
        NEEDS_RECONCILIATION ("????", BOOLEAN, true),
        START_DATE ("shift_start_date", DATE, false),
        START_TIME ("shift_start_time", TIME, false),
        END_DATE ("shift_start_time", DATE, false),
        END_TIME ("shift_start_time", TIME, false),
        WORKED_HOURS ("worked_hours", FLOAT, true),
        NORMAL_HOURS ("normal_hours", FLOAT, true),
        OT_HOURS ("ot_hours", FLOAT, true),
        DBLOT_HOURS ("dblot_hours", FLOAT, true),
        HOLIDAY_HOURS ("holiday_hours", FLOAT, true),
        PAY_START_DATE_TIME ("payroll_shift_start_date_time", DATETIME, true),
        PAY_END_DATE_TIME ("payroll_shift_end_date_time", DATETIME, true),
        PAY_WORKED_HOURS ("pay_worked_hours", FLOAT, true),
        PAY_NORMAL_HOURS ("pay_normal_hours", FLOAT, true),
        PAY_OT_HOURS ("pay_ot_hours", FLOAT, true),
        PAY_DBLOT_HOURS ("pay_dblot_hours", FLOAT, true),
        PAY_HOLIDAY_HOURS ("pay_holiday_hours", FLOAT, true);
        //RATE_CODE (TODO(), TODO(), true),
        //PAY_RATE_OVERRIDE (TODO(), TODO(), true),
        //BILL_RATE_OVERRIDE (TODO(), TODO(), true),
        //BILLABLE_ONLY (TODO(), TODO(), true),
        //TRAINING (TODO(), TODO(), true),
        //PAY_RATE (TODO(), TODO(), true),
        //PAY_RATE_TYPE (TODO(), TODO(), true),
        //PAY_HOLIDAY_MULT (TODO(), TODO(), true),
        //BILL_RATE (TODO(), TODO(), true),
        //BILL_RATE_TYPE (TODO(), TODO(), true),
        //BILL_HOLIDAY_MULT (TODO(), TODO(), true)

        override val sqlTable = SQL.SqlTable("schedule_shifts_new")
        override fun primaryKey () = SHIFT_PK
        override fun secondaryKey() = WORK_LOCATION_PK

}


