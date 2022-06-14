package events

import java.time.LocalDate

sealed class PremiumEvent {
    abstract val contractId: String
}

fun PremiumEvent.date() =
    when (this) {
        is ContractCreatedEvent -> LocalDate.parse(this.startDate)
        is PriceIncreasedEvent -> LocalDate.parse(this.atDate)
        is PriceDecreasedEvent -> LocalDate.parse(this.atDate)
        is ContractTerminatedEvent -> LocalDate.parse(this.terminationDate)
    }

data class ContractCreatedEvent(
    override val contractId: String,
    val premium: Long,
    val startDate: String
) : PremiumEvent()

data class PriceIncreasedEvent(
    override val contractId: String,
    val premiumIncrease: Long,
    val atDate: String
) : PremiumEvent()

data class PriceDecreasedEvent(
    override val contractId: String,
    val premiumReduction: Long,
    val atDate: String
) : PremiumEvent()

data class ContractTerminatedEvent(
    override val contractId: String,
    val terminationDate: String
) : PremiumEvent()
