package com.nedap.fridayafternoon.pivotallightshost;

import com.rapplogic.xbee.api.*;
import org.apache.log4j.Logger;

public class Application {
    static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Usage: Application <pivotal token> <pivotal project id> <port> <baudrate>");
            return;
        }
        Host host;
        try {
            host = new Host(args[2], Integer.parseInt(args[3]));
        } catch (XBeeException e) {
            logger.error(e.getMessage());
            return;
        } catch (NumberFormatException e) {
            logger.error("Invalid baud rate specified");
            return;
        }

        PivotalStatsCollector psc;
        try {
            psc = new PivotalStatsCollector(args[0], Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            logger.error("Invalid project ID specified");
            return;
        }

        PivotalStatsToLEDHistogramDucktape ducktape = new PivotalStatsToLEDHistogramDucktape();

        psc.addListener(ducktape);
        psc.start();

        DisplayMuxer muxer = new DisplayMuxer(host);

        ducktape.addDisplayMessageListener(muxer);

        try {
            psc.join();
        } catch (InterruptedException ignore) {}
    }
}
