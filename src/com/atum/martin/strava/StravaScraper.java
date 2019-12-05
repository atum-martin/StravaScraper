package com.atum.martin.strava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;


public class StravaScraper {

	
	private static final String regex = "pageView.segmentEfforts().reset(";
	private StravaHttpAgent agent;
	
	
	public static void main(String[] args) {
		
		String cookies = args[0];
		String fileIdentifier = args[1];
		int athleteId = Integer.parseInt(args[2]);
		
		System.out.println(cookies);
		
		new StravaScraper(cookies, fileIdentifier, athleteId);
	}
	
	public StravaScraper(String cookies, String fileIdentifier, int athleteId) {
		agent = new StravaHttpAgent(cookies);
		
		String activityFileName = "output/"+fileIdentifier+"_activityids.txt";
		String segmentFileName = "output/"+fileIdentifier+"_segmentids.txt";

		File activityFile = new File(activityFileName);
		if(!activityFile.exists()) {
			List<Long> activityIds = scrapeStravaAthlete(athleteId, 12);
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(activityFile));
				for(long activityId : activityIds) {
					bw.write(activityId+"\n");
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		List<Long> activityIds = readFile(activityFileName);
		try {
			File segmentFile = new File(segmentFileName);
			if(segmentFile.exists())
				return;
			BufferedWriter bw = new BufferedWriter(new FileWriter(segmentFile));
			for(long id : activityIds) {
				List<Long> segmentIds = scrapeStravaActivity(id);
				for(long segmentId : segmentIds) {
					bw.write(segmentId+"\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<Long> readFile(String file) {
		List<Long> activityIds = new ArrayList<Long>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while((line = br.readLine()) != null) {
				activityIds.add(Long.parseLong(line));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return activityIds;
	}

	private List<Long> scrapeStravaActivity(long activityId) {
		String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
		List<String> html = agent.get("activities/"+activityId, accept);
		
		String json = grepJson(html);
		JSONObject jsonObj = parseJson(json);
		List<Long> segmentIds = parseJsonForSegmentIds(jsonObj);
		
		return segmentIds;
	}
	
	private static final Pattern groupActivityPattern = Pattern.compile("js-Group-Activity-([0-9]+)\\\\");
	private static final Pattern soloActivityPattern = Pattern.compile("data-entity-id=\\\\'Activity-([0-9]+)\\\\");
	
	private List<Long> scrapeStravaAthlete(int athleteId, int months) {
		String accept = "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript";
		List<Long> activityIds = new ArrayList<Long>();
		for(int i = 1; i <= months; i++) {
			String monthStr = getMonth(i);
			List<String> html = agent.get("athletes/"+athleteId+"/interval?interval=2019"+monthStr+"&interval_type=month&chart_type=miles&year_offset=0", accept);
			
			for(String line : html) {
		        Matcher matcher = groupActivityPattern.matcher(line);
		        while (matcher.find()) {
		        	activityIds.add(Long.parseLong(matcher.group(1)));
		        	System.out.println(matcher.group(1));
		        }
		        
		        matcher = soloActivityPattern.matcher(line);
		        while (matcher.find()) {
		        	activityIds.add(Long.parseLong(matcher.group(1)));
		        	System.out.println(matcher.group(1));
		        }
				
			}
		}
		return activityIds;
	}
	
	private void scrapeLeaderboardForAthlete(String name, long segmentId) {
		name = name.toLowerCase();
		String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";

		for(int page = 1; page <= 50; page++) {
			//https://www.strava.com/segments/10141957/leaderboard?age_group=25_34&filter=overall&gender=M&page=29&per_page=100&weight_class=65_74&partial=true
			List<String> html = agent.get("segments/"+segmentId+"/leaderboard?age_group=25_34&filter=overall&gender=M&page="+page+"&per_page=100&weight_class=65_74&partial=true", accept);
			int i = 0;
			for(String line : html) {
				i++;
		        if(line.toLowerCase().contains(name)) {
		        	System.out.println(line);
		        	System.out.println(html.get(i+1));
		        	System.out.println(html.get(i+2));
		        	System.out.println(html.get(i+3));
		        	return;
		        }
				
			}
		}
	}
	
	
	
	private String getMonth(int i) {
		if (i < 10)
			return "0"+i;
		return new Integer(i).toString();
	}

	private List<Long> parseJsonForSegmentIds(JSONObject obj) {
		List<Long> segmentIds = new ArrayList<Long>();
		for(int i = 0; i < obj.getJSONArray("efforts").length(); i++) {
			JSONObject segmentEffort = obj.getJSONArray("efforts").getJSONObject(i);
			long segmentId = segmentEffort.getLong("segment_id");
			segmentIds.add(segmentId);
			System.out.println(segmentId); 
		}
		return segmentIds;
	}

	public JSONObject parseJson(String json) {
		JSONObject obj = new JSONObject(json);
		return obj;
	}

	
	public String grepJson(List<String> input) {
		String json = null;
		for(String line : input) {
			if(line.contains(regex)) {
				json = line;
				break;
			}
		}
		if(json == null) {
			System.out.println("can't find json");
			return null;
		}
		json = json.substring(json.indexOf(regex)+regex.length());
		int endIndex = json.lastIndexOf(",");
		json = json.substring(0,endIndex);
		return json;
	}
	
}
