/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/4/17 下午3:14
 */

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
rootProject.name = "mirai-console-plugin-template"