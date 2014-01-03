package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.LEDHistogramMessage;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class PivotalStatsToLEDHistogramDucktape extends DisplayMessageEmitter implements PivotalStatsListener {
    private static final Color ACCEPTED_COLOR = new Color(0x9D, 0xC8, 0xE1);
    private static final Color DELIVERED_COLOR = new Color(0x69, 0x97, 0x12);
    private static final Color REJECTED_COLOR = new Color(0x9B, 0x1F, 0x36);
    private static final Color FINISHED_COLOR = new Color(0xFB, 0x99, 0x14);
    private static final Color STARTED_COLOR = new Color(0x1F, 0x54, 0x6D);
    private static final Color UNSTARTED_COLOR = new Color(0x00, 0x00, 0x00);

    @Override
    public void onCurrentReleaseStats(String release, int accepted, int delivered, int rejected, int finished, int started, int total) {
        LEDHistogramMessage msg = new LEDHistogramMessage(6);

        msg.setHistogramColor(0, ACCEPTED_COLOR);
        msg.setHistogramColor(1, DELIVERED_COLOR);
        msg.setHistogramColor(2, REJECTED_COLOR);
        msg.setHistogramColor(3, FINISHED_COLOR);
        msg.setHistogramColor(4, STARTED_COLOR);
        msg.setHistogramColor(5, UNSTARTED_COLOR);

        msg.setHistogramPartSize(0, (double)accepted/(double)total);
        msg.setHistogramPartSize(1, (double)delivered/(double)total);
        msg.setHistogramPartSize(2, (double)rejected/(double)total);
        msg.setHistogramPartSize(3, (double)finished/(double)total);
        msg.setHistogramPartSize(4, (double)started/(double)total);

        sendDisplayMessage(msg);
    }
}
