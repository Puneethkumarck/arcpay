package com.arcpay.policy.policyengine.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(
        packages = "com.arcpay.policy.policyengine",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    private static final String BASE = "com.arcpay.policy.policyengine";

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
    static final ArchRule domain_should_not_depend_on_application = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".application..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_use_jpa_annotations = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
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

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repository_adapters_should_be_package_private = classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().notBePublic()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_should_reside_in_application = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage(BASE + ".application.controller..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jpa_entities_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().haveSimpleNameEndingWith("Entity")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repository_adapters_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().haveSimpleNameContaining("Repository")
            .and().areNotInterfaces()
            .and().haveSimpleNameNotEndingWith("JpaRepository")
            .and().areNotAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .and().haveSimpleNameNotEndingWith("Configuration")
            .should().haveSimpleNameEndingWith("RepositoryAdapter")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repository_ports_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".domain.port..")
            .and().areInterfaces()
            .and().haveSimpleNameContaining("Repository")
            .should().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entity_annotation_only_in_infrastructure_db = noClasses()
            .that().resideOutsideOfPackage(BASE + ".infrastructure.db..")
            .should().beAnnotatedWith(jakarta.persistence.Entity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule rest_controller_only_in_application_controller = noClasses()
            .that().resideOutsideOfPackage(BASE + ".application.controller..")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule kafka_listener_only_in_application_stream = noMethods()
            .that().areDeclaredInClassesThat().resideOutsideOfPackage(BASE + ".application.stream..")
            .should().beAnnotatedWith(org.springframework.kafka.annotation.KafkaListener.class)
            .allowEmptyShould(true);

    private static final ArchCondition<JavaClass> DECLARE_TOPIC_CONSTANT =
            new ArchCondition<>("declare a public static final String TOPIC field") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    boolean hasTopic = clazz.getFields().stream().anyMatch(HexagonalArchitectureTest::isPublicStaticFinalStringTopic);
                    events.add(new SimpleConditionEvent(clazz, hasTopic,
                            clazz.getName() + (hasTopic
                                    ? " declares a public static final String TOPIC"
                                    : " does not declare a public static final String TOPIC")));
                }
            };

    private static boolean isPublicStaticFinalStringTopic(JavaField field) {
        return "TOPIC".equals(field.getName())
                && field.getModifiers().contains(JavaModifier.PUBLIC)
                && field.getModifiers().contains(JavaModifier.STATIC)
                && field.getModifiers().contains(JavaModifier.FINAL)
                && field.getRawType().isEquivalentTo(String.class);
    }

    @ArchTest
    static final ArchRule events_should_declare_public_static_final_string_topic = classes()
            .that().resideInAPackage(BASE + ".domain.event..")
            .and().areRecords()
            .should(DECLARE_TOPIC_CONSTANT)
            .allowEmptyShould(true);
}
