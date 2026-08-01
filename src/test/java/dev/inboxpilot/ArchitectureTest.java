package dev.inboxpilot;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the hexagonal architecture described in
 * {@code doc/adr/0001-hexagonal-architecture.md}.
 *
 * <p>These rules are the reason the ADR is more than a document: a boundary
 * violation fails the build instead of surviving review. Covers the acceptance
 * criteria of issue #3 — dependencies flow inward, and Gmail-specific types do
 * not leak into domain services.
 */
@DisplayName("Architecture")
class ArchitectureTest {

    private static final String ROOT_PACKAGE = "dev.inboxpilot";

    private static final String DOMAIN_LAYER = "Domain";
    private static final String APPLICATION_LAYER = "Application";
    private static final String INFRASTRUCTURE_LAYER = "Infrastructure";
    private static final String CLI_LAYER = "Cli";

    private static final String DOMAIN_PACKAGES = ROOT_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGES = ROOT_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGES = ROOT_PACKAGE + ".infrastructure..";
    private static final String CLI_PACKAGES = ROOT_PACKAGE + ".cli..";

    private static final String GOOGLE_API_PACKAGES = "com.google..";
    private static final String SPRING_PACKAGES = "org.springframework..";
    private static final String JACKSON_PACKAGES = "com.fasterxml.jackson..";
    private static final String SLF4J_PACKAGES = "org.slf4j..";

    private static final String LAYER_SLICES = ROOT_PACKAGE + ".(*)..";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    @DisplayName("dependencies flow inward through the layers")
    void dependenciesFlowInward() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer(DOMAIN_LAYER).definedBy(DOMAIN_PACKAGES)
                .layer(APPLICATION_LAYER).definedBy(APPLICATION_PACKAGES)
                .layer(INFRASTRUCTURE_LAYER).definedBy(INFRASTRUCTURE_PACKAGES)
                .layer(CLI_LAYER).definedBy(CLI_PACKAGES)

                // The outer layers are entered only by the composition root,
                // which sits in the root package and is therefore in no layer.
                .whereLayer(CLI_LAYER).mayNotBeAccessedByAnyLayer()
                .whereLayer(INFRASTRUCTURE_LAYER).mayNotBeAccessedByAnyLayer()
                // Ports live in application; adapters and the CLI implement or
                // call them. Nothing else may reach in.
                .whereLayer(APPLICATION_LAYER)
                .mayOnlyBeAccessedByLayers(INFRASTRUCTURE_LAYER, CLI_LAYER)
                // The centre: reachable from every outer layer, dependent on none.
                .whereLayer(DOMAIN_LAYER)
                .mayOnlyBeAccessedByLayers(APPLICATION_LAYER, INFRASTRUCTURE_LAYER, CLI_LAYER)

                .check(productionClasses);
    }

    @Test
    @DisplayName("has no cycles between layer packages")
    void hasNoCyclesBetweenLayers() {
        SlicesRuleDefinition.slices()
                .matching(LAYER_SLICES)
                .should().beFreeOfCycles()
                .check(productionClasses);
    }

    /**
     * The domain is plain Java. Each rule below names one framework that must
     * not reach it, so a failure says exactly which dependency leaked.
     */
    @Nested
    @DisplayName("domain layer")
    class DomainLayer {

        @Test
        @DisplayName("does not depend on Gmail or any Google API type")
        void doesNotDependOnGoogleApi() {
            noClasses().that().resideInAPackage(DOMAIN_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(GOOGLE_API_PACKAGES)
                    .because("Gmail is an adapter detail; domain rules must be testable "
                            + "without a mailbox (ADR 0001)")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("does not depend on Spring")
        void doesNotDependOnSpring() {
            noClasses().that().resideInAPackage(DOMAIN_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(SPRING_PACKAGES)
                    .because("the domain must be testable without a Spring context (ADR 0001)")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("does not depend on Jackson")
        void doesNotDependOnJackson() {
            noClasses().that().resideInAPackage(DOMAIN_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(JACKSON_PACKAGES)
                    .because("serialisation is an adapter concern, not a domain one (ADR 0001)")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("does not log")
        void doesNotLog() {
            noClasses().that().resideInAPackage(DOMAIN_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(SLF4J_PACKAGES)
                    .because("domain exceptions are logged where they are caught, "
                            + "in application or infrastructure (ADR 0001)")
                    .check(productionClasses);
        }
    }

    /**
     * Gmail types are confined to infrastructure. This is AC3 of issue #3 —
     * checked mechanically rather than by review.
     */
    @Nested
    @DisplayName("Gmail types")
    class GmailTypes {

        @Test
        @DisplayName("do not leak outside the infrastructure layer")
        void doNotLeakOutsideInfrastructure() {
            noClasses().that().resideOutsideOfPackage(INFRASTRUCTURE_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage(GOOGLE_API_PACKAGES)
                    .because("only the Gmail adapter may know about Google client types; "
                            + "everything else speaks in domain terms (ADR 0001)")
                    .check(productionClasses);
        }
    }
}
