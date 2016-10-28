package com.couchmate.teamcity.phabricator;

import com.couchmate.teamcity.phabricator.conduit.*;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.vcs.*;
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
        Map<String, String> params = new HashMap<>();
        params.putAll(queuedBuild.getBuildPromotion().getParameters());
        Collection<SBuildFeatureDescriptor> phabBuildFeature = queuedBuild.getBuildType().getBuildFeaturesOfType("phabricator");
        if (!phabBuildFeature.isEmpty()) {
            params.putAll(phabBuildFeature.iterator().next().getParameters());
            String phabricatorProtocol = null;
            String phabricatorUrl = null;
            try {
                URL aURL = new URL(params.get("tcphab.phabricatorUrl"));
                phabricatorProtocol = aURL.getProtocol();
                phabricatorUrl = aURL.getHost();
                params.put("tcphab.phabricatorUrl", phabricatorUrl);
            } catch (IOException e) {
               Loggers.SERVER.error(String.format("phabricator url could not be parsed: %s", e.getStackTrace()[0].toString()));
            }
            ConduitClient conduitClient = new ConduitClient(params.get("tcphab.phabricatorUrl"), phabricatorProtocol, params.get("tcphab.conduitToken"), this.logger);
            conduitClient.submitHarbormasterMessage(params.get("env.harbormasterTargetPHID"), "work");
            conduitClient.submitDifferentialComment(params.get("env.revisionId"), "Build added to queue");
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
