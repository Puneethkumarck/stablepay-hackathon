package com.stablepay.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.stablepay", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_models_should_not_import_spring =
            noClasses().that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_should_not_import_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_application_controllers =
            noClasses().that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..application.controller..")
                    .allowEmptyShould(true);
}
