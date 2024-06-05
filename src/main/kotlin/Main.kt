package org.example

import java.io.File

fun main() {
    val parsedModes = parse(File("Colors.json"))
    val modes = preventBadNames(parsedModes)
    generatePalletFile(
        packageName = "com.flipperdevices.core.ui.theme.composable.pallete",
        outputFile = File("out/FlipperPalletV2.kt"),
        mode = modes.values.find { it.name == "Light" }!!
    )
    modes.forEach { (_, mode) ->
        generateModeFile(
            palletName = "FlipperPalletV2",
            folder = File("out"),
            packageName = "com.flipperdevices.core.ui.theme.composable.pallete",
            mode = mode
        )
    }
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