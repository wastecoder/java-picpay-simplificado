import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

// Mutable state populated during test execution and read at print time.
val collectedTotal = java.util.concurrent.atomic.AtomicLong(0)
val collectedPassed = java.util.concurrent.atomic.AtomicLong(0)
val collectedFailed = java.util.concurrent.atomic.AtomicLong(0)
val collectedSkipped = java.util.concurrent.atomic.AtomicLong(0)
val collectedStart = java.util.concurrent.atomic.AtomicLong(0)
val collectedEnd = java.util.concurrent.atomic.AtomicLong(0)
val collectedFailures = java.util.Collections.synchronizedList(mutableListOf<Pair<String, String>>())

tasks.named<Test>("test").configure {
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                val className = testDescriptor.className ?: "(unknown)"
                val method = testDescriptor.name
                val displayName = testDescriptor.displayName
                collectedFailures.add("$className.$method" to displayName)
            }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                collectedTotal.set(result.testCount)
                collectedPassed.set(result.successfulTestCount)
                collectedFailed.set(result.failedTestCount)
                collectedSkipped.set(result.skippedTestCount)
                collectedStart.set(result.startTime)
                collectedEnd.set(result.endTime)
            }
        }
    })
    finalizedBy("printTestSummary")
}

tasks.matching { it.name == "jacocoTestReport" }.configureEach {
    finalizedBy("printTestSummary")
}

tasks.matching { it.name == "pitest" }.configureEach {
    finalizedBy("printTestSummary")
}

tasks.register("printTestSummary") {
    mustRunAfter("test", "jacocoTestReport", "pitest")
    doLast {
        printSummary()
    }
}

fun printSummary() {
    val xmlFileJacoco = file("build/reports/jacoco/test/jacocoTestReport.xml")
    val pitestXml = file("build/reports/pitest/mutations.xml")
    val hasTestData = collectedTotal.get() > 0L || collectedFailed.get() > 0L || collectedSkipped.get() > 0L
    if (!hasTestData && !xmlFileJacoco.exists() && !pitestXml.exists()) {
        return
    }

    val width = 60
    val sep = "=".repeat(width)
    val subSep = "-".repeat(width)
    val out = StringBuilder()

    out.appendLine()
    if (hasTestData) {
        out.appendLine(sep)
        out.appendLine("TEST SUMMARY")
        out.appendLine(subSep)
        out.appendLine("Total:     ${collectedTotal.get()}")
        out.appendLine("Passed:    ${collectedPassed.get()}")
        out.appendLine("Failed:    ${collectedFailed.get()}")
        out.appendLine("Skipped:   ${collectedSkipped.get()}")
        out.appendLine("Time:      ${formatTime(collectedEnd.get() - collectedStart.get())}")
        if (collectedFailures.isNotEmpty()) {
            out.appendLine("Failed tests:")
            synchronized(collectedFailures) {
                for ((id, displayName) in collectedFailures) {
                    out.appendLine("  - $id  ($displayName)")
                }
            }
        }
    }

    if (xmlFileJacoco.exists()) {
        try {
            val cov = parseCoverage(xmlFileJacoco)
            out.appendLine(sep)
            out.appendLine("CODE COVERAGE (JaCoCo)")
            out.appendLine(subSep)
            out.appendLine(formatCoverageRow("Instructions", cov.instructions))
            out.appendLine(formatCoverageRow("Lines",        cov.lines))
            out.appendLine(formatCoverageRow("Branches",     cov.branches))
            out.appendLine(formatCoverageRow("Methods",      cov.methods))
            if (cov.useCaseClasses.isNotEmpty()) {
                out.appendLine(sep)
                out.appendLine("Use cases breakdown:")
                val maxName = cov.useCaseClasses.keys.maxOf { it.length }
                for ((name, lc) in cov.useCaseClasses.entries.sortedBy { it.key }) {
                    val total = lc.missed + lc.covered
                    val pct = pct(lc.covered, total).padStart(6)
                    out.appendLine("  ${name.padEnd(maxName)}  $pct (${lc.covered}/$total)")
                }
            }
        } catch (ex: Exception) {
            logger.warn("Failed to parse JaCoCo XML: ${ex.message}")
        }
    }

    if (pitestXml.exists()) {
        try {
            val mut = parseMutations(pitestXml)
            out.appendLine(sep)
            out.appendLine("MUTATION TESTING (Pitest)")
            out.appendLine(subSep)
            out.appendLine(formatMutationRow("Mutation score", mut.killed, mut.total))
            out.appendLine(formatMutationRow("Survived",       mut.survived, mut.total))
            out.appendLine(formatMutationRow("No coverage",    mut.noCoverage, mut.total))
            if (mut.other > 0) {
                out.appendLine(formatMutationRow("Other",      mut.other, mut.total))
            }
        } catch (ex: Exception) {
            logger.warn("Failed to parse Pitest XML: ${ex.message}")
        }
    }

    out.appendLine(sep)
    val osCmd = osCommand()
    val jacocoHtml = file("build/reports/jacoco/test/html/index.html")
    val junitHtml = file("build/reports/tests/test/index.html")
    val pitestHtml = file("build/reports/pitest/index.html")
    if (jacocoHtml.exists() || junitHtml.exists() || pitestHtml.exists()) {
        out.appendLine("HTML reports:")
        if (jacocoHtml.exists()) {
            out.appendLine("  JaCoCo:  ${normalizeUri(jacocoHtml)}")
            out.appendLine("           $osCmd build/reports/jacoco/test/html/index.html")
        }
        if (junitHtml.exists()) {
            out.appendLine("  JUnit:   ${normalizeUri(junitHtml)}")
            out.appendLine("           $osCmd build/reports/tests/test/index.html")
        }
        if (pitestHtml.exists()) {
            out.appendLine("  Pitest:  ${normalizeUri(pitestHtml)}")
            out.appendLine("           $osCmd build/reports/pitest/index.html")
        }
        out.appendLine(sep)
    }
    out.appendLine("Run mutation testing:  ./gradlew pitest")
    out.appendLine(sep)

    print(out.toString())
}

data class MutationData(
    val total: Int,
    val killed: Int,
    val survived: Int,
    val noCoverage: Int,
    val other: Int
)

fun parseMutations(xml: java.io.File): MutationData {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    dbf.isValidating = false
    val doc = dbf.newDocumentBuilder().parse(xml)
    val mutations = directChildren(doc.documentElement, "mutation")

    var killed = 0
    var survived = 0
    var noCoverage = 0
    var other = 0
    for (m in mutations) {
        when (m.getAttribute("status")) {
            "KILLED"      -> killed++
            "SURVIVED"    -> survived++
            "NO_COVERAGE" -> noCoverage++
            else          -> other++
        }
    }
    return MutationData(
        total = mutations.size,
        killed = killed,
        survived = survived,
        noCoverage = noCoverage,
        other = other
    )
}

fun formatMutationRow(label: String, count: Int, total: Int): String {
    val pctStr = pct(count.toLong(), total.toLong()).padStart(6)
    val labelPart = (label + ":").padEnd(15)
    return "$labelPart $pctStr ($count/$total)"
}

data class JCounter(val missed: Long, val covered: Long)

data class CoverageData(
    val instructions: JCounter,
    val lines: JCounter,
    val branches: JCounter,
    val methods: JCounter,
    val useCaseClasses: Map<String, JCounter>
)

fun parseCoverage(xml: java.io.File): CoverageData {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    dbf.isValidating = false
    val doc = dbf.newDocumentBuilder().parse(xml)
    val root = doc.documentElement

    val rootCounters = directChildren(root, "counter")
        .associate { it.getAttribute("type") to counterOf(it) }

    val useCaseClasses = mutableMapOf<String, JCounter>()
    for (pkg in directChildren(root, "package")) {
        val pkgName = pkg.getAttribute("name") ?: continue
        if (!pkgName.endsWith("/usecases")) continue
        for (cls in directChildren(pkg, "class")) {
            val fqn = cls.getAttribute("name") ?: continue
            val short = fqn.removePrefix("com/wastecoder/picpay/").replace('/', '.')
            val lineCounter = directChildren(cls, "counter")
                .firstOrNull { it.getAttribute("type") == "LINE" }
            if (lineCounter != null) {
                useCaseClasses[short] = counterOf(lineCounter)
            }
        }
    }

    return CoverageData(
        instructions = rootCounters["INSTRUCTION"] ?: JCounter(0, 0),
        lines        = rootCounters["LINE"]        ?: JCounter(0, 0),
        branches     = rootCounters["BRANCH"]      ?: JCounter(0, 0),
        methods      = rootCounters["METHOD"]      ?: JCounter(0, 0),
        useCaseClasses = useCaseClasses
    )
}

fun counterOf(el: Element): JCounter = JCounter(
    missed = (el.getAttribute("missed").takeIf { it.isNotBlank() } ?: "0").toLong(),
    covered = (el.getAttribute("covered").takeIf { it.isNotBlank() } ?: "0").toLong()
)

fun directChildren(parent: Element, name: String): List<Element> {
    val result = mutableListOf<Element>()
    val children = parent.childNodes
    for (i in 0 until children.length) {
        val n = children.item(i)
        if (n.nodeType == Node.ELEMENT_NODE && n is Element && n.tagName == name) {
            result.add(n)
        }
    }
    return result
}

fun pct(covered: Long, total: Long): String =
    if (total == 0L) "n/a" else "%.1f%%".format(covered.toDouble() * 100.0 / total.toDouble())

fun formatCoverageRow(label: String, c: JCounter): String {
    val total = c.missed + c.covered
    val pctStr = pct(c.covered, total).padStart(6)
    val labelPart = (label + ":").padEnd(13)
    return "$labelPart $pctStr (${c.covered}/$total)"
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0.000s"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val msPart = ms % 1000
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
           else "%02d:%02d.%03d".format(minutes, seconds, msPart)
}

fun normalizeUri(file: java.io.File): String {
    val raw = file.toURI().toString()
    return if (raw.startsWith("file:/") && !raw.startsWith("file:///")) {
        raw.replaceFirst("file:/", "file:///")
    } else raw
}

fun osCommand(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> "start"
        osName.contains("mac") -> "open"
        else -> "xdg-open"
    }
}
