package events

import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestPolicyEvent {

    @Test
    fun `will parse the date correctly`() {
        table(
            headers("PremiumEvent", "Date"),
            row(ContractCreatedEvent(contractId = "1", premium = 100L, startDate = "2020-01-01"), LocalDate.of(2020, 1, 1)),
            row(PriceIncreasedEvent(contractId = "1", premiumIncrease = 100L, atDate = "2020-01-01"), LocalDate.of(2020, 1, 1)),
            row(PriceDecreasedEvent(contractId = "1", premiumReduction = 100L, atDate = "2020-01-01"), LocalDate.of(2020, 1, 1)),
            row(ContractTerminatedEvent(contractId = "1", terminationDate = "2020-01-01"), LocalDate.of(2020, 1, 1))
        ).forAll { event, expectedDate ->
            event.date() shouldBe expectedDate
        }
    }
}
