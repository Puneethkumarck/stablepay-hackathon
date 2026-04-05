package com.stablepay.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.stablepay", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final Set<String> ALLOWED_SPRING_IN_DOMAIN = Set.of(
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Component",
            "org.springframework.transaction.annotation.Transactional",
            "org.springframework.transaction.annotation.Isolation",
            "org.springframework.transaction.annotation.Propagation",
            "org.springframework.data.domain.Page",
            "org.springframework.data.domain.Pageable");

    // --- Hexagonal layer boundary rules ---

    @ArchTest
    static final ArchRule shouldNotAllowDomainToDependOnInfrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule shouldNotAllowDomainToDependOnApplication =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shouldNotAllowDomainToImportJakartaPersistence =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule shouldNotAllowDomainModelsToImportSpring =
            noClasses().that().resideInAPackage("..domain..model..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule shouldNotAllowApplicationToDependOnInfrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shouldNotAllowInfrastructureToDependOnApplicationControllers =
            noClasses().that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..application.controller..")
                    .allowEmptyShould(true);

    // --- Domain Spring usage restriction ---

    @ArchTest
    static final ArchRule shouldNotAllowDomainToImportSpringExceptServiceAndTransactional =
            noClasses().that().resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat(
                            DescribedPredicate.describe(
                                    "reside in Spring packages and are not @Service, @Component, or @Transactional",
                                    (JavaClass javaClass) ->
                                            javaClass.getPackageName().startsWith("org.springframework")
                                                    && !ALLOWED_SPRING_IN_DOMAIN.contains(
                                                            javaClass.getName())))
                    .allowEmptyShould(true);

    // --- Code quality rules ---

    @ArchTest
    static final ArchRule shouldNotUseAutowiredAnnotation =
            noFields()
                    .should()
                    .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .as("No field should use @Autowired — use @RequiredArgsConstructor with private final fields")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shouldNotUseSystemOut =
            noClasses()
                    .should()
                    .callMethod(java.io.PrintStream.class, "println", String.class)
                    .orShould()
                    .callMethod(java.io.PrintStream.class, "println", Object.class)
                    .orShould()
                    .callMethod(java.io.PrintStream.class, "print", String.class)
                    .orShould()
                    .callMethod(java.io.PrintStream.class, "print", Object.class)
                    .as("No class should use System.out or System.err — use @Slf4j instead")
                    .allowEmptyShould(true);
}
