package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.conduit.*;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends BuildServerAdapter {

    private Map<String, List<STestRun>> tests = new HashMap<>();
    private PhabLogger logger;
    private String serverUrl;

    public Server(
            @NotNull final EventDispatcher<BuildServerListener> buildServerListener,
            @NotNull final PhabLogger logger
    ) {
        buildServerListener.addListener(this);
        this.logger = logger;
        this.serverUrl = TCServerUrl.getServerUrl();
    }

    @Override
    public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
        super.buildTypeAddedToQueue(queuedBuild);
        if (!queuedBuild.getBuildType().getParameters().containsKey("serverUrl")) {
            SimpleParameter tcServerUrl = new SimpleParameter("serverUrl", this.serverUrl);
            queuedBuild.getBuildType().addParameter(tcServerUrl);
        }
        Collection<SBuildFeatureDescriptor> phabBuildFeature = queuedBuild.getBuildType().getBuildFeaturesOfType("phabricator");
        if (!phabBuildFeature.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.putAll(queuedBuild.getBuildPromotion().getParameters());
            params.putAll(phabBuildFeature.iterator().next().getParameters());
            AppConfig appConfig = new AppConfig();
            appConfig.setParams(params);
            appConfig.parse();
            queuedBuild.getBuildPromotion().setBuildComment(queuedBuild.getTriggeredBy().getUser(),
                appConfig.getPhabricatorProtocol() + "://" + appConfig.getPhabricatorUrl() + "/D" + appConfig.getRevisionId());
            if (appConfig.reportBegin()) {
                ConduitClient conduitClient = new ConduitClient(appConfig.getPhabricatorUrl(), appConfig.getPhabricatorProtocol(), appConfig.getConduitToken(), this.logger);
                conduitClient.submitHarbormasterMessage(appConfig.getHarbormasterTargetPHID(), "work");
                conduitClient.submitDifferentialComment(appConfig.getRevisionId(), "Build added to queue: " + this.serverUrl + "/viewLog.html?buildId=" + queuedBuild.getItemId());
            }
        }
    }

    @Override
    public void buildFinished(@NotNull SRunningBuild runningBuild) {
        super.buildFinished(runningBuild);
        Collection<SBuildFeatureDescriptor> buildFeatures = runningBuild.getBuildFeaturesOfType("phabricator");
        if (!buildFeatures.isEmpty()) {
            BuildTracker buildTracker = new BuildTracker(runningBuild, this.logger);
            buildTracker.run();
        }
    }
}
