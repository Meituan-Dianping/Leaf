<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Leaf</title>
    <link href="/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<table class="table table-hover">
    <thead>
    <tr>
        <th>name</th>
        <th>init</th>
        <th>next</th>
        <th>pos</th>
        <th>value0</th>
        <th>max0</th>
        <th>step0</th>
        <th>value1</th>
        <th>max1</th>
        <th>step1</th>

    </tr>
    </thead>
    <tbody>
    <#if data?exists>
        <#list data?keys as key>
        <tr>
            <td>${key}</td>
            <td>${data[key].initOk?string('true','false')}</td>
            <td>${data[key].nextReady?string('true','false')}</td>
            <td>${data[key].pos}</td>
            <td>${data[key].value0}</td>
            <td>${data[key].max0}</td>
            <td>${data[key].step0}</td>
            <td>${data[key].value1}</td>
            <td>${data[key].max1}</td>
            <td>${data[key].step1}</td>
        </tr>
        <tr>
        </tr>

        </#list>
    </#if>
    <tbody>
</table>
</body>
</html>