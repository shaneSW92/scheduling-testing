package data.clients.work_locations.shifts

import data.clients.work_locations.shifts.breaks.Break
import date_time.DateTime


fun updatePayStartTimeData (shiftPk: ULong, payStartDateTime: DateTime, needsReconciliation: Boolean) {
    // Run SQL query to update the "pay_start_date_time" field for this shift
    // If the shift needs reconciliation, then also set reconciliation data for this shift
}

fun updatePayEndTimeAndUnreconcileShiftData (shiftPk: ULong, payEndDateTime: DateTime) {
    // Run SQL query to update the "pay_end_date_time" field for this shift and set the shift as unreconciled
}

fun updatePayTimesData (shiftPk: ULong, payDateTimes: ClosedRange<DateTime>) {
    // Run SQL query to update the "pay_start_date_time" and "pay_end_date_time" field for this shift
}

fun updatePayHoursData (shifts: List<Shift>, shiftPayWorkHours: List<WorkHours?>, needsReconciliation: List<Boolean>) {
    // Run SQL query to update all the "pay_???_hours" fields for each shift
    // The given lists are parallel
}

fun updateHoursData (shifts: List<Shift>, shiftWorkHours: List<WorkHours?>, newBreaks: List<List<Break>?>) {
    // Run SQL query to update all the "???_hours" fields for each shift and create/update any new break information
}

fun updateBothHoursData (shifts: List<Shift>, shiftWorkHours: List<WorkHours?>,
                         shiftPayWorkHours: List<WorkHours?>, newBreaks: List<List<Break>?>,
                         needsReconciliation: List<Boolean>) {
    // Run SQL query to update all the "???_hours" fields and "pay_???_hours" fields, the reconciliation status,
    // and create/update any new breaks for each shift
}

fun createAndUpdateBothShiftHoursData (newShiftsInsertedAt: List<Int>, shifts: List<Shift>,
                                       shiftsWorkHours: List<WorkHours?>, shiftsPayWorkHours: List<WorkHours?>,
                                       newBreaks: List<List<Break>?>) {
    // Run SQL query to create 1 or more new shift records with the new hours, update the other shift hours, and add or
    // update any new break records.
}

fun createAndUpdateShiftHoursData (newShiftsInsertedAt: List<Int>, shifts: List<Shift>,
                                   shiftHours: List<WorkHours?>, newBreaks: List<List<Break>?>) {
    // Run SQL query to create a new shift record with the shift hours, update the other shift hours, and add or
    // update any new break records.
}

fun createOpenShiftRecords (workLocationPk: ULong, dateTimesList: List<ClosedRange<DateTime>>, billableOt: Boolean,
                            billableTraining: Boolean?, newBreaks: List<Break>?) {
    // Run a SQL query to create new open shift records without any hours and add or update any new break records
}

fun updateOpenShiftRecords (parallelShiftPks: List<ULong>, parallelDateTimes: List<ClosedRange<DateTime>>,
                            breaks: List<Break>?, billableOt: Boolean, billableTraining: Boolean?) {
    // Run a SQL query to update existing shift records without any hours and add or update any new break records
}