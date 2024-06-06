package org.example

import java.io.File

private const val PACKAGE_NAME = "com.flipperdevices.core.ui.theme.composable.pallete"
private const val PALLET_NAME = "FlipperPalletV2"

fun main() {
    val parsedModes = parse(File("Colors.json"))
    val modes = preventBadNames(parsedModes)
    val lightMode = modes.values.find { it.name == "Light" }!!
    generatePalletFile(
        packageName = PACKAGE_NAME,
        outputFile = File("out/$PALLET_NAME.kt"),
        mode = lightMode
    )
    modes.forEach { (_, mode) ->
        generateModeFile(
            palletName = PALLET_NAME,
            folder = File("out"),
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
    var formattedName = name
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