package org.example.ios

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.android.ColorTreeElement
import org.example.android.VariableMode
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun generateXcodeColorAsset(
    outputFolder: File,
    modes: Map<String, VariableMode>,
) {
    val light = modes.values.first { it.name == "Light" }.colors.map
    val dark = modes.values.first { it.name == "Dark" }.colors.map

    val rootContentFile = File(outputFolder, "Contents.json")
    val rootContent = json.encodeToString(XcodeContents.default)
    rootContentFile.writeText(rootContent)

    process(light, dark, outputFolder)
}

private fun process(
    light: Map<String, ColorTreeElement>,
    dark: Map<String, ColorTreeElement>,
    folder: File,
    colorName: String = ""
) {
    light.forEach { (name, lightElement) ->
        var darkElement: ColorTreeElement = dark[name] ?: throw Exception("Not found dark color for '$name'")
        when (lightElement) {
            is ColorTreeElement.Collection -> {
                darkElement = darkElement as? ColorTreeElement.Collection ?: throw Exception("Dark element is not a collection")
                println("Process color collection ${name + colorName}")
                process(lightElement.map, darkElement.map, folder, colorName.capitalize() + name.capitalize())
            }
            is ColorTreeElement.Color -> {
                darkElement = darkElement as? ColorTreeElement.Color ?: throw Exception("Dark element is not a color")

                val lightColor = Color(
                    colorSpace = "srgb",
                    components = Components(
                        alpha = lightElement.alpha,
                        red = lightElement.red,
                        green = lightElement.green,
                        blue = lightElement.blue
                    ),
                )

                val lightColorDetail = ColorDetail(
                    color = lightColor,
                    idiom = "universal",
                    appearances = null
                )

                val darkColor = Color(
                    colorSpace = "srgb",
                    components = Components(
                        alpha = darkElement.alpha,
                        red = darkElement.red,
                        green = darkElement.green,
                        blue = darkElement.blue
                    )
                )

                val darkColorDetail = ColorDetail(
                    color = darkColor,
                    idiom = "universal",
                    appearances = listOf(
                        Appearance("luminosity", "dark")
                    )
                )

                val colorSet = XcodeColorSet(
                    colors = listOf(lightColorDetail, darkColorDetail),
                    info = Info(author = "xcode", version = 1)
                )

                val name = colorName.capitalize() + name.capitalize()
                val colorSetFolder = File(folder, "${name.capitalize()}.colorset").apply { mkdir() }
                val contentFile = File(colorSetFolder, "Contents.json")
                val content = json.encodeToString(colorSet)
                contentFile.writeText(content)
            }
        }
    }
}

private val ColorTreeElement.Color.alpha: String
    get() = String.format("%.3f", color.alpha / 255.0)

private val ColorTreeElement.Color.blue: String
    get() = "0x${String.format("%02X", color.blue)}"

private val ColorTreeElement.Color.green: String
    get() = "0x${String.format("%02X", color.green)}"

private val ColorTreeElement.Color.red: String
    get() = "0x${String.format("%02X", color.red)}"

@Serializable
data class XcodeContents(@SerialName("info") val info: Info) {
    companion object {
        val default = XcodeContents(
            info = Info(
                author = "xcode",
                version = 1
            )
        )
    }
}

@Serializable
data class XcodeColorSet(
    val colors: List<ColorDetail>,
    val info: Info
)

@Serializable
data class ColorDetail(
    val appearances: List<Appearance>? = null,
    val color: Color,
    val idiom: String,
)

@Serializable
data class Color(
    @SerialName("color-space") val colorSpace: String,
    val components: Components
)

@Serializable
data class Components(
    val alpha: String,
    val blue: String,
    val green: String,
    val red: String
)

@Serializable
data class Appearance(
    val appearance: String,
    val value: String
)

@Serializable
data class Info(
    @SerialName("author")
    val author: String,
    @SerialName("version")
    val version: Int
)