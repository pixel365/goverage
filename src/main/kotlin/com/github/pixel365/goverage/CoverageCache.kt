package com.pixel365.goverage

import com.intellij.openapi.project.Project
import java.io.File

object CoverageCache {
    private var lastModified: Long = 0
    private var coverageMap: Map<String, Pair<Int, Int>> = emptyMap()

    fun getCoverageForFile(project: Project, absPath: String): Double? {
        val basePath = project.basePath ?: return null
        val relPath = absPath.removePrefix(basePath).removePrefix("/")

        val goModFile = File(basePath, "go.mod")
        if (!goModFile.exists()) return null

        val moduleName = goModFile.useLines { lines ->
            lines.firstOrNull { it.startsWith("module ") }
                ?.removePrefix("module ")
                ?.trim()
        } ?: return null

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

    private fun parseCoverageFile(file: File): Map<String, Pair<Int, Int>> {
        val result = mutableMapOf<String, Pair<Int, Int>>()

        file.useLines { lines ->
            for (line in lines) {
                if (line.startsWith("mode:")) continue

                val parts = line.split(" ")
                if (parts.size < 3) continue

                val filePart = parts[0].substringBefore(":")
                val statementCount = parts[1].toIntOrNull() ?: continue
                val executionCount = parts[2].toIntOrNull() ?: continue

                val (covered, total) = result.getOrDefault(filePart, 0 to 0)
                val isCovered = executionCount > 0

                result[filePart] = (
                        covered + if (isCovered) statementCount else 0
                        ) to (total + statementCount)
            }
        }

        return result
    }
}
