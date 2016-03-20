package com.webpageanalyzer.resources;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.jsoup.HttpStatusException;

@WebServlet("/analyze")
public class WebPageAnalyzerServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    	PrintWriter out = null;
		try {
			out = response.getWriter();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	try {
			String url = request.getParameter("url");
			
			System.out.println("URL " + url);
			
			if (url.isEmpty()) throw new Exception("URL cannot be empty");
    		
			WebPageAnalyzer analyzer = new WebPageAnalyzer(url);
			JSONObject result = analyzer.analyze();
        	
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            
            out.print(result);
		} catch (SocketTimeoutException e ) {
			response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
			out.print(e.toString());
		} catch (HttpStatusException e) {
			response.setStatus(e.getStatusCode());
			out.print(e.getMessage());
		} catch (UnknownHostException e) {
			out.print(e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			out.print(e.toString());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.print(e.getMessage());
		}
    }

}
