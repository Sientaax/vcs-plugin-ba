package com.github.sientaax.vcspluginba.listeners;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Main implements ProjectManagerListener {

    private static Server server;
    private Process assistantProcess;

    @Override
    public void projectOpened(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        startServer();
        //startAssistant();
        initCommitNotifier();
    }

    private void startServer(){
        server = new Server(80);
        server.start();
    }

    public static void receivedMessage(String message) {
        ParseJson parseJson = new ParseJson(message);
        try {
            //pathname muss mit user.dir ausgetauscht werden
            Git git = Git.init().setDirectory(new File("C:\\Users\\HP\\Desktop\\PluginTesterTwo\\testseven")).call();
            if(parseJson.getType().equals("commitMessage")) {
                git.add().addFilepattern(".").call();
                git.commit().setMessage(parseJson.getData()).call();

                List<String> commitMessageLog = new ArrayList<>();
                Iterable<RevCommit> commitLog = git.log().call();
                commitLog.forEach(i -> commitMessageLog.add(i.getFullMessage()));

                List<String> dateLog = new ArrayList<>();
                Iterable<RevCommit> commitLogDate = git.log().call();
                commitLogDate.forEach(i -> dateLog.add(String.valueOf(i.getAuthorIdent().getWhen())));

                server.sendMessage(CreateJson.createJsonRefresh(commitMessageLog, dateLog).toString());
            }
        } catch(GitAPIException e){
            e.printStackTrace();
        }
    }

    private void startAssistant() {
        try {
            Path path = PluginManagerCore.getPlugin(PluginId.getId("com.github.sientaax.vcspluginba")).getPluginPath();
            String pathToString = path.toString().substring(0, path.toString().lastIndexOf("build"));
            Path assistantPathBuilder = Path.of(pathToString + "build\\resources\\main\\win-unpacked\\vcs-assistant-ba.exe");
            Path assistantPathDirectory = Path.of(pathToString + "build\\resources\\main\\win-unpacked");

            ProcessBuilder processBuilder = new ProcessBuilder(String.valueOf(assistantPathBuilder));
            processBuilder.directory(new File(String.valueOf(assistantPathDirectory)));
            assistantProcess = processBuilder.start();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void initCommitNotifier(){
        //observeFiles();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run() {
                //server.sendMessage("countWorkingTime");
                server.sendMessage(CreateJson.createJsonWorkingTime("countWorkingTime").toString());
            }
        }, new Date(), 60000);
    }

    private void observeFiles(){
        try(WatchService service = FileSystems.getDefault().newWatchService()){
            Map<WatchKey, Path> keyMap = new HashMap<>();
            //pathname muss mit user.dir ausgetauscht werden + \\src
            Path path = Paths.get("C:\\Users\\HP\\Desktop\\PluginTesterTwo\\testseven");
            keyMap.put(path.register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE), path);
            WatchKey watchKey;
            do{
                watchKey = service.take();

                for(WatchEvent<?> event : watchKey.pollEvents()){
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        server.sendMessage(CreateJson.createJsonFileObserverNewFile("createNewFile").toString());
                    } else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        server.sendMessage(CreateJson.createJsonFileObserverDeleteFile("deleteAFile").toString());
                    }
                }
            } while (watchKey.reset());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        server.shutdown();
        assistantProcess.destroy();
    }
}