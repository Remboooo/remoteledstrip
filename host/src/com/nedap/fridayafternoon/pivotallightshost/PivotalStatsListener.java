package com.nedap.fridayafternoon.pivotallightshost;

/**
* Created by rembrand.vanlakwijk on 22-12-13.
*/
public interface PivotalStatsListener {
    public void onCurrentReleaseStats(String release, int accepted, int delivered, int rejected, int finished, int started, int total);
}
