package org.example

import java.io.File
import java.util.*

private const val PACKAGE_NAME = "com.flipperdevices.busybar.theme.generated"
private const val PALLET_NAME = "BusyBarPallet"

fun main() {
    val parsedModes = parse(File("BSBPallet.json"))
    val modes = preventBadNames(parsedModes)
    val lightMode = modes.values.find { it.name == "Light" }!!
    val outputDir = File("out")
    outputDir.deleteRecursively()
    outputDir.mkdirs()
    generatePalletFile(
        packageName = PACKAGE_NAME,
        outputFile = File(outputDir, "$PALLET_NAME.kt"),
        mode = lightMode
    )
    modes.forEach { (_, mode) ->
        generateModeFile(
            palletName = PALLET_NAME,
            folder = outputDir,
            packageName = PACKAGE_NAME,
            mode = mode
        )
    }

    generateAnimatePalletFile(
        outputFile = File("out/AnimatedPallet.kt"),
        mode = lightMode,
        palletName = PALLET_NAME,
        packageName = PACKAGE_NAME
    )
}

private fun preventBadNames(modes: Map<String, VariableMode>): Map<String, VariableMode> {
    return modes.mapValues { (_, mode) ->
        mode.copy(
            colors = mode.colors.mapName("") {
                filterName(it)
            }.second as ColorTreeElement.Collection
        )
    }
}

private fun filterName(name: String): String {
    var formattedName = name.replaceFirstChar { if (it.isLowerCase()) it else it.lowercaseChar() }
    formattedName = formattedName
        .split("&")
        .mapIndexed { index, s ->
            if (index != 0) {
                s.capitalize()
            } else s
        }
        .joinToString("And")
    formattedName = formattedName
        .split(" ", "_")
        .mapIndexed { index, s ->
            if (index != 0) {
                s.capitalize()
            } else s
        }
        .joinToString("")

    println("$name -> $formattedName")
    return formattedName
}