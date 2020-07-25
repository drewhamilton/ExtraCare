package dev.drewhamilton.extracare

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@AutoService(CommandLineProcessor::class)
class ExtraCareCommandLineProcessor : CommandLineProcessor {

    // TODO: Shared with Gradle plugin; centralize
    override val pluginId = "extracare-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(Options.ENABLED, "<true|false>", "", required = true)
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        Options.ENABLED -> configuration.put(KEY_ENABLED, value.toBoolean())
        else -> error("Unknown plugin option: ${option.optionName}")
    }

    private object Options {
        const val ENABLED = "enabled"
    }

    companion object {
        internal val KEY_ENABLED = CompilerConfigurationKey<Boolean>(Options.ENABLED)
    }
}
