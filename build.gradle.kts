/*
 * Copyright (c) 2021.
 * 作者: AdorableParker
 * 最后编辑于: 2021/5/2 下午1:55
 */

val miraiVersion = "2.6.2" // Modify here

plugins {
    val miraiVersion = "2.6.2" // Modify here

    val kotlinVersion = "1.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version miraiVersion  // mirai-console version
}


mirai {
    coreVersion = miraiVersion // mirai-core version
}

group = "org.navigator-TB"
version = "0.3.5"

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
    compileOnly("net.mamoe:mirai-core:$miraiVersion") // mirai-core 的 API
    compileOnly("net.mamoe:mirai-console:$miraiVersion") // 后端

    implementation("com.beust:klaxon:5.4")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("com.mayabot.mynlp:mynlp-all:3.2.1")
    implementation(kotlin("stdlib-jdk8"))

//    testImplementation("net.mamoe:mirai-console-terminal:2.0.0") // 前端, 用于启动测试

}