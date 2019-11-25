package projects

import Gradle_Check.model.GradleBuildBucketProvider
import configurations.StagePasses
import jetbrains.buildServer.configs.kotlin.v2018_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2018_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2018_2.projectFeatures.versionedSettings
import model.CIBuildModel
import model.Stage

class RootProject(model: CIBuildModel, gradleBuildBucketProvider: GradleBuildBucketProvider) : Project({
    uuid = model.projectPrefix.removeSuffix("_")
    id = AbsoluteId(uuid)
    parentId = AbsoluteId("Gradle")
    name = model.rootProjectName

    features {
        versionedSettings {
            id = "PROJECT_EXT_3"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_SETTINGS_FROM_VCS
            rootExtId = "Gradle_Branches_VersionedSettings"
            showChanges = true
            settingsFormat = VersionedSettings.Format.KOTLIN
            param("credentialsStorageType", "credentialsJSON")
        }
    }

    params {
        password("teamcity.user.bot-gradle.token", "credentialsJSON:3c4f2642-d985-4d9d-bb10-f1bd1214a0a7", display = ParameterDisplay.HIDDEN)
    }

    var prevStage: Stage? = null
//    val deferredFunctionalTests = mutableListOf<(Stage) -> List<FunctionalTest>>()
    model.stages.forEach { stage ->
        val stageProject = StageProject(model, gradleBuildBucketProvider, stage, uuid)
        val stagePasses = StagePasses(model, stage, prevStage, stageProject)
        buildType(stagePasses)
        subProject(stageProject)
        prevStage = stage
    }

    if (model.stages.map { stage -> stage.performanceTests }.flatten().isNotEmpty()) {
        subProject(WorkersProject(model))
    }

    buildTypesOrder = buildTypes
    subProjectsOrder = subProjects
})
