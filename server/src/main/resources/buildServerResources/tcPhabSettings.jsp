<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="requestUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="buildTypeId" type="java.lang.String" scope="request"/>

<c:set var="phabricatorUrl" value="${propertiesBean.properties['tcphab.phabricatorUrl']}" />
<c:set var="conduitToken" value="${propertiesBean.properties['tcphab.conduitToken']}" />
<c:set var="pathToArc" value="${propertiesBean.properties['tcphab.pathToArch']}" />
<c:set var="pathToCode" value="${propertiesBean.properties['tcphab.pathToCode']}" />
<c:set var="errorInfo" value="${propertiesBean.properties['tcphab.errorInfo']}" />
<c:set var="patch" value="${propertiesBean.properties['tcphab.patch']}" />
<c:set var="reportBegin" value="${propertiesBean.properties['tcphab.reportBegin']}" />
<c:set var="reportEnd" value="${propertiesBean.properties['tcphab.reportEnd']}" />
<tr><td colspan="2">Report build status in real-time to your Phabricator instance.</td></tr>
<tr><th>Phabricator URL:</th><td><props:textProperty name="tcphab.phabricatorUrl"/></td></tr>
<tr><th>Conduit Token:</th><td><props:textProperty name="tcphab.conduitToken"/></td></tr>
<tr><th>Path To Arcanist:</th><td><props:textProperty name="tcphab.pathToArc"/></td></tr>
<tr><th>Path To Checked Out Code:</th><td><props:textProperty name="tcphab.pathToCode"/></td></tr>
<tr><th>Phabricator Error Message</th><td><props:textProperty name="tcphab.errorInfo"/></td></tr>
<tr><th>Patch</th><td><props:checkboxProperty name="tcphab.patch" uncheckedValue="false" value="true"/></td></tr>
<tr><th>Report Begin</th><td><props:checkboxProperty name="tcphab.reportBegin" uncheckedValue="false" value="true"/></td></tr>
<tr><th>Report End</th><td><props:checkboxProperty name="tcphab.reportEnd" uncheckedValue="false" value="true"/></td></tr>
