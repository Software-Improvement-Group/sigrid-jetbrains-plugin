package com.softwareimprovementgroup.plugins.sigrid.mappers

import com.softwareimprovementgroup.plugins.sigrid.models.*
import com.softwareimprovementgroup.plugins.sigrid.models.DependencyType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenSourceHealthMapperTest {

    private fun makeComponent(
        name: String = "lib",
        group: String = "",
        properties: List<Property> = emptyList(),
        occurrences: List<String?>? = listOf("svc/pom.xml"),
        externalReferences: List<OshExternalReference>? = null,
        bomRef: String? = null,
    ) = OshDependencyResponse(
        type = "library",
        name = name,
        group = group,
        version = "1.0",
        purl = "pkg:maven/$name@1.0",
        properties = properties,
        licenses = emptyList(),
        evidence = occurrences?.let { locs -> OshEvidenceResponse(locs.map { OshOccurrence(it) }) },
        externalReferences = externalReferences,
        bomRef = bomRef,
    )

    private fun makeResponse(
        components: List<OshDependencyResponse>,
        vulnerabilities: List<OshVulnerabilityResponse> = emptyList(),
    ) = OpenSourceHealthResponse(
        bomFormat = "CycloneDX",
        specVersion = "1.4",
        version = 1,
        metadata = OshMetadataResponse(timestamp = "", properties = emptyList()),
        components = components,
        vulnerabilities = vulnerabilities,
    )

    private fun makeVulnerability(
        id: String = "CVE-2021-0000",
        refs: List<String>,
        ratings: List<OshVulnerabilityRatingResponse>? = listOf(OshVulnerabilityRatingResponse(score = 5.0, severity = "medium", method = "CVSSv3")),
    ) = OshVulnerabilityResponse(
        id = id,
        ratings = ratings,
        affects = refs.map { OshVulnerabilityAffectsResponse(it) },
    )

    private fun prop(name: String, value: String) = Property(name, value)

    @Test
    fun map_emptyComponents_returnsEmptyList() {
        assertTrue(OpenSourceHealthMapper.map(makeResponse(emptyList()), "").isEmpty())
    }

    @Test
    fun map_blankSubsystem_returnsAllComponents() {
        val components = listOf(
            makeComponent(name = "a", occurrences = listOf("svcA/pom.xml")),
            makeComponent(name = "b", occurrences = listOf("svcB/pom.xml")),
        )
        assertEquals(2, OpenSourceHealthMapper.map(makeResponse(components), "").size)
    }

    @Test
    fun map_subsystemFilter_excludesNonMatchingComponents() {
        val components = listOf(
            makeComponent(name = "a", occurrences = listOf("svcA/pom.xml")),
            makeComponent(name = "b", occurrences = listOf("svcB/pom.xml")),
        )
        val result = OpenSourceHealthMapper.map(makeResponse(components), "svcA")
        assertEquals(1, result.size)
        assertEquals("a", result[0].name)
    }

    @Test
    fun map_nullEvidence_blankSubsystem_includedWithEmptyFileLocations() {
        val component = makeComponent(name = "noevidence", occurrences = null).copy(evidence = null)
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(1, result.size)
        assertTrue(result[0].fileLocations.isEmpty())
    }

    @Test
    fun map_nullEvidence_withSubsystem_isExcluded() {
        val component = makeComponent(name = "noevidence", occurrences = null).copy(evidence = null)
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "svc")
        assertTrue(result.isEmpty())
    }

    @Test
    fun map_overallRisk_isMaxOfSixRisks() {
        val component = makeComponent(properties = listOf(
            prop("sigrid:risk:vulnerability", "medium"),
            prop("sigrid:risk:legal", "low"),
            prop("sigrid:risk:freshness", "low"),
            prop("sigrid:risk:activity", "low"),
            prop("sigrid:risk:stability", "low"),
            prop("sigrid:risk:management", "low"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(RiskSeverity.Medium, result[0].risk)
    }

    @Test
    fun map_overallRisk_allAbsent_isUnknown() {
        val component = makeComponent(properties = emptyList())
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(RiskSeverity.Unknown, result[0].risk)
    }

    @Test
    fun map_overallRisk_criticalWinsOverHigh() {
        val component = makeComponent(properties = listOf(
            prop("sigrid:risk:vulnerability", "critical"),
            prop("sigrid:risk:legal", "high"),
            prop("sigrid:risk:freshness", "high"),
            prop("sigrid:risk:activity", "high"),
            prop("sigrid:risk:stability", "high"),
            prop("sigrid:risk:management", "high"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(RiskSeverity.Critical, result[0].risk)
    }

    @Test
    fun map_individualRiskFieldsMapped() {
        val component = makeComponent(properties = listOf(
            prop("sigrid:risk:legal", "low"),
            prop("sigrid:risk:vulnerability", "medium"),
            prop("sigrid:risk:freshness", "high"),
            prop("sigrid:risk:activity", "critical"),
            prop("sigrid:risk:stability", "information"),
            prop("sigrid:risk:management", "none"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")[0]
        assertEquals(RiskSeverity.Low,         result.licenseRisk)
        assertEquals(RiskSeverity.Medium,      result.vulnerabilityRisk)
        assertEquals(RiskSeverity.High,        result.freshnessRisk)
        assertEquals(RiskSeverity.Critical,    result.activityRisk)
        assertEquals(RiskSeverity.Information, result.stabilityRisk)
        assertEquals(RiskSeverity.None,        result.managementRisk)
    }

    @Test
    fun map_displayName_withGroup_isGroupSlashName() {
        val component = makeComponent(name = "jackson-databind", group = "com.fasterxml.jackson.core")
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("com.fasterxml.jackson.core/jackson-databind", result[0].displayName)
    }

    @Test
    fun map_displayName_blankGroup_isNameOnly() {
        val component = makeComponent(name = "jackson-databind", group = "")
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("jackson-databind", result[0].displayName)
    }

    @Test
    fun map_dependencyType_directProperty_returnsDirect() {
        val component = makeComponent(properties = listOf(prop("sigrid:transitive", "DIRECT_DEPENDENCY")))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(DependencyType.DIRECT, result[0].dependencyType)
    }

    @Test
    fun map_dependencyType_transitiveProperty_returnsTransitive() {
        val component = makeComponent(properties = listOf(prop("sigrid:transitive", "transitive")))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(DependencyType.TRANSITIVE, result[0].dependencyType)
    }

    @Test
    fun map_dependencyType_missingProperty_returnsUnknown() {
        val component = makeComponent(properties = emptyList())
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(DependencyType.UNKNOWN, result[0].dependencyType)
    }

    @Test
    fun map_fileLocations_countMatchesOccurrences() {
        val component = makeComponent(occurrences = listOf("svc/pom.xml", "svc/build.gradle"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(2, result[0].fileLocations.size)
    }

    @Test
    fun map_fileLocations_componentExtractedBeforeFirstSlash() {
        val component = makeComponent(occurrences = listOf("svc/path/to/pom.xml"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("svc", result[0].fileLocations[0].component)
    }

    @Test
    fun map_fileLocations_filePathNormalizedBySubsystem() {
        val component = makeComponent(occurrences = listOf("svc/path/pom.xml"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "svc")
        assertEquals("path/pom.xml", result[0].fileLocations[0].filePath)
    }

    @Test
    fun map_fileLocations_nullOccurrenceLocationFiltered() {
        val component = makeComponent(occurrences = listOf(null, "svc/pom.xml"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(1, result[0].fileLocations.size)
    }

    @Test
    fun map_sortedDescendingByRiskThenAscendingByDisplayName() {
        val high = prop("sigrid:risk:vulnerability", "high")
        val critical = prop("sigrid:risk:vulnerability", "critical")
        val components = listOf(
            makeComponent(name = "z-lib", properties = listOf(high)),
            makeComponent(name = "a-lib", properties = listOf(critical)),
            makeComponent(name = "b-lib", properties = listOf(high)),
        )
        val result = OpenSourceHealthMapper.map(makeResponse(components), "")
        assertEquals(listOf("a-lib", "b-lib", "z-lib"), result.map { it.name })
    }

    // href

    @Test
    fun map_href_websiteReferencePresent_mapsUrl() {
        val component = makeComponent(externalReferences = listOf(
            OshExternalReference(type = "vcs", url = "https://github.com/example/lib"),
            OshExternalReference(type = "website", url = "https://example.com/lib"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("https://example.com/lib", result[0].href)
    }

    @Test
    fun map_href_firstWebsiteReferenceUsed() {
        val component = makeComponent(externalReferences = listOf(
            OshExternalReference(type = "website", url = "https://first.example.com"),
            OshExternalReference(type = "website", url = "https://second.example.com"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("https://first.example.com", result[0].href)
    }

    @Test
    fun map_href_websiteReferenceWithNullUrl_skipped() {
        val component = makeComponent(externalReferences = listOf(
            OshExternalReference(type = "website", url = null),
            OshExternalReference(type = "website", url = "https://example.com/lib"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("https://example.com/lib", result[0].href)
    }

    @Test
    fun map_href_websiteReferenceWithEmptyUrl_skipped() {
        val component = makeComponent(externalReferences = listOf(
            OshExternalReference(type = "website", url = ""),
            OshExternalReference(type = "website", url = "https://example.com/lib"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals("https://example.com/lib", result[0].href)
    }

    @Test
    fun map_href_noWebsiteReference_isNull() {
        val component = makeComponent(externalReferences = listOf(
            OshExternalReference(type = "vcs", url = "https://github.com/example/lib"),
        ))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(null, result[0].href)
    }

    @Test
    fun map_href_nullExternalReferences_isNull() {
        val component = makeComponent(externalReferences = null)
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertEquals(null, result[0].href)
    }

    // vulnerabilities

    @Test
    fun map_vulnerabilities_matchedByBomRef_attachesToComponent() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val vulnerability = makeVulnerability(id = "CVE-2021-1111", refs = listOf("pkg:maven/lib@1.0"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), listOf(vulnerability)), "")
        assertEquals(listOf("CVE-2021-1111"), result[0].vulnerabilities.map { it.id })
    }

    @Test
    fun map_vulnerabilities_bomRefNull_fallsBackToPurl() {
        val component = makeComponent(name = "lib", bomRef = null)
        val vulnerability = makeVulnerability(id = "CVE-2021-2222", refs = listOf("pkg:maven/lib@1.0"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), listOf(vulnerability)), "")
        assertEquals(listOf("CVE-2021-2222"), result[0].vulnerabilities.map { it.id })
    }

    @Test
    fun map_vulnerabilities_noMatchingRef_isEmptyList() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val vulnerability = makeVulnerability(refs = listOf("pkg:maven/other@2.0"))
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), listOf(vulnerability)), "")
        assertTrue(result[0].vulnerabilities.isEmpty())
    }

    @Test
    fun map_vulnerabilities_noVulnerabilitiesInResponse_isEmptyList() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component)), "")
        assertTrue(result[0].vulnerabilities.isEmpty())
    }

    @Test
    fun map_vulnerabilities_multipleRatings_picksWorstSeverity() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val vulnerability = makeVulnerability(
            refs = listOf("pkg:maven/lib@1.0"),
            ratings = listOf(
                OshVulnerabilityRatingResponse(score = 4.3, severity = "medium", method = "CVSSv2"),
                OshVulnerabilityRatingResponse(score = 9.1, severity = "critical", method = "CVSSv3"),
            ),
        )
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), listOf(vulnerability)), "")[0]
        assertEquals(RiskSeverity.Critical, result.vulnerabilities[0].severity)
        assertEquals(9.1, result.vulnerabilities[0].score)
        assertEquals("CVSSv3", result.vulnerabilities[0].method)
    }

    @Test
    fun map_vulnerabilities_nullRatings_severityIsUnknownAndScoreIsNull() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val vulnerability = makeVulnerability(refs = listOf("pkg:maven/lib@1.0"), ratings = null)
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), listOf(vulnerability)), "")[0]
        assertEquals(RiskSeverity.Unknown, result.vulnerabilities[0].severity)
        assertEquals(null, result.vulnerabilities[0].score)
    }

    @Test
    fun map_vulnerabilities_multipleVulnerabilitiesAffectSameComponent_allAttached() {
        val component = makeComponent(name = "lib", bomRef = "pkg:maven/lib@1.0")
        val vulnerabilities = listOf(
            makeVulnerability(id = "CVE-2021-1111", refs = listOf("pkg:maven/lib@1.0")),
            makeVulnerability(id = "CVE-2021-3333", refs = listOf("pkg:maven/lib@1.0")),
        )
        val result = OpenSourceHealthMapper.map(makeResponse(listOf(component), vulnerabilities), "")
        assertEquals(listOf("CVE-2021-1111", "CVE-2021-3333"), result[0].vulnerabilities.map { it.id })
    }
}
