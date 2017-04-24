<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Biometrics Enrollment</title>
<script type="text/javascript">
	function homeButton() {
		window.location.href = "../Y4J";
		window.location.refresh();
	}
</script>

</head>
<body>
	<form action="<%=request.getContextPath()%>/rest/y4j/takeBiometrics"
		method=post>
		<h2>Please enter details</h2>
		<label id="userIdLbl" style="margin-right: 20px;"><b>Aadhaar Number:</b></label> 
		<input type="text" name="userId" id="userId"> <br /><br/>
		<label id="userNameLbl" name="userName"><b>Candidate Name:</b></label> 
		<input type="text" name="userName" id="userName" style="margin-left:25px;"> 
		<br /><br/> 
			<label id="fingerIdLbl" style="margin-right: 5px;"><b>Finger ID:</b></label> 
			<select id="fingerIdOpt" name="fingerIdOpt" style="margin-left:70px;">
			<option value="0" label="Select Finger">Select Finger</option>
			<optgroup label="Right Hand">Right Hand
				<option value="1" label="Thumb">Thumb</option>
				<option value="2" label="Index">Index</option>
			</optgroup>
			<optgroup label="Left Hand">Left Hand
				<option value="6" label="Thumb">Thumb</option>
				<option value="7" label="Index">Index</option>
			</optgroup>
		</select> <br /> <br />
		<button type="submit" title="Biometric">Enroll</button>
		<button type="button" title="Home" onclick="homeButton();"
			style="margin-left: 10px;">Home</button>
	</form>
</body>
</html>
