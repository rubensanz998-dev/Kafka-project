package com.ruben.shop.payment;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Las reglas de la arquitectura hexagonal, comprobadas en cada build (no solo en un diagrama). */
@AnalyzeClasses(packages = "com.ruben.shop.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPureJava = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..infrastructure..", "org.springframework..", "jakarta..", "org.apache.kafka..");

    @ArchTest
    static final ArchRule applicationDoesNotKnowAdapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "org.apache.kafka..", "jakarta.persistence..");
}
