## 4.0-M1

* #93 Remove deprecated modules (javax flavor) and deprecated APIs

## 3.0-RC1

* #91 Support for javax.inject
* #92 Upgrade to Jersey 3.0.17

## 3.0-M6

* #87 Wiremock transient dependency management - jackson version mismatch
* #88 Runnable-jar-with-dependencies packaging fails on java 17 and 21
* #90 Upgrade Jersey to 2.45 and 3.0.16

## 3.0-M4

* #86 Upgrade to Wiremock 3.5.0

## 3.0-M3

* #84 (Jakarta migration) Deprecate JAX-RS 2 modules
* #85 Remove Jersey Jetty HTTP client

## 3.0.M2

* #65 Upgrade to Jersey 2.38 / 3.0.9
* #67 ParamConverter for LocalDate, LocalTime, LocalDateTime
* #68 ParamConverter for Year, YearMonth
* #69 Integrate Wiremock for testing apps using "jersey-client"
* #70 Per-tester wiremock recording mode
* #71 Stubbing support in WireMockTester
* #72 WireMockTester - explicit method to turn on verbose logging 
* #73 Replace "recording" WireMockTester with proxy/snapshot
* #74 WireMock: support http redirects to same origin
* #75 WireMock: avoid snapshots duplication
* #76 Reduce client logging noise
* #77 RequestTimer to log response headers at DEBUG level
* #78 Jakarta Client - set custom ConnectorProvider via the extender
* #79 Jakarta Client - support for Jetty HttpClient
* #80 JerseyClientInstrumentedModule - better names for healthchecks, consistent config
* #81 Upgrade Jersey 3 to 3.0.11
* #83 Client: propagate transaction ID to async execution threads

## 3.0.M1

* #32 Expose application REST resources
* #59 Upgrade Jersey 2 to 2.35
* #60 Provide Jersey 3 (-jakarta) modules
* #61 Prevent resource registration warning
* #63 Upgrade Jersey 2.x to 2.36

## 2.0.B1

* #56 Support JAX-RS Application
* #57 bootique-jersey-jackson - add extender method to register custom type serializers
* #58 Overlapping redundant JAXB and activation dependencies

## 2.0.M1

* Migrated from Guice to "bootique-di"
* #47 Set<Package> bound in JerseyModule must be qualified with annotation
* #48 Combine bootique-jersey with bootique-jersey-client in one project 
* #49 Upgrade to Jersey 2.30.1
* #50 Integrate 'jersey-bean-validation'
* #51 Resources with dynamically defined paths
* #52 Configurable JSON serialization
* #53 bootique-jersey-jackson: Support java.time serialization

## 1.0.RC1

* #33 Can't inject dependencies with generics to Jersey resources
* #34 StackOverflowError in Jersey 2.21
* #36 Cleaning up APIs deprecated since <= 0.25 
* #37 Update Jackson to 2.9.5
* #38 Upgrade Jersey to 2.25.1
* #39 Upgrade to JAX-RS 2.1 / Jersey 2.27
* #40 JDK9 Compatibility
* #41 Java 10 issue
* #45 Jersey still includes "javax.inject" dependency

## 0.25

* #35 Upgrade to bootique-modules-parent 0.8

## 0.22

* #30 Upgrade to Bootique 0.23, Jetty 0.21
* #31 Support for contributing JAX RS application properties

## 0.21

* #26 Remove API deprecated since 0.15
* #27 Upgrade to bootique 0.22 and bootique-jetty 0.20
* #29 bootique-jersey-jackson: a submodule to handle JSON serialization

## 0.20

* #25 Bootique 0.21 and annotated config help

## 0.19

* #24 Upgrade to Bootique 0.20

## 0.18

* #23 Move to io.bootique namespace.

## 0.17

* #22 Upgrade to Bootique 0.18 / Bootique Jetty 0.16

## 0.16

* #21 Upgrade to Bootique 0.17 

## 0.15:

* #17 Upgrade to bootique-jetty 0.14
* #18 Remove API's deprecated since 0.11
* #19 JerseyModule - switch to "contribute" methods for config instead of "builder".

## 0.14:

* #15 Upgrade to bq-jetty 0.13 and bq 0.15
* #16 Assign predictable name to Jersey servlet - "jersey"
 
## 0.13:

* #13 Upgrade to Bootique 0.14
* #14 Allow empty JerseyModule

## 0.12: 

* #10: Debug added resources
* #12: API for binding DynamicFeature's

## 0.11:

* #5 JerseyBinder: register feature instances
* #6 Move contribution API from JerseyBinder into static methods on JerseyModule
* #7 Upgrade Bootique to 0.12 and bootique-jetty - to 0.11
* #8 Rename "servletPath" property of JerseyServletFactory to "urlPattern"
* #9 Builder-style API for configuring JerseyModule

## 0.10:

* #2 Upgrade to bootique-jetty:0.9
* #4 Support for Jersey servlet path property 

## 0.9:

* #1 Support for Feature injection from downstream modules
