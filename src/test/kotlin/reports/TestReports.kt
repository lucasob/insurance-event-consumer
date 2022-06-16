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

    @Test
    fun `a policy created in january should generate a report for the entire year`() {
        val newContractCreatedEvent = ContractCreatedEvent("1", 100L, "2020-01-01")

        val expectedReports = listOf(
            Report(Month.JANUARY, contracts = 1L, actualGrossPremium = 100L, expectedGrossPremium = 1200L),
            Report(Month.FEBRUARY, contracts = 1L, actualGrossPremium = 200L, expectedGrossPremium = 1200L),
            Report(Month.MARCH, contracts = 1L, actualGrossPremium = 300L, expectedGrossPremium = 1200L),
            Report(Month.APRIL, contracts = 1L, actualGrossPremium = 400L, expectedGrossPremium = 1200L),
            Report(Month.MAY, contracts = 1L, actualGrossPremium = 500L, expectedGrossPremium = 1200L),
            Report(Month.JUNE, contracts = 1L, actualGrossPremium = 600L, expectedGrossPremium = 1200L),
            Report(Month.JULY, contracts = 1L, actualGrossPremium = 700L, expectedGrossPremium = 1200L),
            Report(Month.AUGUST, contracts = 1L, actualGrossPremium = 800L, expectedGrossPremium = 1200L),
            Report(Month.SEPTEMBER, contracts = 1L, actualGrossPremium = 900L, expectedGrossPremium = 1200L),
            Report(Month.OCTOBER, contracts = 1L, actualGrossPremium = 1000L, expectedGrossPremium = 1200L),
            Report(Month.NOVEMBER, contracts = 1L, actualGrossPremium = 1100L, expectedGrossPremium = 1200L),
            Report(Month.DECEMBER, contracts = 1L, actualGrossPremium = 1200L, expectedGrossPremium = 1200L),
        )

        listOf(newContractCreatedEvent).summaries().toReport() shouldBe expectedReports
    }

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