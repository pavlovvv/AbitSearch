// Сервер AbitSearch написано на Node.js. Цей файл лише дає змогу запускати його
// з Android Studio тими самими Gradle-задачами: :server:run і :server:test.
// Потрібен Node.js 22.5+ у PATH.
val isWindows = System.getProperty("os.name").lowercase().contains("win")
fun npm(vararg args: String) = if (isWindows) listOf("cmd", "/c", "npm", *args) else listOf("npm", *args)

val npmInstall by tasks.registering(Exec::class) {
    group = "abitsearch"
    description = "Встановлює залежності сервера (npm install)"
    workingDir = projectDir
    commandLine(npm("install"))
    inputs.file("package.json")
    outputs.dir("node_modules")
}

tasks.register<Exec>("run") {
    group = "abitsearch"
    description = "Запускає сервер"
    dependsOn(npmInstall)
    workingDir = projectDir
    // node запускається напряму, а не через npm: тоді кнопка Stop в IDE зупиняє сам процес сервера.
    commandLine("node", "--no-warnings=ExperimentalWarning", "src/index.js")
}

tasks.register<Exec>("test") {
    group = "abitsearch"
    description = "Запускає тести сервера (node --test)"
    dependsOn(npmInstall)
    workingDir = projectDir
    commandLine("node", "--no-warnings=ExperimentalWarning", "--test", "test/snapshot.test.js", "test/api.test.js")
}
