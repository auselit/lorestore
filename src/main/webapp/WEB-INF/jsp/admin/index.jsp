<%@ include file="/WEB-INF/jsp/oreInclude.jsp" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>LoreStore Server</title>
  </head>
  <body>
  	<h1>LoreStore Administration</h1>
  	<p>${someText}</p>
  	<ul>
  		<li><a href="import">Import Data</a></li>
  		<li><a href="stats">Statistics</a></li>
  		<li><a href="wipeDatabase">Wipe Database</a></li>
  		<li><a href="testQueries">Test Queries</a></li>
  		<li><a href="sparqlPage">SPARQL Query</a></li>
  	</ul>
  	
  </body>
</html>