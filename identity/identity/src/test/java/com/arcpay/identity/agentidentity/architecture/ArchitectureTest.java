package com.arcpay.identity.agentidentity.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(
        packages = "com.arcpay.identity.agentidentity",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String BASE = "com.arcpay.identity.agentidentity";

    // ── Layered architecture ──

    @ArchTest
    static final ArchRule layered_architecture = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("APPLICATION").definedBy(BASE + ".application..")
            .layer("DOMAIN").definedBy(BASE + ".domain..")
            .layer("INFRASTRUCTURE").definedBy(BASE + ".infrastructure..")
            .whereLayer("APPLICATION").mayOnlyAccessLayers("DOMAIN")
            .whereLayer("DOMAIN").mayNotAccessAnyLayer()
            .whereLayer("INFRASTRUCTURE").mayOnlyAccessLayers("DOMAIN");

    // ── Domain purity ──

    @ArchTest
    static final ArchRule domain_does_not_use_jpa = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domain_only_uses_allowed_spring_packages = classes()
            .that().resideInAPackage(BASE + ".domain..")
            .should().onlyDependOnClassesThat(
                    JavaClass.Predicates.resideOutsideOfPackage("org.springframework..")
                            .or(JavaClass.Predicates.resideInAnyPackage(
                                    "org.springframework.stereotype..",
                                    "org.springframework.transaction..",
                                    "org.springframework.data.domain.."
                            ))
            );

    // ── Infrastructure isolation ──

    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_application = noClasses()
            .that().resideInAPackage(BASE + ".infrastructure..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".application..");

    // ── No @Autowired ──

    @ArchTest
    static final ArchRule production_code_does_not_use_autowired = noClasses()
            .that().resideInAnyPackage(BASE + ".application..", BASE + ".domain..", BASE + ".infrastructure..")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired");

    // ── Naming conventions ──

    @ArchTest
    static final ArchRule controllers_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".application.controller..")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule jpa_entities_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().haveSimpleNameEndingWith("Entity");

    @ArchTest
    static final ArchRule repository_adapters_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().haveSimpleNameContaining("Repository")
            .and().areNotInterfaces()
            .and().haveSimpleNameNotEndingWith("JpaRepository")
            .and().areNotAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .and().haveSimpleNameNotEndingWith("Configuration")
            .should().haveSimpleNameEndingWith("RepositoryAdapter");

    // ── Package conventions ──

    @ArchTest
    static final ArchRule entity_annotation_only_in_infrastructure_db = noClasses()
            .that().resideOutsideOfPackage(BASE + ".infrastructure.db..")
            .should().beAnnotatedWith(jakarta.persistence.Entity.class);

    @ArchTest
    static final ArchRule rest_controller_only_in_application_controller = noClasses()
            .that().resideOutsideOfPackage(BASE + ".application.controller..")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

    @ArchTest
    static final ArchRule kafka_listener_only_in_application_stream = noMethods()
            .that().areDeclaredInClassesThat().resideOutsideOfPackage(BASE + ".application.stream..")
            .should().beAnnotatedWith(org.springframework.kafka.annotation.KafkaListener.class);
}
