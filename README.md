# Insurance Event Consumer

## What

This application consumes a variety of events pertaining to premiums for insurance policies. It then aggregates these
into relevant reports.

Once a policy has been in effect, it will be reported on for the entire remainder of the _calendar_ year. Whilst this
isn't really how contracts work it's a reasonable simplification that allows for a more defined project.

Another simplification is the generalisation / assumption that you can't receive multiple events for your policy within
the same month.

## Invocation

There isn't really an executable here, rather just some test that provide the assertions around the calculation logic.
To run all tests, simply:

    ./gradlew test

## Environment

* OpenJDK 16
* JVM Version 13

