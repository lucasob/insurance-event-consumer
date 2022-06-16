package reports

import events.ContractCreatedEvent
import events.ContractTerminatedEvent
import events.PriceDecreasedEvent
import events.PriceIncreasedEvent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import policies.PolicySummary
import policies.summaries
import java.time.Month

class TestReports {

    @Test
    fun `Test only active and terminated policies`() {

        val events = listOf(
            ContractCreatedEvent("1", 100L, "2020-01-01"),
            ContractCreatedEvent("2", 100L, "2020-02-01"),
            ContractTerminatedEvent("1", "2020-03-30"),
            ContractTerminatedEvent("2", "2020-04-30")
        )

        val reports = listOf(
            Report(month = Month.JANUARY, contracts = 1, actualGrossPremium = 100L, expectedGrossPremium = 1200L),
            Report(month = Month.FEBRUARY, contracts = 2, actualGrossPremium = 300, expectedGrossPremium = 2300L),
            Report(month = Month.MARCH, contracts = 2, actualGrossPremium = 500L, expectedGrossPremium = 1400L),
            Report(month = Month.APRIL, contracts = 1, actualGrossPremium = 600L, expectedGrossPremium = 600L),
            Report(month = Month.MAY, contracts = 0, actualGrossPremium = 600L, expectedGrossPremium = 600L)
        )

        events.summaries().toReport().take(5) shouldBe reports
    }

    @Test
    fun `A contract that has gone through a roller coaster should be reported on correctly`() {

        val events = listOf(
            ContractCreatedEvent("1", 100L, "2020-01-01"),
            PriceIncreasedEvent("1", 100L, "2020-02-01"),
            PriceDecreasedEvent("1", 100L, "2020-03-01"),
            ContractTerminatedEvent("1", "2020-04-30")
        )

        val reports = listOf(
            Report(Month.JANUARY, 1, 100L, 1200L),
            Report(Month.FEBRUARY, 1, 300L, 2300L),
            Report(Month.MARCH, 1, 400L, 1300L),
            Report(Month.APRIL, 1, 500L, 500L),
            Report(Month.MAY, 0, 500L, 500L),
        )

        events.summaries().toReport().take(5) shouldBe reports
    }
}