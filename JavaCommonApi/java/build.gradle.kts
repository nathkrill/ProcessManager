/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

plugins {
    `java-library`
//    id("com.android.library")
    id("kotlin-platform-jvm")
}

val `kotlin_version`: String by rootProject
val kotlinVersion get() = `kotlin_version`

base {
    archivesBaseName = "JavaCommonApi"
}

/*
android {
    compileSdkVersion(27)
}
*/

dependencies {
    expectedBy(project(":JavaCommonApi:common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation(project(":multiplatform:java"))
}
