package org.example.ios

import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.ExtensionSpec
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.Modifier
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.TypeSpec
import org.example.android.ColorTreeElement
import org.example.android.VariableMode
import java.io.File
import java.util.Locale

fun generateSwiftPalletFile(
    outputFile: File,
    modes: Map<String, VariableMode>,
) {
    val palletName = outputFile.nameWithoutExtension

    val light = modes.values.first { it.name == "Light" }.colors
    val dark = modes.values.first { it.name == "Dark" }.colors

    val result = generateExtensionAndTypes(light, dark)

    val fileSpec = FileSpec.builder(palletName)
        .addComment("""
            DO NOT EDIT.
            swift-format-ignore-file
            swiftlint:disable line_length
            Autogenerated code from https://github.com/LionZXY/FlipperPalletGenerator
            """.trimIndent()
        )
        .addExtension(result.first)
        .apply { result.second.forEach { addType(it) } }
        .addExtension(generateColorExtension())
        .addImport("SwiftUI")
        .indent("    ")
        .build()

    val content = "$fileSpec// swiftlint:enable line_length\n" // IDK addComment doest work on end

    outputFile.writeText(content)
}

private fun generateExtensionAndTypes(
    light: ColorTreeElement.Collection,
    dark: ColorTreeElement.Collection
): Pair<ExtensionSpec, List<TypeSpec>> {

    val extensionBuilder = ExtensionSpec
        .builder(DeclaredTypeName("SwiftUI", "View"))

    val types = mutableListOf<TypeSpec>()

    light.map.forEach { (name, lightElement) ->
        val darkElement = dark.map[name]
            ?: throw Exception("Not found dark color for '$name'")

        val className = name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } + "Colors"

        extensionBuilder.addProperty(
            PropertySpec
                .builder(name, DeclaredTypeName("", className))
                .getter(
                    FunctionSpec.getterBuilder()
                        .addStatement("$className()")
                        .build()
                )
                .build()
        )

        val typeBuilder = TypeSpec.structBuilder(className)

        addColorElement(
            builder = typeBuilder,
            name = name,
            lightElement = lightElement,
            darkElement = darkElement,
            isFirstLevel = true
        )

        types.add(typeBuilder.build())
    }

    return extensionBuilder.build() to types
}

private fun addColorElement(
    builder: TypeSpec.Builder,
    name: String,
    lightElement: ColorTreeElement,
    darkElement: ColorTreeElement,
    isFirstLevel: Boolean = false
) {
    when (lightElement) {
        is ColorTreeElement.Color -> {
            if (darkElement is ColorTreeElement.Color) {
                builder.addProperty(
                    PropertySpec
                        .builder(name, DeclaredTypeName("SwiftUI", "Color"))
                        .initializer(
                            "Color(any: Color(red: %L, green: %L, blue: %L, opacity: %L), dark: Color(red: %L, green: %L, blue: %L, opacity: %L))",
                            lightElement.red,
                            lightElement.green,
                            lightElement.blue,
                            lightElement.alpha,
                            darkElement.red,
                            darkElement.green,
                            darkElement.blue,
                            darkElement.alpha
                        )
                        .build()
                )
            } else {
                throw Exception("Mismatched types: expected Color for dark mode at '$name'")
            }
        }

        is ColorTreeElement.Collection -> {
            println("Generate name collection $name")
            if (darkElement is ColorTreeElement.Collection) {
                val structName = name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } + "Colors"
                println("Generate structName $name")

                val innerBuilder = if (isFirstLevel) {
                    builder
                } else {
                    TypeSpec.structBuilder(structName)
                }

                lightElement.map.forEach { (innerName, innerLightElement) ->
                    val innerDarkElement = darkElement.map[innerName]
                        ?: throw Exception("Not found dark color for '$innerName' in '$name'")

                    addColorElement(
                        builder = innerBuilder,
                        name = innerName,
                        lightElement = innerLightElement,
                        darkElement = innerDarkElement
                    )
                }

                builder.addType(innerBuilder.build())

                builder.addProperty(
                    PropertySpec
                        .builder(name, DeclaredTypeName("", structName))
                        .initializer("$structName()")
                        .build()
                )
            } else {
                throw Exception("Mismatched types: expected Collection for dark mode at '$name'")
            }
        }
    }
}


private fun generateColorExtension(): ExtensionSpec {
    return ExtensionSpec
        .builder(DeclaredTypeName("SwiftUI", "Color"))
        .addModifiers(Modifier.FILEPRIVATE)
        .addFunction(
            FunctionSpec
                .constructorBuilder()
                .addParameter("any", type = DeclaredTypeName("SwiftUI", "Color"))
                .addParameter("dark", type = DeclaredTypeName("SwiftUI", "Color"))
                .addCode(
                    """
                    #if canImport(UIKit)
                    self.init(UIColor { traitCollection in
                        traitCollection.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(any)
                    })
                    #elseif canImport(AppKit)
                    self.init(NSColor { appearance in
                        let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
                        return isDark ? NSColor(dark) : NSColor(any)
                    })
                    #else
                    self = any
                    #endif
                    
                    """.trimIndent()
                )
                .build()
        )
        .build()
}

private val ColorTreeElement.Color.red: Float
    get() = color.red / 255f

private val ColorTreeElement.Color.green: Float
    get() = color.green / 255f

private val ColorTreeElement.Color.blue: Float
    get() = color.blue / 255f

private val ColorTreeElement.Color.alpha: Float
    get() = color.alpha / 255f
