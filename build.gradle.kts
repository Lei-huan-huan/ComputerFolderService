plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

kotlin {
    jvmToolchain(17)
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//
//        nativeDistributions {
//            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
//            packageName = "ComputerFolderService"
//            packageVersion = "1.0.0"
//        }
//    }
//}
compose.desktop {
    application {
        mainClass = "com.computerfolder.app.MainKt"

        nativeDistributions {
            packageName = "ComputerFolderService"
            packageVersion = "1.0.0"
            modules("jdk.httpserver")

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
            )

            macOS {
//                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
        }
    }
}
