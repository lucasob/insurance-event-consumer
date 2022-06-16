package reports

import policies.PolicySummary
import java.time.Month

data class Report(val month: Month, val contracts: Long, val actualGrossPremium: Long, val expectedGrossPremium: Long)

/**
 * From a List of Policies spanning any arbitrary time range, return a report
 * for all policies across the entire time range
 */
fun List<PolicySummary>.toReport() =
    this
        .groupBy { it.at }
        .map { (month, policies) ->
            Report(
                month = month,
                contracts = policies.count { policy ->
                    policy.terminationDate()?.let { it.month.value == month.value } ?: true
                }.toLong(),
                actualGrossPremium = policies.sumOf { it.actualGrossWrittenPremiumToDate },
                expectedGrossPremium = policies.sumOf { it.expectedGrossWrittenPremium }
            )
        }
