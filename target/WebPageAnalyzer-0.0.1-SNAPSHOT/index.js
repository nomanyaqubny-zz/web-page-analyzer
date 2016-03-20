function getAnalysisReport() {
	var input = document.getElementById("searchfield").value;
	if (input) {
		var params = "?url="+input;
		document.getElementById("report").innerHTML = '<img src="./images/squares.gif">';
		xhrGet("analyze"+params, function(response) {
			var responseObj = parseJson(response);
			if(typeof responseObj === 'object') {
				var table = getAnalysisReportHtml(responseObj);
				document.getElementById("report").innerHTML = table;
			} else {
				displayError(response);
			}
		}, function(err) {
			displayError(err);
		});
	} else {
		displayError("Url cannot be empty.");
	}
}

function displayError(err) {
	document.getElementById("report").innerHTML = "<span class='red err'>"+err+"</span>";
}

function getAnalysisReportHtml(jsonObject) {
	var table = "<table>" +
			"<tr><td class=left> HTML -version" + 
			"</td><td>" + getDocType(jsonObject) +
			"</td></tr>" +
			"<tr><td class=left> Title" +
			"</td><td>" + getTitle(jsonObject) +
			"</td></tr>" +
			"<tr><td class=left> Headings" +
			"</td><td>" + getHeadingsDetail(jsonObject.headings) +
			"</td></tr>" +
			"<tr><td class=left> Links" +
			"</td><td>" + getLinksDetail(jsonObject.links) +
			"</td></tr>" +
			"<tr><td class=left> Is Login Page?" + 
			"</td><td>" + getIsLoginPage(jsonObject) +
			"</td></tr>" +
			"</table>";
	return table;
}

function getDocType(obj) {
	return (obj.docType) ? obj.docType : "No Information Found";
}

function getTitle(obj) {
	return (obj.title) ? obj.title : "No Information Found";
}

function getIsLoginPage(obj) {
	return (obj.hasLoginForm) ? "Yes" : "No";
}

function getHeadingsDetail(headings) {
	var headingsHtml = "";
	if(headings) {
		Object.keys(headings)
	    .sort()
	    .forEach(function(tag, i) {
	    	headingsHtml += tag + ":<span class=green>" + headings[tag] + "</span>";
	    	// as h6 is the max heading so 0-5
	    	headingsHtml += (i<5) ? ", " : "";
	     });
		
	}
	return (headingsHtml) ? headingsHtml : "No Information Found";
}

function getLinksDetail(links) {
	var linksHtml = "";
	if (links) {
		linksHtml += "Internal: <span class=green title=Accessible Links>"+links.internal.reachable +"</span>:" +
			"<span class=red title=Inaccessible Links>"+links.internal.notReachable+"</span>, " +
			" External: <span class=green title=Accessible Links>"+links.external.reachable+"</span>:" +
					"<span class=red title=Inaccessible Links>"+links.external.notReachable+"</span>";
	}
	return (linksHtml) ? linksHtml : "No Information Found";
}

//utilities
function createXHR(){
	if(typeof XMLHttpRequest != 'undefined'){
		return new XMLHttpRequest();
	}else{
		try{
			return new ActiveXObject('Msxml2.XMLHTTP');
		}catch(e){
			try{
				return new ActiveXObject('Microsoft.XMLHTTP');
			}catch(e){}
		}
	}
	return null;
}

function xhrGet(url, callback, errback) {
	var xhr = new createXHR();
	xhr.open("GET", url, true);
	xhr.onreadystatechange = function() {
		if(xhr.readyState == 4){
			if(xhr.status == 200){
				callback(xhr.responseText);
			} else {
				errback(xhr.status + ": " +xhr.responseText);
			}
		}
	};
	xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	//xhr.timeout = 3000;
	xhr.ontimeout = errback;
	xhr.send();
}

function parseJson(str) {
	return (window.JSON && isJson(str)) ? JSON.parse(str) : str;
}

function isJson(str) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}