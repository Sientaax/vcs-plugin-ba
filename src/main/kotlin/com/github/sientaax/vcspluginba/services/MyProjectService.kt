package com.github.sientaax.vcspluginba.services

import com.intellij.openapi.project.Project
import com.github.sientaax.vcspluginba.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
