/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.adapter.pdfium.document"
}

dependencies {
    api(project(":readium:readium-shared"))
//    implementation(project(":readium:adapters:pdfium:readium-adapter-pdfium-common"))
//    implementation(files("../libs/pdfium-android-1.8.2.jar"))
    implementation("com.github.barteksc:pdfium-android:1.9.0")
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
}
