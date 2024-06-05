package org.example

import com.squareup.kotlinpoet.*
import java.io.File


fun generatePalletFile(
    packageName: String,
    outputFile: File,
    mode: VariableMode
) {
    val fileCode = FileSpec.builder(packageName, outputFile.nameWithoutExtension)
        .addType(generatePalletClass(mode, outputFile.nameWithoutExtension))
        .build()
        .toString()
        .replace("public data class", "data class")
        .replace("public val", "val")

    outputFile.writeText(fileCode)
}

private fun generatePalletClass(mode: VariableMode, palletName: String): TypeSpec {
    val mainBuilder = TypeSpec.classBuilder(palletName)
        .addModifiers(KModifier.DATA)

    val primaryConstructor = FunSpec.constructorBuilder()

    mode.colors.map.forEach { (name, element) ->
        addColorElement(mainBuilder, primaryConstructor, name, element)
    }

    return mainBuilder
        .primaryConstructor(primaryConstructor.build())
        .build()
}

private fun addColorElement(
    builder: TypeSpec.Builder,
    constructorBuilder: FunSpec.Builder,
    name: String,
    element: ColorTreeElement
) {
    // Add to constructor
    when (element) {
        is ColorTreeElement.Color -> {
            builder.addProperty(
                PropertySpec.builder(
                    name,
                    ClassName("androidx.compose.ui.graphics", "Color")
                ).initializer(name).build()
            )
            constructorBuilder.addParameter(
                name,
                ClassName("androidx.compose.ui.graphics", "Color")
            )
        }

        is ColorTreeElement.Collection -> {
            @Suppress("Deprecated")
            val innerBuilder = TypeSpec.classBuilder(name.capitalize())
                .addModifiers(KModifier.DATA)
            val innerConstructor = FunSpec.constructorBuilder()

            element.map.forEach { (innerName, innerElement) ->
                addColorElement(innerBuilder, innerConstructor, innerName, innerElement)
            }

            val assembledClass = innerBuilder
                .primaryConstructor(innerConstructor.build())
                .build()


            builder.addProperty(
                PropertySpec.builder(
                    name,
                    ClassName("", assembledClass.name.toString())
                ).initializer(name).build()
            )
            constructorBuilder.addParameter(
                name,
                ClassName("", assembledClass.name.toString())
            )

            builder.addType(assembledClass)
        }
    }
}