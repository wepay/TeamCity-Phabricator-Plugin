package com.couchmate.teamcity.phabricator;

import jetbrains.buildServer.log.Loggers;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.io.*;

public class TCServerUrl {

    private static String serverUrlPath = "/root/.BuildServer/config/main-config.xml";

    public static String getServerUrl() {
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document document = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            document = db.parse(new File(TCServerUrl.serverUrlPath));
        } catch (Exception e) {
            Loggers.SERVER.error("Could not get server url: " + e);
        }
        NodeList nodeList = document.getElementsByTagName("server");
        return nodeList.item(0).getAttributes().getNamedItem("rootURL").getNodeValue();
    }
}