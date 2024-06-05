package org.example

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.State
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import androidx.compose.ui.graphics.Color as ComposeColor
import java.awt.Color
import java.io.File

fun generateAnimatePalletFile(
    outputFile: File,
    mode: VariableMode,
    palletName: String,
    packageName: String
) {
    val fileCode = FileSpec.builder(packageName, outputFile.nameWithoutExtension)
        .addAnimatedUtilFunction()
        .addFunction(generatePalletFunction(mode, packageName, palletName))
        // Workaround for alias conflict bug
        .addAliasedImport(
            ClassName("$packageName.$palletName.Surface.Fade", "TransparentBlack"),
            "FadeTransparentBlack"
        )
        .addAliasedImport(
            ClassName("$packageName.$palletName.Surface.Fade", "TransparentWhite"),
            "FadeTransparentWhite"
        )
        .build()
        .toString()
    outputFile.writeText(fileCode)
}

private fun FileSpec.Builder.addAnimatedUtilFunction(): FileSpec.Builder {
    val durationSpec = PropertySpec.builder("ANIMATION_DURATION_MS", INT, KModifier.PRIVATE, KModifier.CONST)
        .initializer("750")
        .build()
    addProperty(durationSpec)
    val animationSpec = PropertySpec.builder(
        "animateColor",
        AnimationSpec::class.parameterizedBy(ComposeColor::class),
        KModifier.PRIVATE
    ).initializer(
        CodeBlock.of(
            "%N(%N)",
            MemberName("androidx.compose.animation.core", "tween", isExtension = true),
            durationSpec
        )
    ).build()
    addProperty(animationSpec)
    val animateColorSpec = FunSpec.builder("animateColor")
        .addModifiers(KModifier.PRIVATE)
        .addParameter(
            ParameterSpec.builder(
                name = "targetValue",
                type = ComposeColor::class
            ).build()
        ).returns(
            State::class.parameterizedBy(ComposeColor::class),
        )
        .addCode(
            CodeBlock.of(
                "return %N(targetValue = targetValue, animationSpec = animationSpec)",
                MemberName(
                    packageName = "androidx.compose.animation",
                    simpleName = "animateColorAsState",
                    isExtension = true
                )
            )
        )
        .build()
    addFunction(animateColorSpec)
    return this
}

private fun generatePalletFunction(mode: VariableMode, packageName: String, palletName: String): FunSpec {
    val mainBuilder = FunSpec.builder("animatePallet")
        .addModifiers(KModifier.INTERNAL)
        .receiver(ClassName(packageName, palletName))
        .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
        .returns(ClassName(packageName, palletName))
        .addKdoc(CodeBlock.of("Autogenerated code from https://github.com/LionZXY/FlipperPalletGenerator/"))

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
            "$name = animatedColor()",
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