package policies

import ContractId
import events.*
import java.time.Month

/**
 * Represents the state of a Policy at a single point in time
 */
data class Policy(
    val at: Month,
    val active: Boolean = true,
    val contractId: ContractId,
    val currentPremium: Long,
    val actualGrossWrittenPremiumToDate: Long,
    val expectedGrossWrittenPremium: Long
)

/**
 * For a policy that exists, determine the representation at the
 * next period of time.
 */
fun Policy.nextPeriod(event: PolicyEvent?) =
    when (event) {
        null -> this.same()
        else -> {
            if (event.date().month == this.at) throw MultipleEventsException()
            if (!this.active) throw InvalidEventOrderException()
            when (event) {
                is PriceIncreasedEvent -> this.increase(event)
                is PriceDecreasedEvent -> this.decrease(event)
                is ContractTerminatedEvent -> this.terminate()
                is ContractCreatedEvent -> throw InvalidEventOrderException()
            }
        }
    }

// Progress the policy's representation on by a month.
private fun Policy.same() = this.copy(at = this.at.next())

// Increase the current, actual and expected premiums
private fun Policy.increase(event: PriceIncreasedEvent): Policy {
    val nextPremium = this.currentPremium + event.premiumIncrease
    return this.copy(
        at = this.at.next(),
        currentPremium = nextPremium,
        actualGrossWrittenPremiumToDate = (this.actualGrossWrittenPremiumToDate + nextPremium),
        expectedGrossWrittenPremium = calculateNewEGP(this.at, this.actualGrossWrittenPremiumToDate, nextPremium)
    )
}

private fun Policy.decrease(event: PriceDecreasedEvent): Policy {
    val nextPremium = this.currentPremium - event.premiumReduction
    return this.copy(
        at = this.at.next(),
        currentPremium = nextPremium,
        actualGrossWrittenPremiumToDate = (this.actualGrossWrittenPremiumToDate + nextPremium),
        expectedGrossWrittenPremium = calculateNewEGP(this.at, this.actualGrossWrittenPremiumToDate, nextPremium)
    )
}

private fun Policy.terminate() = this.copy(
    at = this.at.next(),
    currentPremium = 0L,
    expectedGrossWrittenPremium = calculateNewEGP(this.at, this.actualGrossWrittenPremiumToDate, 0L)
)


fun Month.next(): Month = if (this == Month.DECEMBER) Month.JANUARY else Month.of(this.value + 1)


/**
 * From the current month, determine the expected value for the remainder of the year
 */
private fun calculateNewEGP(currentMonth: Month, currentActual: Long, premiumValue: Long) =
    currentActual + (12 - currentMonth.value) * premiumValue
