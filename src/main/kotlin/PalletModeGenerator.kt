package org.example

import com.squareup.kotlinpoet.*
import java.awt.Color
import java.io.File

fun generateModeFile(
    folder: File,
    mode: VariableMode,
    palletName: String,
    packageName: String
) {
    val outputFile = File(folder, "${mode.name.capitalize()}Pallet.kt")
    val fileCode = FileSpec.builder(packageName, outputFile.nameWithoutExtension)
        .addFunction(generatePalletFunction(mode, packageName, palletName))
        .addAliasedImport(ClassName("$packageName.$palletName.Surface.Fade", "TransparentBlack"), "FadeTransparentBlack")
        .addAliasedImport(ClassName("$packageName.$palletName.Surface.Fade", "TransparentWhite"), "FadeTransparentWhite")
        .build()
        .toString()
    outputFile.writeText(fileCode)
}

private fun getCollectionChilds(
    element: ColorTreeElement.Collection
): List<String> {
    val items = mutableListOf<String>()
    element.map.forEach { (key, value) ->
        when (value) {
            is ColorTreeElement.Collection -> {
                items.add(key.capitalize())
                val subChilds = getCollectionChilds(value)
                subChilds.forEach { items.add("${key.capitalize()}." + it) }
            }

            is ColorTreeElement.Color -> {}
        }
    }
    return items
}


private fun generatePalletFunction(mode: VariableMode, packageName: String, palletName: String): FunSpec {
    val mainBuilder = FunSpec.builder("get${mode.name}Pallet")
        .addModifiers(KModifier.INTERNAL)
        .returns(ClassName(packageName, palletName))

    val codeBlock = CodeBlock.builder()

    codeBlock.add("return %T(\n⇥", ClassName(packageName, palletName))
    addCollectionToCodeBlock(codeBlock, mode.colors, "$packageName.$palletName")
    codeBlock.add("⇤)")

    return mainBuilder
        .addCode(codeBlock.build())
        .build()
}

private fun addTreeElementToCodeBlock(
    codeBlock: CodeBlock.Builder,
    name: String,
    treeElement: ColorTreeElement,
    packageName: String
) {
    when (treeElement) {
        is ColorTreeElement.Collection -> {
            codeBlock.add("%N = %T(\n⇥", name, ClassName(packageName, name.capitalize()))
            addCollectionToCodeBlock(codeBlock, treeElement, "$packageName.${name.capitalize()}")
            codeBlock.add("⇤)")
        }

        is ColorTreeElement.Color -> codeBlock.add(
            "$name = %T(0x${treeElement.color.toHexString()})",
            ClassName("androidx.compose.ui.graphics", "Color")
        )
    }
}

private fun addCollectionToCodeBlock(
    codeBlock: CodeBlock.Builder,
    element: ColorTreeElement.Collection,
    packageName: String
) {
    val entries = element.map.entries
    entries.forEachIndexed { index, (name, element) ->
        addTreeElementToCodeBlock(codeBlock, name, element, packageName)
        if (index != entries.size - 1) {
            codeBlock.add(",")
        }
        codeBlock.add("\n")
    }
}

private fun Color.toHexString(): String {
    return String.format("%02X%02X%02X%02X", alpha, red, green, blue);
}