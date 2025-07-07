package com.pixel365.goverage

import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

@Suppress("UnstableApiUsage")
class GoverageInlayHintsProvider : InlayHintsProvider<NoSettings> {

    override val name = "Goverage Hints"
    override val key: SettingsKey<NoSettings> = SettingsKey("goverage.inlay.hints")
    override val previewText: String? = "package main\n\nfunc main() {}"

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val factory = factory

                if (element is GoFile) {
                    val pkg = element.getPackage()
                    val vfile = element.virtualFile
                    val base = editor.project?.baseDir
                    val relPath = vfile?.let { base?.let { VfsUtilCore.getRelativePath(vfile, it) } }

                    if (pkg != null && relPath != null) {
                        val percent = CoverageCache.getCoverageForFile(editor.project!!, relPath)
                        if (percent != null) {
                            val text = "File Coverage: $percent%"
                            sink.addBlockElement(
                                pkg.textOffset,
                                relatesToPrecedingText = false,
                                showAbove = true,
                                priority = 0,
                                presentation = factory.smallText(text)
                            )
                        }
                    }
                }

                return true
            }
        }
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): javax.swing.JComponent {
                return javax.swing.JPanel()
            }
        }
    }

    override fun createSettings(): NoSettings = NoSettings()
}
