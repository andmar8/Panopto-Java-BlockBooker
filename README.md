Panopto-Java-BlockBooker
========================

A simple command line tool to take a series of schedules in a csv file and book them all in one go using the API

The code requires the [Panopto-Java-Util](https://github.com/andmar8/Panopto-Java-Util) Library

Format of the CSV
-----------------

To use the block booker you need to specify a CSV file in the following format...

<table>
<tr>
	<th>name</th>
	<th>folderExternalId</th>
	<th>start</th>
	<th>end</th>
	<th>location</th>
	<th>externalId</th>
</tr>
<tr>
	<td>COM1001/L01</td>
	<td>Q1213-COM1001</td>
	<td>02/10/2012 13:00</td>
	<td>02/10/2012 14:00</td>
	<td>BLDG.1.10</td>
	<td>#SPLUS123456</td>
</tr>
<table>

Or, as it would be in the csv....

<pre>
"name","folderExternalId","start","end","location","externalId"
"COM1001/L01","Q1213-COM1001","02/10/2012 13:00","02/10/2012 14:00","BLDG.1.10","#SPLUS123456"
</pre>