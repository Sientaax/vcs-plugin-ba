package com.github.sientaax.vcspluginba.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main implements ProjectManagerListener {

    private static Server server;
    private static Path projectPath;
    private static boolean statusChecker = true;
    private Process assistantProcess;

    @Override
    public void projectOpened(@NotNull Project project) {
        projectPath = Path.of(Objects.requireNonNull(project.getBasePath()));
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        startServer();
        startAssistant();
        initCommitNotifier();
    }

    private void startServer(){
        server = new Server(80);
        server.start();
    }

    private void startAssistant() {
        /**try {
            Path path = PluginManagerCore.getPlugin(PluginId.getId("com.github.sientaax.vcspluginba")).getPluginPath();
            String pathToString = path.toString().substring(0, path.toString().lastIndexOf("build"));
            Path assistantPathBuilder = Path.of(pathToString + "build\\resources\\main\\win-unpacked\\vcs-assistant-ba.exe");
            Path assistantPathDirectory = Path.of(pathToString + "build\\resources\\main\\win-unpacked");

            ProcessBuilder processBuilder = new ProcessBuilder(String.valueOf(assistantPathBuilder));
            processBuilder.directory(new File(String.valueOf(assistantPathDirectory)));
            assistantProcess = processBuilder.start();
        } catch(IOException e){
            e.printStackTrace();
        }*/

        File userDir = new File(System.getProperty("user.home"));
        //File dataPath = new File(userDir, ".VersionBuddy-Plugin");
        Path pathBuilder = Path.of(userDir + "\\.VersionBuddy-Plugin\\win-unpacked\\vcs-assistant-ba.exe");
        Path pathDirectory = Path.of(userDir + "\\.VersionBuddy-Plugin\\win-unpacked");
        /**if(!dataPath.exists()){
            try {
                System.out.println("DataPath not existing");
                dataPath.mkdir();
                String fileId = "1iGKeZLNOPDXejVUzrNkgPww_jAlmJ4yK";
                OutputStream outputStream = new FileOutputStream(userDir + "\\.VersionBuddy-Plugin\\downloadedfile.pdf");
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                outputStream.flush();
                outputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        } else {*/
            try {
                System.out.println("DataPath is existing");
                ProcessBuilder processBuilder = new ProcessBuilder(String.valueOf(pathBuilder));
                processBuilder.directory(new File(String.valueOf(pathDirectory)));
                assistantProcess = processBuilder.start();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    //}

    private void initCommitNotifier() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                server.sendMessage(CreateJson.createJsonWorkingTime("countWorkingTime").toString());
                getLogData();
            }
        }, new Date(), 5000);
    }

    private void getLogData(){
        try {
            Git git = Git.init().setDirectory(new File(String.valueOf(projectPath))).call();

            List<String> messageLog = new ArrayList<>();
            Iterable<RevCommit> commitMessageLog = git.log().call();
            commitMessageLog.forEach(i -> messageLog.add(i.getFullMessage()));

            List<String> dateLog = new ArrayList<>();
            Iterable<RevCommit> commitDateLog = git.log().call();
            commitDateLog.forEach(i -> dateLog.add(String.valueOf(i.getAuthorIdent().getWhen())));

            server.sendMessage(CreateJson.createJsonRefresh(messageLog, dateLog).toString());

            int counter = 0;
            for(int i = 0; i < messageLog.size(); i++){
                if(!messageLog.get(i).equals("interimCommit")){
                    counter++;
                }
            }
            server.sendMessage(CreateJson.createJsonLogCounter(String.valueOf(counter)).toString());

            if(statusChecker) {
                try {
                    Status status = git.status().call();
                    for (String added : status.getUntracked()) {
                        if (!added.isEmpty()) {
                            statusChecker = false;
                            server.sendMessage(CreateJson.createJsonFileObserverNewFile("createNewFile").toString());
                        }
                    }
                    for (String deleted : status.getRemoved()) {
                        if (!deleted.isEmpty()) {
                            statusChecker = false;
                            server.sendMessage(CreateJson.createJsonFileObserverDeleteFile("deleteAFile").toString());
                        }
                    }
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            }
        } catch(GitAPIException e){
            e.printStackTrace();
        }
    }

    public static void receivedMessage(String message) {
        ParseJson parseJson = new ParseJson(message);
        try{
            Git git = Git.init().setDirectory(new File(String.valueOf(projectPath))).call();
            if(parseJson.getType().equals("commitMessage")){
                Ref ref = git.getRepository().exactRef("refs/heads/" +parseJson.getData());
                if(ref == null) {
                    statusChecker = true;
                    git.add().addFilepattern(".").call();
                    git.commit().setMessage(parseJson.getData()).call();
                    git.tag().setName(parseJson.getData()).call();
                    String startPoint = "refs/tags/" + parseJson.getData();
                    git.checkout().setCreateBranch(true).setName(parseJson.getData()).setStartPoint(startPoint).call();
                } else {
                    server.sendMessage(CreateJson.createJsonBranchExists("branchExists").toString());
                }
            } else if(parseJson.getType().equals("loadBranch")){
                git.add().addFilepattern(".").call();
                git.commit().setMessage("interimCommit").call();
                git.checkout().setName(parseJson.getData()).call();
            } else if(parseJson.getType().equals("loadBranchMaster")){
                git.checkout().setName(parseJson.getData()).call();
            } else if(parseJson.getType().equals("continueWorking")){
                git.checkout().setName(parseJson.getData()).call();
            }
        } catch (GitAPIException | IOException e){
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