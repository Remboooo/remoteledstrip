package com.nedap.fridayafternoon.pivotallightshost;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class PivotalStatsCollector extends Thread {
    private static final Logger logger = Logger.getLogger(PivotalStatsCollector.class);
    private final String token;
    private final long projectID;
    private volatile boolean running = true;
    private final Set<PivotalStatsListener> listeners = new HashSet<PivotalStatsListener>();

    public PivotalStatsCollector(String token, long projectID) {
        this.token = token;
        this.projectID = projectID;
    }

    public void addListener(PivotalStatsListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(PivotalStatsListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void getReleaseStories() {
        try {
            logger.debug("Fetching stories in current release of project "+projectID+" from Pivotal");
            String url = "https://www.pivotaltracker.com/services/v5/projects/"+projectID+"/iterations?scope=current_backlog";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
            get.addRequestHeader("X-TrackerToken", token);
            get.addRequestHeader("Content-Type", "application/json");
            int status = client.executeMethod(get);
            if (status != HttpStatus.SC_OK) {
                logger.warn("Pivotal request failed: " + get.getStatusLine());
            } else {
                byte[] response = get.getResponseBody();
                JsonParser parser = new JsonParser();
                JsonElement root = parser.parse(new String(response, "UTF-8"));
                ArrayList<JsonObject> releaseStories = new ArrayList<JsonObject>();
                boolean done = false;
                for (JsonElement iteration : root.getAsJsonArray()) {
                    for (JsonElement storyElem : iteration.getAsJsonObject().get("stories").getAsJsonArray()) {
                        JsonObject story = storyElem.getAsJsonObject();
                        if ("release".equals(story.get("story_type").getAsString())) {
                            if ("accepted".equals(story.get("current_state").getAsString())) {
                                releaseStories.clear();
                            } else {
                                processReleaseStories(story, releaseStories);
                                done = true;
                                break;
                            }
                        }
                        else {
                            releaseStories.add(story);
                        }
                    }
                    if (done) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get release stories for project "+projectID+" from Pivotal", e);
        }
    }

    private void processReleaseStories(JsonObject releaseStory, List<JsonObject> releaseStories) {
        logger.debug("Release " + releaseStory.get("name") + " contains " + releaseStories.size() + " stories");
        int accepted = 0, delivered = 0, rejected = 0, finished = 0, started = 0, total = 0;
        for (JsonObject story : releaseStories) {
            String state = story.get("current_state").getAsString();
            if ("accepted".equals(state)) {
                accepted++;
            }
            else if ("delivered".equals(state)) {
                delivered++;
            }
            else if ("rejected".equals(state)) {
                rejected++;
            }
            else if ("finished".equals(state)) {
                finished++;
            }
            else if ("started".equals(state)) {
                started++;
            }
            total++;
        }
        synchronized (listeners) {
            for (PivotalStatsListener l : listeners) {
                l.onCurrentReleaseStats(releaseStory.get("name").getAsString(), accepted, delivered, rejected, finished, started, total);
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            getReleaseStories();
            try {
                Thread.sleep(300000);
            } catch (InterruptedException ignore) {}
          }
    }

}
