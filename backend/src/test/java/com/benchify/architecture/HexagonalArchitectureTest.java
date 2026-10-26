package com.benchify.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Reglas de dependencia de la arquitectura hexagonal por feature (ADR-0004).
 * Si una regla falla, el build falla.
 */
@AnalyzeClasses(packages = "com.benchify", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    static final List<String> FEATURES = List.of("banco", "resena", "usuario", "favorito", "checkin", "moderacion");

    private static final String[] FRAMEWORKS = {
            "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..", "com.fasterxml..",
            "com.google..", "org.mapstruct..", "io.swagger.."
    };

    @ArchTest
    static final ArchRule sharedDomainIsPlainJava = classes()
            .that().resideInAPackage("com.benchify.shared.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "com.benchify.shared.domain..")
            .because("el dominio compartido no puede depender de frameworks ni de features");

    @ArchTest
    static void eachFeatureDomainDependsOnlyOnItselfAndTheSharedDomain(JavaClasses classes) {
        for (String feature : FEATURES) {
            classes().that().resideInAPackage(base(feature) + ".domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..", "com.benchify.shared.domain..", base(feature) + ".domain..")
                    .because("el dominio de '" + feature + "' debe ser Java puro y no conocer otras features")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @ArchTest
    static final ArchRule applicationIgnoresAdaptersAndInfrastructureFrameworks = noClasses()
            .that().resideInAPackage("com.benchify.(*).application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "jakarta.persistence..", "org.springframework.web..",
                    "org.springframework.data..", "org.springframework.jdbc..", "org.hibernate..",
                    "com.google..", "tools.jackson..")
            .because("los casos de uso solo hablan con puertos; los adaptadores viven en infrastructure");

    @ArchTest
    static void noFeatureReachesIntoAnotherFeaturesInfrastructure(JavaClasses classes) {
        for (String feature : FEATURES) {
            noClasses().that().resideOutsideOfPackage(base(feature) + "..")
                    .should().dependOnClassesThat().resideInAPackage(base(feature) + ".infrastructure..")
                    .because("la infraestructura de '" + feature + "' es un detalle interno de esa feature")
                    .check(classes);
        }
    }

    @ArchTest
    static final ArchRule sharedKernelDoesNotDependOnFeatures = noClasses()
            .that().resideInAPackage("com.benchify.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    FEATURES.stream().map(f -> base(f) + "..").toArray(String[]::new));

    @ArchTest
    static final ArchRule featuresAreFreeOfCycles = slices()
            .matching("com.benchify.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllersOnlyUseInboundPorts = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..application.port.out..")
            .because("los controladores entran por los puertos de entrada (casos de uso)");

    @ArchTest
    static final ArchRule controllersLiveInTheWebAdapter = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..infrastructure.web..");

    @ArchTest
    static final ArchRule entitiesLiveInThePersistenceAdapter = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule servicesImplementUseCases = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("..application.service..")
            .andShould().implement(DescribedPredicate.describe("a use case interface",
                    (JavaClass c) -> c.getPackageName().endsWith(".application.port.in")));

    @ArchTest
    static final ArchRule domainDoesNotUseFrameworks = noClasses()
            .that().resideInAPackage("com.benchify.(*).domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS);

    private static String base(String feature) {
        return "com.benchify." + feature;
    }
}
