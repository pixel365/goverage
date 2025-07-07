package com.pixel365.goverage

import com.intellij.openapi.project.Project
import java.io.File

data class CoverageEntry(
    val startLine: Int,
    val endLine: Int,
    val statements: Int,
    val count: Int
)

object CoverageCache {
    private var lastModified: Long = 0
    private var coverageMap: Map<String, Pair<Int, Int>> = emptyMap()
    private var detailedMap: Map<String, List<CoverageEntry>> = emptyMap()

    fun getCoverageForFile(project: Project, absPath: String): Double? {
        val basePath = project.basePath ?: return null
        val relPath = absPath.removePrefix(basePath).removePrefix("/")

        val goModFile = File(basePath, "go.mod")
        if (!goModFile.exists()) return null

        val moduleName = readModuleName(basePath) ?: return null

        val coverageFile = File(basePath, "coverage.out")
        if (!coverageFile.exists()) return null

        if (coverageFile.lastModified() > lastModified) {
            coverageMap = parseCoverageFile(coverageFile)
            lastModified = coverageFile.lastModified()
        }

        val coverageKey = "$moduleName/$relPath"
        val (covered, total) = coverageMap[coverageKey] ?: return null

        return if (total > 0)
            (covered.toDouble() * 100 / total).let { Math.round(it * 100) / 100.0 }
        else null
    }

    fun getCoverageEntriesForFile(project: Project, absPath: String): List<CoverageEntry>? {
        val basePath = project.basePath ?: return null
        val relPath = absPath.removePrefix(basePath).removePrefix("/")

        val moduleName = readModuleName(basePath) ?: return null
        val coverageFile = File(basePath, "coverage.out")
        if (!coverageFile.exists()) return null

        if (coverageFile.lastModified() > lastModified) {
            parseCoverageFile(coverageFile)
            lastModified = coverageFile.lastModified()
        }

        val key = "$moduleName/$relPath"
        return detailedMap[key]
    }

    private fun readModuleName(basePath: String): String? {
        val goModFile = File(basePath, "go.mod")
        if (!goModFile.exists()) return null
        return goModFile.useLines { lines ->
            lines.firstOrNull { it.startsWith("module ") }
                ?.removePrefix("module ")
                ?.trim()
        }
    }

    private fun parseCoverageFile(file: File): Map<String, Pair<Int, Int>> {
        val totals = mutableMapOf<String, Pair<Int, Int>>()
        val entries = mutableMapOf<String, MutableList<CoverageEntry>>()

        file.useLines { lines ->
            for (line in lines) {
                if (line.startsWith("mode:")) continue

                val parts = line.split(" ")
                if (parts.size < 3) continue

                val location = parts[0]
                val filePart = location.substringBefore(":")
                val range = location.substringAfter(":").split(",")
                if (range.size != 2) continue

                val startLine = range[0].substringBefore(".").toIntOrNull() ?: continue
                val endLine = range[1].substringBefore(".").toIntOrNull() ?: continue
                val statements = parts[1].toIntOrNull() ?: continue
                val count = parts[2].toIntOrNull() ?: continue

                val (covered, total) = totals.getOrDefault(filePart, 0 to 0)
                totals[filePart] = (
                        covered + if (count > 0) statements else 0
                        ) to (total + statements)

                entries.computeIfAbsent(filePart) { mutableListOf() }.add(
                    CoverageEntry(startLine, endLine, statements, count)
                )
            }
        }

        detailedMap = entries
        return totals
    }
}
