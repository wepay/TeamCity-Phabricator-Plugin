package com.couchmate.teamcity.phabricator;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import com.couchmate.teamcity.phabricator.HttpClient;
import com.couchmate.teamcity.phabricator.HttpRequestBuilder;
import com.couchmate.teamcity.phabricator.conduit.*;
import jetbrains.buildServer.messages.Status;
import com.couchmate.teamcity.phabricator.PhabLogger;
import jetbrains.buildServer.tests.TestInfo;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildTracker {

    private SRunningBuild build;
    private AppConfig appConfig;
    private Map<String, STest> tests;
    private ConduitClient conduitClient = null;
    private PhabLogger logger;

    public BuildTracker(SRunningBuild build, PhabLogger logger) {
        this.build = build;
        this.appConfig = new AppConfig();
        this.logger = logger;
        this.tests = new HashMap<>();
        Loggers.SERVER.info("Tracking build" + build.getBuildNumber());
    }

    public void run() {
        if (!appConfig.isEnabled()) {
            try {
                Map<String, String> params = new HashMap<>();
                //params.putAll(this.build.getBuildOwnParameters());
                params.putAll(this.build.getValueResolver().resolve(this.build.getBuildPromotion().getParameters()));
                params.putAll(this.build.getBuildFeaturesOfType("phabricator").iterator().next().getParameters());
                for (String param : params.keySet()) {
                    if (param != null) {
                        Loggers.AGENT.info(String.format("Found %s", param));
                    }
                }
                this.appConfig.setParams(params);
                this.appConfig.parse();
                this.conduitClient = new ConduitClient(this.appConfig.getPhabricatorUrl(), this.appConfig.getPhabricatorProtocol(), this.appConfig.getConduitToken(), this.logger);
            } catch (Exception e) { Loggers.SERVER.error("BuildTracker Param Parse", e); }
        }
        if (appConfig.isEnabled()) {
            build.getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS)
                    .getAllTests()
                    .forEach(
                            testRun -> {
                                if(!this.tests.containsKey(testRun.getTest().getName().getAsString())) {
                                    this.tests.put(testRun.getTest().getName().getAsString(),
                                            testRun.getTest());
                                    sendTestReport(testRun.getTest().getName().getAsString(),
                                            testRun);
                                }
                            }
                    );
             String buildInfo = this.appConfig.getServerUrl() + "/viewLog.html?buildId=" + this.build.getBuildId();
             Loggers.SERVER.info("is this build successful " + this.build.getBuildStatus().isSuccessful());
             Loggers.SERVER.info("this.appConfig.getRevisionId() = " + this.appConfig.getRevisionId());
             Status status = this.build.getBuildStatus();
             if (this.appConfig.isEnabled() && this.appConfig.reportEnd()) {
                 buildInfo = this.appConfig.getServerUrl() + "/viewLog.html?buildId=" + build.getBuildId();
                 if (status.isFailed()) {
                     this.conduitClient.submitDifferentialComment(this.appConfig.getRevisionId(), "Build failed: " + buildInfo);
                     this.conduitClient.submitHarbormasterMessage(this.appConfig.getHarbormasterTargetPHID(), "fail");
                 } else if (status.isSuccessful()) {
                     this.conduitClient.submitDifferentialComment(this.appConfig.getRevisionId(), "Build successful: " + buildInfo);
                     this.conduitClient.submitHarbormasterMessage(this.appConfig.getHarbormasterTargetPHID(), "pass");
                 } else {
                     this.conduitClient.submitDifferentialComment(this.appConfig.getRevisionId(), "Build error: " + buildInfo);
                     this.conduitClient.submitHarbormasterMessage(this.appConfig.getHarbormasterTargetPHID(), "fail");
                 }
             }
             Loggers.SERVER.info(this.build.getBuildNumber() + " finished");
        }
    }

    private CloseableHttpClient createHttpClient() {
        HttpClient client = new HttpClient(true);
        return client.getCloseableHttpClient();
    }

    private void sendTestReport(String testName, STestRun test) {
        HttpRequestBuilder httpPost = new HttpRequestBuilder()
                .post()
                .setHost(this.appConfig.getPhabricatorUrl())
                .setScheme(this.appConfig.getPhabricatorProtocol())
                .setPath("/api/harbormaster.sendmessage")
                .addFormParam(new StringKeyValue("api.token", this.appConfig.getConduitToken()))
                .addFormParam(new StringKeyValue("buildTargetPHID", this.appConfig.getHarbormasterTargetPHID()))
                .addFormParam(new StringKeyValue("type", "work"))
                .addFormParam(new StringKeyValue("unit[0][name]", test.getTest().getName().getTestMethodName()))
                .addFormParam(new StringKeyValue("unit[0][namespace]", test.getTest().getName().getClassName()));

        if (test.getStatus().isSuccessful()) {
            httpPost.addFormParam(new StringKeyValue("unit[0][result]", "pass"));
        } else if (test.getStatus().isFailed()) {
            httpPost.addFormParam(new StringKeyValue("unit[0][result]", "fail"));
        }
        try (CloseableHttpResponse response = createHttpClient().execute(httpPost.build())) {
            Loggers.SERVER.warn(String.format("Test Response: %s\nTest Body: %s\n",
                    response.getStatusLine().getStatusCode(),
                    IOUtils.toString(response.getEntity().getContent())));
        } catch (Exception e) { Loggers.SERVER.error("Send error", e); }
    }
}
