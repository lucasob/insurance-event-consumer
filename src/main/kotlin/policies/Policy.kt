package policies

import ContractId
import events.*
import java.time.Month

/**
 * Represents a Policy at a single point in time
 */
data class Policy(
    val at: Month,
    val contractId: ContractId,
    val currentPremium: Long,
    val actualGrossWrittenPremiumToDate: Long,
    val expectedGrossWrittenPremium: Long
)

/**
 * For a policy that exists, determine the representation at the
 * next period of time.
 *
 * It is required that all events are in the same time window.
 */
fun Policy.nextPeriod(event: PolicyEvent?) =
    when (event) {
        is Nothing -> this
        is PriceIncreasedEvent -> this
        is PriceDecreasedEvent -> this
        is ContractTerminatedEvent -> this
        else -> throw InvalidEventOrderException()
    }
