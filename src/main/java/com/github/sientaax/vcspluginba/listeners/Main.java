package com.github.sientaax.vcspluginba.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class Main implements ProjectManagerListener {

    private Project project;
    private Server server;

    //public Main (Project project){
        //this.project = project;
    //}

    @Override
    public void projectOpened(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        //server = new Server(80);
        //server.start();
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        server.shutdown();
    }
}