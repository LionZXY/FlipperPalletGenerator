package org.example

import org.example.android.ColorTreeElement
import org.example.android.VariableMode
import org.example.android.generateAnimatePalletFile
import org.example.android.generateModeFile
import org.example.android.generatePalletFile
import org.example.android.parse
import org.example.ios.generateSwiftPalletFile
import org.example.ios.generateXcodeColorAsset
import java.io.File

private const val PACKAGE_NAME = "com.flipperdevices.busybar.theme.generated"
private const val PALLET_NAME = "Color"
private const val PALLET_FILE = "BSB.json"

fun main() {
    val parsedModes = parse(File(PALLET_FILE))
    val modes = preventBadNames(parsedModes)
    val lightMode = modes.values.find { it.name == "Light" }!!

    // Output
    val outputDir = File("out")
    val iosOutputDir =  File("out/iOS")
    val androidOutputDir =  File("out/Android")

    outputDir.deleteRecursively()
    outputDir.mkdirs()
    iosOutputDir.mkdirs()
    androidOutputDir.mkdirs()

    // Kotlin
    generatePalletFile(
        packageName = PACKAGE_NAME,
        outputFile = File(androidOutputDir, "$PALLET_NAME.kt"),
        mode = lightMode
    )
    modes.forEach { (_, mode) ->
        generateModeFile(
            palletName = PALLET_NAME,
            folder = androidOutputDir,
            packageName = PACKAGE_NAME,
            mode = mode
        )
    }

    generateAnimatePalletFile(
        outputFile = File(androidOutputDir, "AnimatedPallet.kt"),
        mode = lightMode,
        palletName = PALLET_NAME,
        packageName = PACKAGE_NAME
    )

    generateSwiftPalletFile(
        outputFile = File(iosOutputDir, "$PALLET_NAME.swift"),
        modes = modes
    )

    val iosXcodeOutputDir = File(iosOutputDir, "Colors").apply { mkdirs() }
    generateXcodeColorAsset(
        outputFolder = iosXcodeOutputDir,
        modes = modes
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

    // println("$name -> $formattedName")
    return formattedName
}