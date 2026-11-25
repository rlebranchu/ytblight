import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Accès au catalogue `gradle/libs.versions.toml` depuis les plugins de convention. */
internal val Project.catalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.versionOf(alias: String): String = catalog.findVersion(alias).get().requiredVersion

internal fun Project.intVersionOf(alias: String): Int = versionOf(alias).toInt()
