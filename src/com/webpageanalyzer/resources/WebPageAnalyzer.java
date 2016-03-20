package com.webpageanalyzer.resources;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class WebPageAnalyzer {
	
	private String url;
	private Document doc;

	public WebPageAnalyzer(String url) throws org.jsoup.HttpStatusException, java.net.SocketTimeoutException, IOException {
		this.url = url;
		doc = Jsoup.connect(this.url).get();
	}

	public JSONObject analyze() {
		JSONObject jsonObject = new JSONObject();
		System.out.println("Processing DOCTYPE..");
		jsonObject.put("docType", getHtmlVersion());
		System.out.println("Processing TITLE..");
		jsonObject.put("title", getTitle());
		System.out.println("Processing HEADINGS..");
		jsonObject.put("headings", getHeadings());
		System.out.println("Processing LINKS..");
		jsonObject.put("links", getLinks());
		System.out.println("Processing LOGINPAGE..");
		jsonObject.put("hasLoginForm", isLogInPage());
		return jsonObject;
	}
	
	/*
	 * get the very first child node of the document
	 * check if it is the instance of documentType
	 * to make sure we have the right node
	 * and then check if it has HTML 5 doctype
	 * else get the html version from the doctype
	 * regex gets the string between '//DTD ' and '//'
	 * else returns an empty string
	 */
	private String getHtmlVersion() {
		String version = "";
		Node docTypeNode = this.doc.childNodes().get(0);
		if (docTypeNode instanceof DocumentType) {
			if (docTypeNode.toString().matches("<!DOCTYPE html>")) {
				version = "HTML 5";
			} else {
				Pattern pattern = Pattern.compile("(?i)//DTD\\s([^/]+)//");
				Matcher matcher = pattern.matcher(docTypeNode.toString());
				if (matcher.find()) {
				    version = matcher.group(1);
				}
			}
		}
		return version;
	}

	/*
	 * As there is no sure way to detect the page has a login page
	 * Though we try to check different elements
	 * 
	 * Assuming the web page is in English Language
	 * 
	 * 1. check the page has a form element
	 * 2. check the form element has name/id/action contains login/signin/log in/ sign in
	 * 3. check form has Forgot password checkbox/link
	 * 4. check is there any password field
	 * 5. check is there any username or email field
	 * 6. confirm there is only one password field as signup may have one/two password fields
	 * 7. check the submit button is labeled as login/log in/sign in/ ignIn
	 */
	public Boolean isLogInPage() {
		// regex to find either string has words like log-in login /sign-in-form
		String regEx = "(?i).*(log|sign)[\\s-_]?(in).*";
		Integer chances = 0; 
		Elements forms = this.doc.select("form");
		/*
		 * loop over all form on the page
		 * until criteria to detect login form is matched
		 * or end of forms list
		 */	
		for (Element form : forms) {			
			// check if form i, class, name or action has words like login, log in, sing-in etc
			if (form.attr("id").matches(regEx)
				|| form.attr("class").matches(regEx)
				|| form.attr("name").matches(regEx)
				|| form.attr("action").matches(regEx)) {
				chances++;
			}
			// check if form contains email field
			// or form has input field name as email or username
			if (form.select("input").attr("name").matches("(email|username)")
				|| (form.select("input[type=email]").size()>0)) {
				chances++;
			}
			// check form has input type password field
			if (form.select("input[type=password]").size()>0) {
				chances++;
			}
			// check either form has link that has text containing Forgot word
			if (form.select("a[href]").text().matches("(?i).*orgot.*")) {
				chances++;
			}
			// check button/input of type submit has text or value that matches login, log-in, sign in comb 
			if (form.select("input[type=submit]").text().matches(regEx)
				|| form.select("button[type=submit]").text().matches(regEx)
				|| form.select("input[type=submit]").val().matches(regEx)
				|| form.select("button[type=submit]").val().matches(regEx))	{
				chances++;
			}
			// if 3 out of 5 criteria matches then we have a login form
			if (chances>=3) break;
		}
		return (chances>=3)?true:false;
	}

	public String getTitle() {
		Elements title = this.doc.select("title");
		return title.text();
	}
	
	/*
	 * get all headings at once using select with multiple tag
	 * check if it has h1-h6 one by one and size of elements in h1-h6
	 * represents the total number of headings of particular heading level
	 */
	public JSONObject getHeadings() {
		Elements headings = this.doc.select("h1, h2, h3, h4, h5, h6");
		JSONObject headingsObject = new JSONObject();
		headingsObject.put("h1", headings.select("h1").size());
		headingsObject.put("h2", headings.select("h2").size());
		headingsObject.put("h3", headings.select("h3").size());
		headingsObject.put("h4", headings.select("h4").size());
		headingsObject.put("h5", headings.select("h5").size());
		headingsObject.put("h6", headings.select("h6").size());
		return headingsObject;
	}
	
	/*
	 * get all 'a' tags that have attribute 'href'
	 * parse the url and get host name
	 * host name is used to detect if it is internal link or external
	 * and ping the url to check whether is it accessible or not
	 * count the number of accesible and inaccessible internal links
	 * count the number of accesible and inaccessible external links 
	 */
	public JSONObject getLinks() {
		Elements links = this.doc.select("a[href]");
		String[] urlArr = this.url.split("/");
		String baseUrl = urlArr[0]+"//"+urlArr[2];
		
		Integer internalReachable = 0,
				internalNotReachable = 0,
				externalReachable = 0,
				externalNotReachable = 0;
		
		for (Element link : links) {
			String linkHref = link.attr("abs:href");
			//filter email links here
			if (linkHref.matches("https?.*")) {
				//check if links are accessible
				Boolean reachable = pingURL(linkHref, 1000);
				//if internal links
				if (linkHref.contains(baseUrl)) {
					// is link reachable increment reachable otherwise notReachable counter
					if (reachable) internalReachable++; else internalNotReachable++; 
				} else { // if external links
					if (reachable) externalReachable++; else externalNotReachable++;
				}
			}
		}
		
		JSONObject internalLinksObject = new JSONObject()
			.put("reachable", internalReachable)
			.put("notReachable", internalNotReachable);
		JSONObject externalLinksObject = new JSONObject()
			.put("reachable", externalReachable)
			.put("notReachable", externalNotReachable);
		
		JSONObject linksObject = new JSONObject()
			.put("internal", internalLinksObject)
			.put("external", externalLinksObject);
		
		return linksObject; 
	}

	/*
	 * request a URL by sending HEAD request
	 * returns true if status code is 200-399
	 * it requires a url
	 * and a timeout in milli seconds for conenction timeout and response read timeout
	 * returns true if response on head request send statuscode between 200 and 399
	 * else return true
	 */
	public static boolean pingURL(String url, int timeout) {
	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("HEAD");
	        int responseCode = connection.getResponseCode();
	        return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	    	System.err.println(exception);
	        return false;
	    }
	}

}
