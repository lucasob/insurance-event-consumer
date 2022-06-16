package policies

import ContractId
import events.*
import java.time.LocalDate
import java.time.Month

/**
 * Represents the state of a Policy at a single point in time
 */
data class PolicySummary(
    val at: Month,
    val contractId: ContractId,
    val currentPremium: Long,
    val actualGrossWrittenPremiumToDate: Long,
    val expectedGrossWrittenPremium: Long,
    val terminatedOn: String? = null
) {

    /**
     * A LocalDate instance that the Policy was terminated on
     */
    fun terminationDate() = terminatedOn?.let { LocalDate.parse(it) }

}

/**
 * For a policy that exists, determine the representation at the
 * next period of time.
 */
fun PolicySummary.nextPeriod(event: PolicyEvent?) =
    when (event) {
        null -> this.same()
        else -> {
            if (event.date().month == this.at) throw MultipleEventsException()
            this.terminatedOn?.let { throw InvalidEventOrderException() }
            when (event) {
                is PriceIncreasedEvent -> this.increase(event)
                is PriceDecreasedEvent -> this.decrease(event)
                is ContractTerminatedEvent -> this.terminate(event)
                is ContractCreatedEvent -> throw InvalidEventOrderException()
            }
        }
    }

// Progress the policy's representation on by a month.
private fun PolicySummary.same() = this.copy(
    at = this.at.next(),
    actualGrossWrittenPremiumToDate = this.actualGrossWrittenPremiumToDate + this.currentPremium
)

// Increase the current, actual and expected premiums
private fun PolicySummary.increase(event: PriceIncreasedEvent): PolicySummary {
    val nextPremium = this.currentPremium + event.premiumIncrease
    return this.copy(
        at = this.at.next(),
        currentPremium = nextPremium,
        actualGrossWrittenPremiumToDate = (this.actualGrossWrittenPremiumToDate + nextPremium),
        expectedGrossWrittenPremium = calculateNewEGP(this.at, this.actualGrossWrittenPremiumToDate, nextPremium)
    )
}

// Increase the current, actual and expected premiums
private fun PolicySummary.decrease(event: PriceDecreasedEvent): PolicySummary {
    val nextPremium = this.currentPremium - event.premiumReduction
    return this.copy(
        at = this.at.next(),
        currentPremium = nextPremium,
        actualGrossWrittenPremiumToDate = (this.actualGrossWrittenPremiumToDate + nextPremium),
        expectedGrossWrittenPremium = calculateNewEGP(this.at, this.actualGrossWrittenPremiumToDate, nextPremium)
    )
}

private fun PolicySummary.terminate(event: ContractTerminatedEvent): PolicySummary {
    val nextPremium = (this.actualGrossWrittenPremiumToDate + this.currentPremium)
    return this.copy(
        at = at.next(),
        terminatedOn = event.terminationDate,
        currentPremium = 0L,
        actualGrossWrittenPremiumToDate = nextPremium,
        expectedGrossWrittenPremium = calculateNewEGP(this.at, nextPremium, 0L)
    )
}

/**
 * Creates a new Policy from the creation event
 */
fun ContractCreatedEvent.newPolicy() = PolicySummary(
    at = LocalDate.parse(this.startDate).month,
    contractId = this.contractId,
    currentPremium = this.premium,
    actualGrossWrittenPremiumToDate = this.premium,
    expectedGrossWrittenPremium = (this.premium * (13 - LocalDate.parse(this.startDate).month.value))
)

fun Month.next(): Month = if (this == Month.DECEMBER) Month.JANUARY else Month.of(this.value + 1)


/**
 * From the current month, determine the expected value for the remainder of the year
 */
private fun calculateNewEGP(currentMonth: Month, currentActual: Long, premiumValue: Long) =
    currentActual + (12 - currentMonth.value) * premiumValue


/**
 * Given a list of PolicyEvents, return a flat list of the monthly summaries,
 * from their first event, to the end of the calendar year.
 *
 * Policy summaries must be reported on to the end of the year, as once
 * a payment is made against a policy, then it has contributed to
 * the actual gross written premium.
 */
fun List<PolicyEvent>.summaries(): List<PolicySummary> {

    // Organise by contractId
    return this.groupBy { it.contractId }.entries.map { (_, events) ->
        val eventsInChronologicalOrder = events.sortedBy { it.date().month }

        val firstEvent = eventsInChronologicalOrder.first()
        if (firstEvent !is ContractCreatedEvent) {
            throw InvalidEventOrderException()
        }

        val newPolicy = firstEvent.newPolicy()
        if (newPolicy.at == Month.DECEMBER) {
            listOf(newPolicy)
        }

        val eventMonthLookup = eventsInChronologicalOrder.groupBy { it.date().month }
        ((newPolicy.at.next().value)..Month.DECEMBER.value)
            .fold(listOf(newPolicy)) { policies, monthNumber ->
                policies + policies.last().nextPeriod(eventMonthLookup[Month.of(monthNumber)]?.first())
            }
    }.flatten()
}
