package com.arcpay.policy.policyengine.architecture;

import com.arcpay.policy.policyengine.api.PolicyRule;
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
import jakarta.persistence.Entity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.List;
import java.util.Set;

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
    static final ArchRule repository_adapters_should_be_package_private = classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().notBePublic();

    @ArchTest
    static final ArchRule repository_adapters_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".infrastructure.db..")
            .and().haveSimpleNameContaining("Repository")
            .and().areNotInterfaces()
            .and().haveSimpleNameNotEndingWith("JpaRepository")
            .and().areNotAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .and().haveSimpleNameNotEndingWith("Configuration")
            .should().haveSimpleNameEndingWith("RepositoryAdapter");

    @ArchTest
    static final ArchRule repository_ports_should_be_suffixed = classes()
            .that().resideInAPackage(BASE + ".domain.port..")
            .and().areInterfaces()
            .and().haveSimpleNameContaining("Repository")
            .should().haveSimpleNameEndingWith("Repository");

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
            .should(DECLARE_TOPIC_CONSTANT);

    @Test
    void policyRuleSealedInterfaceShouldHaveExactlyTenImplementations() {
        Class<?>[] permitted = PolicyRule.class.getPermittedSubclasses();

        Assertions.assertThat(permitted)
                .as("PolicyRule sealed interface must declare exactly 10 implementations")
                .hasSize(10);
    }

    @Test
    void jpaEntitiesShouldBeSuffixedWithEntity() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<BeanDefinition> entities = scanner.findCandidateComponents(BASE + ".infrastructure.db");
        List<String> names = entities.stream().map(BeanDefinition::getBeanClassName).toList();

        Assertions.assertThat(names)
                .as("JPA entities in infrastructure.db must exist and be suffixed with 'Entity'")
                .isNotEmpty()
                .allSatisfy(name -> Assertions.assertThat(name).endsWith("Entity"));
    }
}
