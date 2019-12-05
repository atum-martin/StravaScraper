package com.atum.martin.strava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class StravaHttpAgent {

	private String cookies;
	StravaHttpAgent(String cookies){
		this.cookies = cookies;
	}
	
	public List<String> get(String path, String accept){
		String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36";
		List<String> output = new ArrayList<String>();
		try {
			HttpsURLConnection conn = (HttpsURLConnection) (new URL("https://www.strava.com/"+path)).openConnection();
			conn.addRequestProperty("user-agent", useragent);
			conn.addRequestProperty("cookie", cookies);
			conn.addRequestProperty("accept", accept);
			conn.addRequestProperty("referer", "https://www.strava.com/dashboard");
			if(path.contains("athletes/"))
				conn.addRequestProperty("x-requested-with", "XMLHttpRequest");
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				output.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
	
}
