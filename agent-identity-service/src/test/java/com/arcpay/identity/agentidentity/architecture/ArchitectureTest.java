package com.arcpay.identity.agentidentity.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.arcpay.identity.agentidentity",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String BASE = "com.arcpay.identity.agentidentity";

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

    @ArchTest
    static final ArchRule domain_does_not_use_jpa = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_only_uses_allowed_spring_packages = classes()
            .that().resideInAPackage(BASE + ".domain..")
            .should().onlyDependOnClassesThat(
                    JavaClass.Predicates.resideOutsideOfPackage("org.springframework..")
                            .or(JavaClass.Predicates.resideInAnyPackage(
                                    "org.springframework.stereotype..",
                                    "org.springframework.transaction.."
                            ))
            )
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_application = noClasses()
            .that().resideInAPackage(BASE + ".infrastructure..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".application..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule production_code_does_not_use_autowired = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired")
            .allowEmptyShould(true);
}
