package data.clients.work_locations.shifts

import data.clients.ClientData
import data.clients.work_locations.WorkLocationData
import data.employees.EmployeeData
import data.states.StateOtDefinitionData
import date_time.DateTime
import edm.GlobalEdmCode
import rate.PayRate

data class PayrollApiEntry (val shift: ShiftData, val employee: EmployeeData, val client: ClientData,
                            val workLocation: WorkLocationData, val stateOtDefinition: StateOtDefinitionData,
                            val startTime: DateTime, val endTime: DateTime, val hours: Float,
                            val earningCode: GlobalEdmCode, val earningCodeEdm: String,
                            val basePayRate: PayRate, val secondPayRate: PayRate?)