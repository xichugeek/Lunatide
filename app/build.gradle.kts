import com.android.build.api.artifact.SingleArtifact
import javax.xml.parsers.DocumentBuilderFactory
import java.util.Properties
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.xichugeek.lunatide"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "com.xichugeek.lunatide"
        minSdk = 26
        targetSdk = 37
        versionName = "0.1.0"
        val parts = requireNotNull(versionName).split('.').map(String::toInt)
        versionCode = parts[0] * 10000 + parts[1] * 100 + parts[2]
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val signingFile = rootProject.file("keystore.properties")
    if (signingFile.exists()) {
        val signingValues = Properties().apply { signingFile.inputStream().use { load(it) } }
        signingConfigs.create("localRelease") {
            storeFile = rootProject.file(signingValues.getProperty("storeFile"))
            storePassword = signingValues.getProperty("storePassword")
            keyAlias = signingValues.getProperty("keyAlias")
            keyPassword = signingValues.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("localRelease") ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    if (!signingFile.exists()) logger.warn("【仅供本地测试】未配置正式签名，release 使用 debug 签名，禁止发布。")
    buildFeatures { compose = true; buildConfig = true }
    androidResources { localeFilters += listOf("zh", "en") }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }

abstract class GenerateChineseResources : DefaultTask() {
    @get:InputFile abstract val sourceFile: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun generate() {
        val output = outputDirectory.file("values-zh/strings.xml").get().asFile
        output.parentFile.mkdirs()
        output.writeBytes(sourceFile.get().asFile.readBytes())
    }
}
val generateChineseResources = tasks.register<GenerateChineseResources>("generateChineseResources") {
    sourceFile.set(layout.projectDirectory.file("src/main/res/values/strings.xml"))
    outputDirectory.set(layout.buildDirectory.dir("generated/chineseResources"))
}
androidComponents.onVariants { variant ->
    variant.sources.res?.addGeneratedSourceDirectory(generateChineseResources, GenerateChineseResources::outputDirectory)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore)
    implementation(libs.sqlcipher)
    implementation(libs.coroutines)
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.ext)
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

val allowedPermissions = setOf(
    "android.permission.POST_NOTIFICATIONS", "android.permission.USE_BIOMETRIC",
    "android.permission.USE_FINGERPRINT", "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.WAKE_LOCK", "android.permission.FOREGROUND_SERVICE",
    "com.xichugeek.lunatide.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
)
val verifyManifestPermissions = tasks.register("verifyManifestPermissions") {
    group = "verification"
    description = "校验 debug 与 release 合并后的权限与备份防线"
}
androidComponents.onVariants { variant ->
    if (variant.name in listOf("debug", "release")) {
        val merged = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val verifyVariant = tasks.register("verify${variant.name.replaceFirstChar(Char::uppercase)}PrivacyManifest") {
            inputs.file(merged)
            doLast {
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }
                val manifest = factory.newDocumentBuilder().parse(merged.get().asFile)
                val ns = "http://schemas.android.com/apk/res/android"
                listOf("uses-permission", "uses-permission-sdk-23").forEach { tag ->
                    val nodes = manifest.getElementsByTagName(tag)
                    for (i in 0 until nodes.length) {
                        val name = (nodes.item(i) as org.w3c.dom.Element).getAttributeNS(ns, "name")
                        check(name in allowedPermissions) { "隐私检查失败：${variant.name} 出现未授权权限 $name" }
                    }
                }
                val app = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element
                check(app.getAttributeNS(ns, "allowBackup") == "false") { "隐私检查失败：必须禁用系统备份" }
                check(app.getAttributeNS(ns, "fullBackupContent") == "false") { "隐私检查失败：必须禁用旧版备份" }
                check(app.getAttributeNS(ns, "dataExtractionRules") == "@xml/data_extraction_rules") { "隐私检查失败：缺少数据提取规则" }
                check(app.getAttributeNS(ns, "usesCleartextTraffic") != "true") { "隐私检查失败：不允许明文通信" }
            }
        }
        verifyManifestPermissions.configure { dependsOn(verifyVariant) }
    }
}
val verifyDependencies = tasks.register("verifyDependencies") {
    group = "verification"
    doLast {
        val forbidden = listOf(
            "com.google.firebase", "com.google.android.gms", "com.google.android.play", "com.crashlytics",
            "com.squareup.okhttp", "com.squareup.retrofit2", "io.ktor", "com.android.volley", "com.umeng",
            "com.tencent", "com.baidu", "com.bytedance", "com.pangle", "com.facebook", "io.sentry",
            "com.appsflyer", "com.adjust", "cn.jiguang", "cn.jpush", "com.getui", "com.igexin",
            "com.huawei.hms", "com.xiaomi", "com.heytap", "com.vivo",
        )
        val resolved = configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
        val coordinates = resolved.map { it.moduleVersion.id }.distinct().sortedBy { it.toString() }
        val report = layout.buildDirectory.file("reports/privacy/dependencies.txt").get().asFile
        report.parentFile.mkdirs()
        report.writeText(coordinates.joinToString("\n", postfix = "\n"))
        coordinates.forEach { id ->
            check(forbidden.none { id.group.startsWith(it) }) { "隐私检查失败：禁止依赖 $id" }
            val allowed = id.group.startsWith("androidx.") || id.group.startsWith("org.jetbrains.kotlin") ||
                id.group in setOf("com.google.dagger", "net.zetetic", "com.google.android.material")
            check(allowed || "${id.group}:${id.name}" in setOf(
                "javax.inject:javax.inject", "jakarta.inject:jakarta.inject-api",
                "org.jspecify:jspecify", "org.jetbrains:annotations",
                "com.google.code.findbugs:jsr305", "com.google.guava:listenablefuture",
                "com.squareup.okio:okio", "com.squareup.okio:okio-jvm",
            )) {
                "隐私检查失败：不在依赖白名单 $id，请审查并记录 DECISIONS.md"
            }
        }
    }
}
val verifySourceForbiddenApis = tasks.register("verifySourceForbiddenApis") {
    group = "verification"
    doLast {
        val forbidden = listOf("java.net.", "HttpURLConnection", "okhttp", "retrofit", "io.ktor", "WebView", "DownloadManager", "Settings.Secure.ANDROID_ID", "getDeviceId", "AdvertisingIdClient", "getImei")
        val exceptions = file("privacy-allowlist.txt").readLines().filter { it.isNotBlank() && !it.startsWith('#') }.toSet()
        fileTree("src/main") { include("**/*.kt", "**/*.java") }.forEach { source ->
            val path = source.relativeTo(projectDir).invariantSeparatorsPath
            if (path !in exceptions) forbidden.forEach { token ->
                check(token !in source.readText()) { "隐私检查失败：$path 使用禁止 API $token" }
            }
        }
        val rules = file("src/main/res/xml/data_extraction_rules.xml").readText()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(rules.byteInputStream())
        listOf("cloud-backup", "device-transfer").forEach { tag ->
            val exclusions = (doc.getElementsByTagName(tag).item(0) as org.w3c.dom.Element).getElementsByTagName("exclude")
            val domains = (0 until exclusions.length).map { exclusions.item(it) as org.w3c.dom.Element }
                .filter { it.getAttribute("path") == "." }.map { it.getAttribute("domain") }.toSet()
            check(domains.containsAll(setOf("root", "file", "database", "sharedpref", "external"))) { "隐私检查失败：备份排除域不完整" }
        }
    }
}
val verifyPrivacy = tasks.register("verifyPrivacy") {
    group = "verification"
    dependsOn(verifyManifestPermissions, verifyDependencies, verifySourceForbiddenApis)
}
tasks.named("check") { dependsOn(verifyPrivacy) }
