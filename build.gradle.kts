/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/2/14 上午3:11
 */

plugins {
    val kotlinVersion = "1.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.3.0" // mirai-console version
}

mirai {
    coreVersion = "2.3.0" // mirai-core version
}

group = "org.example"
version = "0.1.0"

repositories {
    maven("https://dl.bintray.com/him188moe/")
    maven("https://maven.aliyun.com/nexus/content/groups/public/")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://jitpack.io")
    jcenter()
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-core:2.3.0") // mirai-core 的 API
    compileOnly("net.mamoe:mirai-console:2.3.0") // 后端

    implementation("com.beust:klaxon:5.4")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("com.mayabot.mynlp:mynlp-all:3.2.1")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("net.mamoe:mirai-console-terminal:2.0.0") // 前端, 用于启动测试

}