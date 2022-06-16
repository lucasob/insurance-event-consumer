package policies

import events.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.*

class TestPolicySummaries {

    // Represents a policy that has been created
    private fun simplePolicy() = PolicySummary(
        at = Month.JANUARY,
        contractId = UUID.randomUUID().toString(),
        currentPremium = 100L,
        actualGrossWrittenPremiumToDate = 100L,
        expectedGrossWrittenPremium = 1200L
    )


    @Test
    fun `a policy with no updates will only progress to the next month`() {
        table(
            headers("Month", "Next Month"),
            row(Month.JANUARY, Month.FEBRUARY),
            row(Month.DECEMBER, Month.JANUARY)
        ).forAll { currentMonth, nextMonth ->
            PolicySummary(
                at = currentMonth,
                contractId = UUID.randomUUID().toString(),
                currentPremium = 0L,
                actualGrossWrittenPremiumToDate = 0L,
                expectedGrossWrittenPremium = 0L
            ).nextPeriod(null).at shouldBe nextMonth
        }
    }

    @Test
    fun `a cancelled policy cannot receive a new event`() {
        val initialPolicy = PolicySummary(
            Month.JANUARY,
            UUID.randomUUID().toString(),
            0L,
            actualGrossWrittenPremiumToDate = 0L,
            expectedGrossWrittenPremium = 0L,
            terminatedOn = "2020-01-01"
        )

        table(
            headers("Event"),
            row(
                ContractCreatedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, Month.FEBRUARY, 1).toString()
                )
            ),
            row(
                PriceIncreasedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, Month.FEBRUARY, 1).toString()
                )
            ),
            row(
                PriceDecreasedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, Month.FEBRUARY, 1).toString()
                )
            ),
            row(
                ContractTerminatedEvent(
                    initialPolicy.contractId,
                    LocalDate.of(2022, Month.FEBRUARY, 1).toString()
                )
            )
        ).forAll { event -> shouldThrow<InvalidEventOrderException> { initialPolicy.nextPeriod(event) } }
    }

    @Test
    fun `a policy cannot receive an event for the same month regardless of the event kind`() {
        val initialPolicy = simplePolicy()
        table(
            headers("Event"),
            row(
                ContractCreatedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, initialPolicy.at.value, 1).toString()
                )
            ),
            row(
                PriceIncreasedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, initialPolicy.at.value, 1).toString()
                )
            ),
            row(
                PriceDecreasedEvent(
                    initialPolicy.contractId,
                    100L,
                    LocalDate.of(2022, initialPolicy.at.value, 1).toString()
                )
            ),
            row(
                ContractTerminatedEvent(
                    initialPolicy.contractId,
                    LocalDate.of(2022, initialPolicy.at.value, 1).toString()
                )
            )
        ).forAll { event -> shouldThrow<InvalidEventException> { initialPolicy.nextPeriod(event) } }
    }

    @Test
    fun `a policy receiving a ContractCreated event should raise an InvalidEventOrder exception`() {
        val initialPolicy = simplePolicy()

        val creationEvent = ContractCreatedEvent(
            contractId = initialPolicy.contractId,
            premium = 100L,
            startDate = LocalDate.now().toString()
        )

        shouldThrow<InvalidEventOrderException> { initialPolicy.nextPeriod(creationEvent) }
    }

    @Test
    fun `a policy whose price increases must reflect the change accordingly`() {
        val initialPolicy = simplePolicy()

        val increaseEvent = PriceIncreasedEvent(
            contractId = initialPolicy.contractId,
            atDate = LocalDate.of(2022, Month.FEBRUARY, 1).toString(),
            premiumIncrease = 100L
        )

        val expectedPolicy = PolicySummary(
            at = Month.FEBRUARY,
            contractId = initialPolicy.contractId,
            currentPremium = 200L,
            actualGrossWrittenPremiumToDate = 300L,
            expectedGrossWrittenPremium = 2300L
        )

        initialPolicy.nextPeriod(increaseEvent) shouldBe expectedPolicy
    }

    @Test
    fun `a policy whose price decreases must reflect the change accordingly`() {
        val initialPolicy = simplePolicy()

        val decreaseEvent = PriceDecreasedEvent(
            contractId = initialPolicy.contractId,
            atDate = LocalDate.of(2022, Month.FEBRUARY, 1).toString(),
            premiumReduction = 50L
        )

        val expectedPolicySummary = PolicySummary(
            at = Month.FEBRUARY,
            contractId = initialPolicy.contractId,
            currentPremium = 50L,
            actualGrossWrittenPremiumToDate = 150L,
            expectedGrossWrittenPremium = 650L
        )

        initialPolicy.nextPeriod(decreaseEvent) shouldBe expectedPolicySummary
    }

    @Test
    fun `a policy that is cancelled pays the final month and marks as terminated`() {
        val initialPolicy = simplePolicy()

        val terminatedEvent = ContractTerminatedEvent(
            contractId = initialPolicy.contractId,
            terminationDate = LocalDate.of(2022, Month.FEBRUARY, 1).toString()
        )

        val expectedPolicySummary = PolicySummary(
            at = Month.FEBRUARY,
            contractId = initialPolicy.contractId,
            terminatedOn = terminatedEvent.terminationDate,
            actualGrossWrittenPremiumToDate = 200L,
            expectedGrossWrittenPremium = 200L,
            currentPremium = 0L
        )

        initialPolicy.nextPeriod(terminatedEvent) shouldBe expectedPolicySummary
    }

    @Test
    fun `a policy created in january should generate a series of summaries for the rest of the year`() {
        val newContractCreatedEvent = ContractCreatedEvent("1", 100L, "2020-01-01")

        val expectedPolicies = listOf(
            PolicySummary(Month.JANUARY, "1", 100L, 100L, 1200L),
            PolicySummary(Month.FEBRUARY, "1", 100L, 200L, 1200L),
            PolicySummary(Month.MARCH, "1", 100L, 300L, 1200L),
            PolicySummary(Month.APRIL, "1", 100L, 400L, 1200L),
            PolicySummary(Month.MAY, "1", 100L, 500L, 1200L),
            PolicySummary(Month.JUNE, "1", 100L, 600L, 1200L),
            PolicySummary(Month.JULY, "1", 100L, 700L, 1200L),
            PolicySummary(Month.AUGUST, "1", 100L, 800L, 1200L),
            PolicySummary(Month.SEPTEMBER, "1", 100L, 900L, 1200L),
            PolicySummary(Month.OCTOBER, "1", 100L, 1000L, 1200L),
            PolicySummary(Month.NOVEMBER, "1", 100L, 1100L, 1200L),
            PolicySummary(Month.DECEMBER, "1", 100L, 1200L, 1200L)
        )

        listOf(newContractCreatedEvent).summaries() shouldBe expectedPolicies
    }
}
