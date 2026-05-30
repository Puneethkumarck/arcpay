package com.arcpay.settlement.architecture;

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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@AnalyzeClasses(
        packages = "com.arcpay.settlement",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    private static final String BASE = "com.arcpay.settlement";

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
            .should().dependOnClassesThat().resideInAPackage(BASE + ".application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_use_jpa_annotations = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule domain_should_not_use_spring_web = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.web..");

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

    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_application = noClasses()
            .that().resideInAPackage(BASE + ".infrastructure..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".application..");

    @ArchTest
    static final ArchRule production_code_does_not_use_autowired = noClasses()
            .that().resideInAnyPackage(BASE + ".application..", BASE + ".domain..", BASE + ".infrastructure..")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired");

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .that().areDeclaredInClassesThat().resideInAnyPackage(BASE + ".application..", BASE + ".domain..", BASE + ".infrastructure..")
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

    @ArchTest
    static final ArchRule domain_port_adapters_should_be_package_private = classes()
            .that().areNotInterfaces()
            .and().implement(JavaClass.Predicates.resideInAPackage(BASE + ".domain.port.."))
            .should().notBePublic();

    @ArchTest
    static final ArchRule repository_adapters_should_be_package_private = classes()
            .that().haveSimpleNameEndingWith("Adapter")
            .and().resideInAPackage(BASE + ".infrastructure..")
            .should().notBePublic();

    @ArchTest
    static final ArchRule adapters_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().haveSimpleNameContaining("Repository")
            .and().areNotInterfaces()
            .and().haveSimpleNameNotEndingWith("JpaRepository")
            .and().areNotAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .and().haveSimpleNameNotEndingWith("Configuration")
            .should().haveSimpleNameEndingWith("Adapter");

    @ArchTest
    static final ArchRule repository_ports_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".domain.port..")
            .and().areInterfaces()
            .and().haveSimpleNameContaining("Repository")
            .should().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule gateway_ports_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".domain.port..")
            .and().areInterfaces()
            .and().haveSimpleNameContaining("Gateway")
            .should().haveSimpleNameEndingWith("Gateway");

    @ArchTest
    static final ArchRule controllers_should_be_rest_controllers_and_validated = classes()
            .that().resideInAPackage(BASE + ".application..")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .andShould().beAnnotatedWith(org.springframework.validation.annotation.Validated.class);

    @ArchTest
    static final ArchRule rest_controllers_reside_in_application = noClasses()
            .that().resideOutsideOfPackage(BASE + ".application..")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

    @ArchTest
    static final ArchRule entity_annotation_only_in_infrastructure_db = noClasses()
            .that().resideOutsideOfPackage(BASE + ".infrastructure.db..")
            .should().beAnnotatedWith(jakarta.persistence.Entity.class);

    @ArchTest
    static final ArchRule web3j_confined_to_infrastructure = noClasses()
            .that().resideInAnyPackage(BASE + ".application..", BASE + ".domain..")
            .should().dependOnClassesThat().resideInAPackage("org.web3j..");

    @ArchTest
    static final ArchRule circle_rest_client_confined_to_infrastructure = noClasses()
            .that().resideInAnyPackage(BASE + ".application..", BASE + ".domain..")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.web.client.RestClient");

    @ArchTest
    static final ArchRule settlement_carries_no_policy_business_logic = noClasses()
            .that().resideInAnyPackage(BASE + ".application..", BASE + ".domain..", BASE + ".infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.arcpay.policy..",
                    "com.arcpay.compliance..");

    @ArchTest
    static final ArchRule domain_should_not_define_state_machine_types = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().haveSimpleNameContaining("StateMachine");

    private static final ArchCondition<JavaClass> DECLARE_TOPIC_CONSTANT =
            new ArchCondition<>("declare a public static final String TOPIC field") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    var hasTopic = clazz.getFields().stream().anyMatch(HexagonalArchitectureTest::isPublicStaticFinalStringTopic);
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
            .should(DECLARE_TOPIC_CONSTANT);

    @ArchTest
    static final ArchRule event_topic_fields_should_be_public_static_final_string = fields()
            .that().haveName("TOPIC")
            .and().areDeclaredInClassesThat().resideInAPackage(BASE + ".domain.event..")
            .should().bePublic()
            .andShould().beStatic()
            .andShould().beFinal()
            .andShould().haveRawType(String.class);
}
