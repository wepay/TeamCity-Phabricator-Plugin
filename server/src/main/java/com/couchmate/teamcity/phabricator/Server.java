package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.conduit.*;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends BuildServerAdapter {

    private Map<String, List<STestRun>> tests = new HashMap<>();
    private Collection<SBuildFeatureDescriptor> buildFeatures = null;
    private PhabLogger logger;

    public Server(
            @NotNull final EventDispatcher<BuildServerListener> buildServerListener,
            @NotNull final PhabLogger logger
    ) {
        buildServerListener.addListener(this);
        this.logger = logger;
    }

    @Override
    public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
        super.buildTypeAddedToQueue(queuedBuild);
        Collection<SBuildFeatureDescriptor> phabBuildFeature = queuedBuild.getBuildType().getBuildFeaturesOfType("phabricator");
        if (!phabBuildFeature.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.putAll(queuedBuild.getBuildPromotion().getParameters());
            params.putAll(phabBuildFeature.iterator().next().getParameters());
            AppConfig appConfig = new AppConfig();
            appConfig.setParams(params);
            appConfig.parse();
            ConduitClient conduitClient = new ConduitClient(appConfig.getPhabricatorUrl(), appConfig.getPhabricatorProtocol(), appConfig.getConduitToken(), this.logger);
            conduitClient.submitHarbormasterMessage(appConfig.getHarbormasterTargetPHID(), "work");
            conduitClient.submitDifferentialComment(appConfig.getRevisionId(), "Build added to queue");
        }
    }

    @Override
    public void buildStarted(@NotNull SRunningBuild runningBuild) {
        super.buildStarted(runningBuild);
        this.buildFeatures = runningBuild.getBuildFeaturesOfType("phabricator");
        if (!this.buildFeatures.isEmpty()) {
            try {
                new Thread(new BuildTracker(runningBuild)).start();
            }
            catch(Exception e) {
                Loggers.SERVER.error("Exception thrown by BuildTracker e = " + e.getMessage());
            }
        }
    }
}
