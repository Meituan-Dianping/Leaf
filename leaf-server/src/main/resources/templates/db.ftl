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
<th>tag</th>
<th>max</th>
<th>step</th>
<th>update</th>
</tr>
</thead>
<tbody>
<#if items?exists>
<#list items as item>
<tr>
<td>${item.key}</td>
<td>${item.maxId}</td>
<td>${item.step}</td>
<td>${item.updateTime}</td>
</tr>
<tr>
</tr>

</#list>
</#if>
<tbody>
</table>
</body>
</html>