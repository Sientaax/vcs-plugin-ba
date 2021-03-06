package com.github.sientaax.vcspluginba.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.Messages;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;

public class Main implements ProjectManagerListener {

    private static Server server;
    private static Path projectPath;
    private static Git git;
    private static boolean statusChecker = true;
    private static Logging logging;
    private static Timestamp timestamp;
    private Process assistantProcess;
    private Project project_;
    private int counterToLog = 0;
    private String userName;

    @Override
    public void projectOpened(@NotNull Project project) {
        project_ = project;
        projectPath = Path.of(Objects.requireNonNull(project.getBasePath()));
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        userName = getMacAdresse();
        logging = new Logging(userName);
        timestamp = new Timestamp(System.currentTimeMillis());
        logging.appendToFile("ProjectOpened:\t" + timestamp + "\n" );
        startServer();
        startAssistant();
        initGitProject();
        initCommitNotifier();
    }

    private String getMacAdresse(){
        byte [] mac = new byte[0];
        try{
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            mac = network.getHardwareAddress();
        } catch (UnknownHostException | SocketException e){
            e.printStackTrace();
        }
        String userName = Arrays.toString(mac);
        userName = userName.replaceAll("[^0-9]", "");
        userName = userName.replaceAll(" ", "_");
        return userName;
    }

    private void startServer(){
        server = new Server(80);
        server.start();
    }

    private void startAssistant() {
        File dataPath = new File(System.getProperty("user.home") + "\\.VersionBuddy-Plugin\\win-unpacked\\vcs-assistant-ba.exe");
        File userDir = new File(System.getProperty("user.home"));
        Path pathBuilder = Path.of(userDir + "\\.VersionBuddy-Plugin\\win-unpacked\\vcs-assistant-ba.exe");
        Path pathDirectory = Path.of(userDir + "\\.VersionBuddy-Plugin\\win-unpacked");
        String message = "Der Assistent ist nicht richtig installiert.\n" +
                "Bitte kontrolliere nochmals anhand dem Cheatsheet zur Installation des Prototypen, ob alles richtig gemacht wurde.\n" +
                "Starte im Anschluss IntelliJ erneut.";
        String title = "Assistent nicht gefunden";
        if(!dataPath.exists()){
            Messages.showMessageDialog(project_, message, title, Messages.getInformationIcon());
        } else {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(String.valueOf(pathBuilder));
                processBuilder.directory(new File(String.valueOf(pathDirectory)));
                assistantProcess = processBuilder.start();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void initGitProject(){
        try {
            git = Git.init().setDirectory(new File(String.valueOf(projectPath))).call();
            git.add().addFilepattern(".").call();
            git.commit().setMessage("interimCommit").call();
        } catch (GitAPIException e){
            e.printStackTrace();
        }
    }

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

            counterToLog++;
            if(counterToLog == 179){
                counterToLog = 0;
                timestamp = new Timestamp(System.currentTimeMillis());
                logging.appendToFile("15 minutes over:\t" + timestamp + "\n" );
            }

            if(statusChecker) {
                try {
                    Status status = git.status().call();
                    for (String added : status.getUntracked()) {
                        if (!added.isEmpty() && !added.matches("\\.idea.*") && !added.matches(".*(\\.iml)")) {
                            statusChecker = false;
                            server.sendMessage(CreateJson.createJsonFileObserverNewFile("createNewFile").toString());
                            timestamp = new Timestamp(System.currentTimeMillis());
                            logging.appendToFile("Added:\t" + timestamp + "\n" );
                        }
                    }
                    for (String deleted : status.getRemoved()) {
                        if (!deleted.isEmpty()) {
                            statusChecker = false;
                            server.sendMessage(CreateJson.createJsonFileObserverDeleteFile("deleteAFile").toString());
                            timestamp = new Timestamp(System.currentTimeMillis());
                            logging.appendToFile("Deleted:\t" + timestamp + "\n" );
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
            if(parseJson.getType().equals("commitMessage")){
                Ref ref = git.getRepository().exactRef("refs/heads/" +parseJson.getData());
                if(ref == null) {
                    statusChecker = true;
                    git.add().addFilepattern(".").call();
                    git.commit().setMessage(parseJson.getData()).call();
                    git.tag().setName(parseJson.getData()).call();
                    String startPoint = "refs/tags/" + parseJson.getData();
                    git.checkout().setCreateBranch(true).setName(parseJson.getData()).setStartPoint(startPoint).call();
                    timestamp = new Timestamp(System.currentTimeMillis());
                    logging.appendToFile("Commit created: " + parseJson.getData() + "\t" + timestamp + "\n");
                } else {
                    server.sendMessage(CreateJson.createJsonBranchExists("branchExists").toString());
                }
            } else if(parseJson.getType().equals("loadBranch")){
                git.add().addFilepattern(".").call();
                git.commit().setMessage("interimCommit").call();
                git.checkout().setName(parseJson.getData()).call();
                timestamp = new Timestamp(System.currentTimeMillis());
                logging.appendToFile("LoadBranch: " + parseJson.getData() + "\t" + timestamp + "\n");
            } else if(parseJson.getType().equals("loadBranchMaster")){
                git.add().addFilepattern(".").call();
                git.commit().setMessage("interimCommit").call();
                git.checkout().setName(parseJson.getData()).call();
                server.sendMessage(CreateJson.createJsonLoadBranchMaster("loadBranch").toString());
                timestamp = new Timestamp(System.currentTimeMillis());
                logging.appendToFile("Load Master: " + parseJson.getData() + "\t" + timestamp + "\n");
            } else if(parseJson.getType().equals("continueWorking")){
                git.add().addFilepattern(".").call();
                git.commit().setMessage("interimCommit").call();
                git.checkout().setName(parseJson.getData()).call();
                timestamp = new Timestamp(System.currentTimeMillis());
                logging.appendToFile("Continue Working: " + parseJson.getData() + "\t" + timestamp + "\n");
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
        timestamp = new Timestamp(System.currentTimeMillis());
        logging.appendToFile("Project Closed:\t" + timestamp + "\n" );
        server.shutdown();
        assistantProcess.destroy();
        logging.close();
    }
}