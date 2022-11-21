import data.DataTable
import data.SheetReader
import data.clients.work_locations.WorkLocationData
import data.clients.work_locations.shifts.ShiftCalculationResults
import data.states.StateEnum
import data.states.StateOtDefinitionEnum

object Messages {

    fun printShiftCalculationResults (results: ShiftCalculationResults) {

        try {

            // Print all the state definitions
            println("--------- STATE DEFINITIONS ---------\n")
            for (stateEnum in results.dataQuery.statesOtDefinitions.keys) {
                val weeklyOt = results.dataQuery.statesOtDefinitions[stateEnum]?.get(StateOtDefinitionEnum.WEEKLY_OT)
                val weeklyDblot = results.dataQuery.statesOtDefinitions[stateEnum]?.get(StateOtDefinitionEnum.WEEKLY_DBLOT)
                val dailyOt = results.dataQuery.statesOtDefinitions[stateEnum]?.get(StateOtDefinitionEnum.DAILY_OT)
                val dailyDblot = results.dataQuery.statesOtDefinitions[stateEnum]?.get(StateOtDefinitionEnum.DAILY_DBLOT)
                println("$stateEnum:")
                println("\tWeekly overtime: $weeklyOt")
                println("\tWeekly double time: $weeklyDblot")
                println("\tDaily overtime: $dailyOt")
                println("\tDaily double time: $dailyDblot\n")
            }

            // Print all the clients, along with their work locations (and the state of the work location) and
            // holidays on the contract
            println("-------------- CLIENTS --------------\n")
            val clientPkToWorkLocationPks =
                SheetReader.createDataMapToValuesMap(SheetReader.readCsv(), WorkLocationData.CLIENT_PK)
            val stateEnums = StateEnum.values()
            for (clientId in results.dataQuery.shiftsClient.toSet()) {
                println("Client ${clientId}:\n")
                for (workLocationMap in clientPkToWorkLocationPks[clientId]!!) {
                    val workLocationId = workLocationMap[WorkLocationData.WORK_LOCATION_PK] as ULong
                    println("\tWork Location $workLocationId:")
                    val workLocationState = stateEnums[(workLocationMap[WorkLocationData.STATE_INDEX] as ULong - 1u).toInt()]
                    println("\t\t$workLocationState\n")
                }

                println("\tContract holidays:")
                if (!results.dataQuery.clientsHolidays[clientId].isNullOrEmpty()) {
                    for (clientHoliday in results.dataQuery.clientsHolidays[clientId]!!)
                        println("\t\t$clientHoliday")
                    println()
                } else
                    println("\t\tNone\n")

            }

            // Now print the processed shifts for the employee
            println("-------------- SHIFTS ---------------\n")
            for ((index, shift) in results.shifts.shifts.withIndex()) {
                val shiftId = shift.primaryKey
                val payStartDateTime = shift.payStartDateTime
                val payEndDateTime = shift.payEndDateTime
                val startDateTime = shift.startDateTime
                val endDateTime = shift.endDateTime
                println("\tShift $shiftId:")
                println("\t\tWork location: ${shift.workLocationPk}")
                val breaks = results.dataQuery.shiftsBreaks[index]
                println("\t\tBreaks:")
                if (breaks != null) {
                    for (brk in breaks) {
                        println("\t\t\tBreak ${brk.primaryKey}:")
                        println("\t\t\t\tStart datetime: ${brk.startDateTime}")
                        println("\t\t\t\tDuration (minutes): ${brk.durationMinutes}")
                        println("\t\t\t\tPremium? : ${brk.isPremium}")
                        println("\t\t\t\tMeal? : ${brk.isMeal}")
                    }
                } else {
                    println("\t\t\tNone")
                }
                println("\t\tNeeds reconciliation: ${shift.needsReconciliation}")
                if (results.forPay) {
                    println("\t\tPay:")
                    println("\t\t\tStart datetime: $payStartDateTime")
                    println("\t\t\tEnd datetime: $payEndDateTime")
                } else {
                    println("\t\tBilling:")
                    println("\t\t\tStart datetime: $startDateTime")
                    println("\t\t\tEnd datetime: $endDateTime")
                }
                println("\t\t\tWorked hours: ${results.shifts.shiftHours[index]?.workedHours}")
                println("\t\t\tNormal hours: ${results.shifts.shiftHours[index]?.normalHours}")
                println("\t\t\tOT hours: ${results.shifts.shiftHours[index]?.otHours}")
                println("\t\t\tDBLOT hours: ${results.shifts.shiftHours[index]?.dblotHours}")
                println("\t\t\tHoliday hours: ${results.shifts.shiftHours[index]?.holidayHours}")
                println()
            }

        } catch (exception: Exception) {
            exception.printStackTrace()
            val exceptionMessage = if (exception.message == null) "" else "${exception.message}\n\n"
            prettyError("${exceptionMessage}Failed to process all shifts!\n\nTerminating process . . .")
        }

    }

    fun prettyError (body: String) {
        println("-=-=-=-=-=-=:{  ERROR  }:-=-=-=-=-=-=-\n")
        println(body)
        println("\n-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
    }

    fun failedToFindSheet (className: String?, filepath: String) =
        "Missing CSV file for the data class \"$className\"!\n\n" +
                "The CSV file needs to be the following filepath and filename:\n" +
                filepath

    // Generic error message for the enum class
    fun failedConversionMessage (value: String, expectedClass: String) =
        prettyError("Failed to convert the string \"$value\" to a $expectedClass!")

    fun wrongHeaders (errorMessage: String, enums: Array<out DataTable>) =
        prettyError (
            "$errorMessage\n" +
                    "\nThe correct headers are:\n" +
                    "\n${enums.joinToString()}\n" +
                    "\n------ OR ------\n" +
                    "\n${enums.joinToString { it.fieldName }}\n" +
                    "\nReturning an empty list . . ."
        )

}

