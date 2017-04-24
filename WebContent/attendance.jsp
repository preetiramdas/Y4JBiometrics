<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Attendance Sheet</title>
<script type="text/javascript" src="jquery-3.2.0.js"></script>
</head>

<body>
	<form action="<%=request.getContextPath()%>/rest/y4j/recordAttendance" method=post id="form1">
		<label id="batchCode" style="margin-right:5px;"><b>Batch Code:</b></label> 
		<select id="batchList" class="batchId">
		<%-- <optgroup>
			<option value="Select">Select Batch</option>
			<% Iterator it = batch.entrySet().iterator();
				while(it.hasNext()){
					Map.Entry pair = (Map.Entry)it.next();
				
			 %>
				<option value="<%=pair.getKey() %>" > <%=pair.getValue()%> </option>
			<%	}
			//end of for %>
			</optgroup> --%>
		</select>
		<br/>
		<br/>
		<label id="batchCode" style="margin-right: 45px;"><b>Name:</b></label> 
		<select id="batchCodeAt" name="batchCodeAt">
			<option value="Select Name">Select Name</option>
		</select> 
		<!-- <label id="batchCode" style="margin-right: 5px;">Name:</label> <input
			type="text" name="batchCodeAt" id="batchCodeAt"> 
		 -->	
		<br />
		<br />
		<button type="submit" title="Mark Attendance">Mark Attendance</button>
		<button type="button" title="Home" onclick="homeButton();" style="margin-left:10px;">Home</button>
		<div id="demo">
		
		</div>
	</form>

	<script type="text/javascript">
	var selectedBatch ="";
	function homeButton(){
		window.location.href="../Y4J";
		window.location.refresh();
	}
	
	
	
	$(document).ready(function(){
		var url = "http://66.175.212.166:8080/Y4J/rest/y4j/getBatches";
		$.get( url, function( data ) {
			 
			  document.getElementById("batchList").innerHTML = data;
			});
	});
	
	$(".batchId").on('change',function(){
		
		var batchCode = $(this).find(":selected").val();
		 var url = "http://66.175.212.166:8080/Y4J/rest/y4j/getCandidates/"+batchCode;
		 $.get( url, function( data ) {
			 
			  document.getElementById("batchCodeAt").innerHTML = data;
			});
		/* $.ajax({
		    url: url,
		    dataType: "html",
		    success: function(data) {
		    	console.log(data);
		    }
		});*/
		
		 //loadDoc(batchCode);
	});
	
	function loadDoc(name) {
		
	    var xhttp = new XMLHttpRequest();
	    xhttp.onreadystatechange = function() {
	        if (this.readyState == 4 && this.status == 200) {
	        	console.log(this.responseText);
	           /*  document.getElementById("demo").innerHTML =
	            this.responseText; */
	       }
	    };
	    var url = "http://localhost:8080/Y4J/rest/y4j/getCandidates/"+name;
	    xhttp.open("GET", url, false);
	    xhttp.send(); 
	}
	</script>
	
</body>
</html>
