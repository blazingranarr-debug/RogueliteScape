plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.fastutil)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.or2.all.cache)
    implementation(libs.or2.definition)
    implementation(libs.simmetrics.core)
    implementation(projects.api.areaChecker)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.instances)
    implementation(projects.api.realm)
    implementation(projects.api.registry)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)
    implementation(projects.content.other.commands.commandsPack)

    implementation(projects.api.utils.utilsSystem)
    implementation(projects.engine.utilsBits)

    testImplementation(libs.jackson.dataformat.toml)
}
