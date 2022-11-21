package data

import data.DataTable.DataTableType
import Messages.wrongHeaders
import java.io.File
import Messages.failedToFindSheet
import Messages.prettyError
import data.states.StateEnum
import data.states.StateOtDefinitionData
import data.states.StateOtDefinitionEnum
import data.clients.contracts.ContractData
import data.clients.contracts.holidays.ContractHolidayData
import data.clients.work_locations.WorkLocationData
import data.clients.work_locations.shifts.Shift
import data.clients.work_locations.shifts.ShiftData
import data.clients.work_locations.shifts.breaks.Break
import data.clients.work_locations.shifts.breaks.BreakData
import data.holidays.HolidayData
import data.settings.ScheduleSettingsData
import date_time.Date
import date_time.DateTime
import date_time.Time
import java.time.DayOfWeek

/**
 * A class intended to provide methods of reading spreadsheets
 */
object SheetReader {

    /**
     * This function returns a List of Maps where the key represents an Enum and the value is a cell in the CSV file,
     * with the keys / Enums representing the columns of the spreadsheet.
     *
     * Keys / Headers are NOT case-sensitive.
     */
    inline fun <reified Field> readCsv () :
            List<Map<Field, Any?>> where Field: Enum<Field>, Field: DataTableType {

        // First get the file and verify data was retrieved, or otherwise return an empty List
        val className = Field::class.simpleName
        val filepath = "sheets/$className.csv"
        val fileReader = File(filepath)
        val fileRows = try { fileReader.readLines() } catch (exception: Exception) {
            throw Exception(failedToFindSheet(className, filepath))
        }
        if (fileRows.isEmpty())
            throw Exception("The following filepath contains no information! :\n$filepath")


        // Assign the Enums to the column indices as shown in the spreadsheet
        val headers = getCsvRow(fileRows[0])
        if (headers.size != enumValues<Field>().size) {
            wrongHeaders (
                "There are too many or too few headers!",
                enumValues<Field>()
            )
            return listOf()
        }
        var headerValue: String
        val columnsOfEnums =
            List(enumValues<Field>().size) { column: Int ->
                headerValue = headers[column].uppercase()
                try {
                    enumValueOf<Field>(headerValue)
                } catch (exception: IllegalArgumentException) {
                    headerValue = headers[column]
                    for (enum in enumValues<Field>())
                        if (enum.fieldName == headerValue)
                            enumValueOf<Field>(enum.name)
                    wrongHeaders (
                        "The header \"$headerValue\" is not a valid column header!",
                        enumValues<Field>()
                    )
                    return listOf()
                }
            }

        // Walk through each row of cells and add a Map, where the key is the Enum itself and the value is Any type of
        // value derived from the Enum's conversion method, to a List. If too many values are null, and they are not
        // supposed to be, then this throws an exception
        val cells = mutableListOf<Map<Field, Any?>>()
        val row = mutableMapOf<Field, Any?>()
        var failures = 0
        val maxFailures = 3
        for (rowIndex in 1 until fileRows.size) {
            var skip = false
            for ((column, cellStringValue) in getCsvRow(fileRows[rowIndex]).withIndex()) {
                try {
                    val value = columnsOfEnums[column].valueType.stringToType(cellStringValue)
                    if (value == null && !columnsOfEnums[column].nullable)
                        throw IllegalArgumentException()
                    row[columnsOfEnums[column]] = value
                // If the conversion failed and the Enum's method throws an Exception, print the error message
                // and set the value for the Enum as null
                } catch (exception: IllegalArgumentException) {
                    failures++
                    prettyError("The value for ${columnsOfEnums[column]} is illegally null on " +
                            "row ${rowIndex + 1}!\n\nSkipping this row . . .")
                    if (failures > maxFailures)
                        throw Exception("Failed reading a sheet too many times!")
                    skip = true
                    break
                } catch (exception: Exception) {
                    failures++
                    prettyError("${exception.message}\n\nMethod of ${columnsOfEnums[column]} to convert the " +
                            "string value \"$cellStringValue\" to a ${columnsOfEnums[column].valueType} on row " +
                            "${rowIndex + 1} failed!\n\nSkipping this row . . .")
                    if (failures > maxFailures)
                        throw Exception("Failed reading a sheet too many times!")
                    skip = true
                    break
                }
            }

            if (!skip)
                cells.add(row.toMap())

        }

        return cells.toList()

    }

    // Get a List of trimmed string values from a comma-separated string line
    fun getCsvRow (stringRow: String): List<String> {
        val untrimmedRow = stringRow.split(',').toMutableList()
        for ((index, untrimmedCell) in untrimmedRow.withIndex())
            untrimmedRow[index] = untrimmedCell.trim()
        return untrimmedRow.toList()
    }

    inline fun <reified Field> createDataMapToValuesMap (data: List<Map<Field, Any?>>, keyField: Field):
            MutableMap<ULong, MutableList<Map<Field, Any?>>> where Field : Enum<Field>, Field: DataTableType {
        val tempMap = mutableMapOf<ULong, MutableList<Map<Field, Any?>>>()
        for (row in data) {
            val key = row[keyField] as ULong
            if (tempMap[key].isNullOrEmpty())
                tempMap[key] = mutableListOf(row.minus(keyField))
            else
                tempMap[key]?.add(row.minus(keyField))
        }
        return tempMap
    }

    private inline fun <reified Field> createDataMapToValue (data: List<Map<Field, Any?>>, keyField: Field,
                                                             valueField: Field):
            Map<ULong, Any?> where Field : Enum<Field>, Field: DataTableType {

        val tempMap = mutableMapOf<ULong, Any?>()
        for (row in data)
            tempMap[row[keyField] as ULong] = row[valueField]
        return tempMap.toMap()

    }

    fun getShiftCalculationData (shifts: List<Shift>, newBreaks: List<List<Break>?>,
                                 forPay: Boolean) : ShiftCalculationDataQuery {

        val startingDayOfWeek =
            readCsv<ScheduleSettingsData>().first()[ScheduleSettingsData.STARTING_DAY_OF_WEEK] as DayOfWeek
        val otherShifts = getEmployeeShiftsWithinPeriods(shifts, startingDayOfWeek, forPay)
        val allShiftsSorted = shifts.union(otherShifts).sortedBy { shift -> shift.startDateTime }

        val parallelDateTimesList = List (allShiftsSorted.size) { index ->
            val shiftInList = allShiftsSorted[index]
            if (forPay)
                if (shiftInList.payStartDateTime != null && shiftInList.payEndDateTime != null)
                    shiftInList.payStartDateTime.rangeTo(shiftInList.payEndDateTime)
                else
                    null
            else
                shiftInList.startDateTime.rangeTo(shiftInList.endDateTime)
        }

        val workLocationMaps = getWorkLocationMaps(allShiftsSorted, shifts)
        val parallelClientList = List (allShiftsSorted.size) { index ->
            workLocationMaps.toClientPk[allShiftsSorted[index].workLocationPk] as ULong
        }
        val parallelStateEnumList = List (allShiftsSorted.size) { index ->
            workLocationMaps.toStateEnum[allShiftsSorted[index].workLocationPk] as StateEnum
        }

        val shiftPkToBreaksMap = getShiftPkToBreaksMap(allShiftsSorted)
        for ((index, shift) in shifts.withIndex()) {
            if (newBreaks[index] != null)
                shiftPkToBreaksMap[shift.primaryKey as ULong] = newBreaks[index] as List<Break>
        }
        val parallelBreakList = List (allShiftsSorted.size) { index ->
            shiftPkToBreaksMap[allShiftsSorted[index].primaryKey]
        }

        val shiftIndices = List (shifts.size) { index ->
            allShiftsSorted.indexOfFirst{ shift-> shift.primaryKey == shifts[index].primaryKey }
        }

        return ShiftCalculationDataQuery (
            shiftsDateTimes = parallelDateTimesList,
            shiftsStateEnum = parallelStateEnumList,
            shiftsBreaks = parallelBreakList,
            shiftsClient = parallelClientList,
            statesOtDefinitions = getStateEnumToStateOtDefinitionsMap(parallelStateEnumList),
            clientsHolidays = getClientPkToHolidaysMap(parallelClientList),
            startingDayOfWeek = startingDayOfWeek,
            insertedAt = shiftIndices,
            shifts = allShiftsSorted
        )

    }

    data class ShiftCalculationDataQuery (val shiftsDateTimes: List<ClosedRange<DateTime>?>,
                                          val shiftsStateEnum: List<StateEnum>,
                                          val shiftsBreaks: List<List<Break>?>, val shiftsClient: List<ULong>,
                                          val statesOtDefinitions: Map<StateEnum, Map<StateOtDefinitionEnum, Float?>>,
                                          val clientsHolidays: Map<ULong, List<Date>>,
                                          val startingDayOfWeek: DayOfWeek, val insertedAt: List<Int>,
                                          val shifts: List<Shift>)

    private fun getEmployeeShiftsWithinPeriods (shifts: List<Shift>, startingDayOfWeek: DayOfWeek,
                                                forPay: Boolean): List<Shift> {

        val periods = mutableListOf<ClosedRange<DateTime>>()

        var inPeriod = false
        for (shift in shifts) {
            for (period in periods)
                if (shift.startDateTime in period.start .. period.endInclusive) {
                    inPeriod = true
                    break
                }
            if (!inPeriod) {
                val startOfWeek = DateTime(shift.startDateTime.date.getFirstDayOfWeek(startingDayOfWeek), Time(0u,0u,0u))
                val endOfWeek = DateTime(startOfWeek.date.offsetDays(6), Time(23u, 59u, 59u))
                periods.add(startOfWeek.rangeTo(endOfWeek))
            }
            inPeriod = false
        }

        val shiftPkQuickLookup = getQuickLookup(List (shifts.size) { index -> shifts[index].primaryKey })
        val employeePk = shifts.first().employeePk

        val shiftsDataList = readCsv<ShiftData>().filter { shiftMap ->

            shiftMap[ShiftData.EMPLOYEE_PK] as ULong? == employeePk
                &&
                !shiftPkQuickLookup.containsKey(shiftMap[ShiftData.SHIFT_PK] as ULong)
                &&
                periods.any { period ->
                    val startDateTime = DateTime (
                        shiftMap[ShiftData.START_DATE] as Date,
                        shiftMap[ShiftData.START_TIME] as Time
                    )
                    startDateTime in period.start .. period.endInclusive
                }
                &&
                if (forPay) {
                    shiftMap[ShiftData.PAY_START_DATE_TIME] != null
                            && shiftMap[ShiftData.PAY_END_DATE_TIME] != null
                            && !(shiftMap[ShiftData.NEEDS_RECONCILIATION] as Boolean? ?: true)
                } else {
                    true
                }

        }

        return List (shiftsDataList.size) { index ->
            val shiftMap = shiftsDataList[index]
            Shift (
                primaryKey = shiftMap[ShiftData.SHIFT_PK] as ULong,
                employeePk = shiftMap[ShiftData.EMPLOYEE_PK] as ULong,
                workLocationPk = shiftMap[ShiftData.WORK_LOCATION_PK] as ULong,
                startDateTime = DateTime (
                    shiftMap[ShiftData.START_DATE] as Date,
                    shiftMap[ShiftData.START_TIME] as Time
                ),
                endDateTime = DateTime (
                    shiftMap[ShiftData.END_DATE] as Date,
                    shiftMap[ShiftData.END_TIME] as Time
                ),
                payStartDateTime = shiftMap[ShiftData.PAY_START_DATE_TIME] as DateTime?,
                payEndDateTime = shiftMap[ShiftData.PAY_END_DATE_TIME] as DateTime?,
                needsReconciliation = shiftMap[ShiftData.NEEDS_RECONCILIATION] as Boolean? == true
            )
        }

    }

    private fun <keyClass> getQuickLookup (keyList: Collection<keyClass>) : MutableMap<keyClass, Boolean> {
        val quickLookup = mutableMapOf<keyClass, Boolean>()
        keyList.forEach { key ->
            quickLookup[key] = true
        }
        return quickLookup
    }

    private fun getShiftPkToBreaksMap (sortedEmployeeShifts: List<Shift>): MutableMap<ULong, List<Break>> {

        val shiftQuickLookup = getQuickLookup(sortedEmployeeShifts.map { it.primaryKey!! })

        return createDataMapToValuesMap(readCsv(), BreakData.SHIFT_PK).mapValues { clientPkToBreaks ->
            val breaks = clientPkToBreaks.value
            List (breaks.size) { breaksIndex ->
                Break (
                    breaks[breaksIndex][BreakData.BREAK_PK] as ULong,
                    clientPkToBreaks.key,
                    breaks[breaksIndex][BreakData.START_DATE_TIME] as DateTime,
                    breaks[breaksIndex][BreakData.DURATION_MINUTES] as UInt,
                    breaks[breaksIndex][BreakData.PREMIUM] as Boolean,
                    breaks[breaksIndex][BreakData.MEAL] as Boolean
                )
            }
        }.filter { entry ->
            shiftQuickLookup.containsKey(entry.key)
        }.toMutableMap()

    }

    private fun getWorkLocationMaps (shifts: List<Shift>, extraShifts: List<Shift>): WorkLocationMaps {

        val quickLookupMap = getQuickLookup(shifts.map{ shift -> shift.workLocationPk })
        for (shift in extraShifts)
            quickLookupMap[shift.workLocationPk] = true

        val stateEnums = StateEnum.values()
        val workLocationPkToClientPkMap = mutableMapOf<ULong, ULong>()
        val workLocationPkToStateEnumMap = mutableMapOf<ULong, StateEnum>()
        readCsv<WorkLocationData>().forEach { workLocationDataMap ->
            val workLocationPk = workLocationDataMap[WorkLocationData.WORK_LOCATION_PK] as ULong
            if (quickLookupMap.containsKey(workLocationPk)) {
                workLocationPkToClientPkMap[workLocationPk] = workLocationDataMap[WorkLocationData.CLIENT_PK] as ULong
                workLocationPkToStateEnumMap[workLocationPk] =
                    stateEnums[(workLocationDataMap[WorkLocationData.STATE_INDEX] as ULong).toInt() - 1]
            }
        }

        return WorkLocationMaps(workLocationPkToClientPkMap, workLocationPkToStateEnumMap)

    }

    data class WorkLocationMaps (val toClientPk: Map<ULong, ULong>, val toStateEnum: Map<ULong, StateEnum>)

    private fun getStateEnumToStateOtDefinitionsMap
                (stateEnums: List<StateEnum>) : Map<StateEnum, Map<StateOtDefinitionEnum, Float?>> {

        val stateEnumsLookup = StateEnum.values()

        val quickLookup = getQuickLookup(stateEnums)

        return createDataMapToValuesMap(readCsv(), StateOtDefinitionData.STATE_PK).mapKeys { statePkToDefs ->
            stateEnumsLookup[statePkToDefs.key.toInt() - 1]
        }.filter { stateEnumToDefs ->
            quickLookup.containsKey(stateEnumToDefs.key)
        }.mapValues { statePkToDefs ->

            val otDefinitions = statePkToDefs.value

            val weekDefs = otDefinitions.filter { otDef ->
                (otDef[StateOtDefinitionData.PERIOD] as String).lowercase().contains("we")
            }.sortedBy { otDef ->
                otDef[StateOtDefinitionData.MINIMUM_HOURS] as Float
            }.map {it[StateOtDefinitionData.MINIMUM_HOURS] as Float}

            val dailyDefs = otDefinitions.filter { otDef ->
                (otDef[StateOtDefinitionData.PERIOD] as String).lowercase().contains("da")
            }.sortedBy { otDef ->
                otDef[StateOtDefinitionData.MINIMUM_HOURS] as Float
            }.map {it[StateOtDefinitionData.MINIMUM_HOURS] as Float}

            mapOf (
                StateOtDefinitionEnum.WEEKLY_OT to weekDefs.getOrNull(0),
                StateOtDefinitionEnum.WEEKLY_DBLOT to weekDefs.getOrNull(1),
                StateOtDefinitionEnum.DAILY_OT to dailyDefs.getOrNull(0),
                StateOtDefinitionEnum.DAILY_DBLOT to dailyDefs.getOrNull(1)
            )

        }

    }

    private fun getClientPkToHolidaysMap (clientPks: List<ULong>) : Map<ULong, List<Date>> {

        var quickLookup = getQuickLookup(clientPks)

        val clientToContract = createDataMapToValuesMap(readCsv(), ContractData.CLIENT_PK).filter { clientToContracts ->
            quickLookup.containsKey(clientToContracts.key)
        }.mapValues { clientToContracts ->
            val latestContract = clientToContracts.value.maxBy { contractList ->
                contractList[ContractData.CREATED_AT] as DateTime
            }
            latestContract[ContractData.CONTRACT_PK] as ULong
        }

        quickLookup = getQuickLookup(clientToContract.values)

        val contractPkToHolidayPks = createDataMapToValuesMap (
            readCsv(),
            ContractHolidayData.CONTRACT_PK
        ).filter { contractToHolidays ->
            quickLookup.containsKey(contractToHolidays.key)
        }.mapValues { contractToHolidays ->
            List (contractToHolidays.value.size) { index ->
                contractToHolidays.value[index][ContractHolidayData.HOLIDAY_PK] as ULong
            }
        }

        quickLookup.clear()
        contractPkToHolidayPks.forEach { holidayPks ->
            holidayPks.value.forEach { holidayPk ->
                quickLookup[holidayPk] = true
            }
        }

        val holidayPkToDate = createDataMapToValue (
            readCsv(),
            HolidayData.HOLIDAY_PK, HolidayData.DATE
        ).filter { holidayPkToDate ->
            quickLookup.containsKey(holidayPkToDate.key)
        }

        return clientToContract.mapValues { clientPkToContractPk ->
            contractPkToHolidayPks[clientPkToContractPk.value]
        }.mapValues { clientPkToHolidayPks ->
            clientPkToHolidayPks.value?.let { holidayPks ->
                List (holidayPks.size) { index ->
                    holidayPkToDate[holidayPks[index]] as Date
                }
            } ?: listOf()
        }

    }

}