package org.example.android

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.awt.Color
import java.io.File
import kotlin.math.roundToInt


private val gson = GsonBuilder().create()

sealed class ColorTreeElement {
    data class Color(
        val color: java.awt.Color
    ) : ColorTreeElement()

    data class Collection(
        val map: Map<String, ColorTreeElement>
    ) : ColorTreeElement()

    fun mapName(
        keyName: String,
        block: (name: String) -> String
    ): Pair<String, ColorTreeElement> {
        return when (this) {
            is Collection -> block(keyName) to copy(map = map.map { (key, value) -> value.mapName(key, block) }.toMap())
            is Color -> block(keyName) to this
        }
    }
}

data class VariableMode(
    val name: String,
    val colors: ColorTreeElement.Collection
)

fun parse(jsonFile: File): Map<String, VariableMode> {
    val mapAdapter = gson.getAdapter(object : TypeToken<Map<String, Any?>>() {})
    val model: Map<String, Any?> = mapAdapter.fromJson(jsonFile.readText())
    var modes = HashMap<String, VariableMode>()

    (model["modes"] as Map<String, Any>).forEach { mode, name ->
        modes[mode] = VariableMode(name = name as String, colors = ColorTreeElement.Collection(emptyMap()))
    }

    val variables = model["variables"] as List<Map<String, Any>>
    var modesImmutableMap = modes.toMap()
    variables.forEach {
        modesImmutableMap = parseOneVariable(modesImmutableMap, it)
    }
    // println(modesImmutableMap)
    return modesImmutableMap
}

private fun parseOneVariable(modes: Map<String, VariableMode>, variable: Map<String, Any>): Map<String, VariableMode> {
    val modesMap = modes.toMutableMap()
    val name = variable["name"] as String
    val values = variable["resolvedValuesByMode"] as Map<String, Any>
    values.forEach { (key, value) ->
        val parsedValue = (value as Map<String, Any>)["resolvedValue"] as Map<String, Any>
        modesMap[key] = addColorToMode(modesMap[key]!!, name, extractColor(parsedValue))
    }
    return modesMap
}

private fun addColorToMode(mode: VariableMode, colorName: String, color: Color): VariableMode {
    val paths = colorName.split("/")
    return mode.copy(
        colors = addColorToTreeElement(
            mode.colors,
            paths,
            color
        )
    )
}

private fun addColorToTreeElement(
    element: ColorTreeElement.Collection,
    paths: List<String>,
    color: Color
): ColorTreeElement.Collection {
    val currentName = paths[0]
    return if (paths.size == 1) {
        element.copy(
            map = element.map.plus(currentName to ColorTreeElement.Color(color))
        )
    } else {
        val currentElement = element.map[currentName] as? ColorTreeElement.Collection
            ?: ColorTreeElement.Collection(emptyMap())
        element.copy(
            map = element.map.plus(
                currentName to addColorToTreeElement(
                    currentElement as ColorTreeElement.Collection,
                    paths.drop(1),
                    color
                )
            )
        )
    }
}

private fun extractColor(resolvedValue: Map<String, Any>): Color {
    val red = ((resolvedValue["r"] as Double) * 255).roundToInt()
    val green = ((resolvedValue["g"] as Double) * 255).roundToInt()
    val blue = ((resolvedValue["b"] as Double) * 255).roundToInt()
    val alpha = ((resolvedValue["a"] as Double) * 255).roundToInt()
    return Color(red, green, blue, alpha)
}