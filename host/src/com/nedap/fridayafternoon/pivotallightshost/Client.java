package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.ClientMessageGenerator;
import com.rapplogic.xbee.api.*;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import org.apache.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class Client implements PacketListener {
    final Logger logger = Logger.getLogger(this.getClass());

    final XBeeAddress64 address;
    private final Host host;
    private final long ledCount;
    private volatile boolean running = true;
    final Watchdog watchdog = new Watchdog();
    final MessageSender sender = new MessageSender();

    public Client(Host host, XBeeAddress64 address, long ledCount) {
        this.address = address;
        this.host = host;
        this.ledCount = ledCount;
        watchdog.start();
        sender.start();
    }

    public void send(int[] data) {
        ZNetTxRequest tx  = new ZNetTxRequest(address, data);
        sender.send(tx);
    }

    public void destroy() {
        running = false;
        watchdog.interrupt();
        sender.interrupt();
        try {
            watchdog.join();
            sender.join();
        } catch (InterruptedException ignore) {}
    }

    @Override
    public void processResponse(XBeeResponse xBeeResponse) {
        if (xBeeResponse instanceof ZNetRxResponse) {
            ZNetRxResponse rx = (ZNetRxResponse) xBeeResponse;
            if (rx.getRemoteAddress64().equals(address)) {
                watchdog.kick();
            }
        }
    }

    public void send(ClientMessageGenerator message) {
        sender.send(message.buildTxRequest(address, this));
    }

    public long getLedCount() {
        return ledCount;
    }

    private class Watchdog extends Thread {
        final static long TIMEOUT = 60000;
        final AtomicLong lastKick = new AtomicLong();

        @Override
        public void run() {
            lastKick.set(System.currentTimeMillis());
            while (running) {
                try {
                    Thread.sleep(System.currentTimeMillis()-lastKick.get());
                    if (lastKick.get() < System.currentTimeMillis()-TIMEOUT) {
                        Client.this.watchdogTimeout();
                    }
                } catch (InterruptedException ignore) {
                }
            }
        }

        public void kick() {
             lastKick.set(System.currentTimeMillis());
        }
    }

    private void watchdogTimeout() {
        host.timedOut(this);
    }

    private class MessageSender extends Thread {
        final ArrayBlockingQueue<ZNetTxRequest> queue = new ArrayBlockingQueue<ZNetTxRequest>(100);

        public void send(ZNetTxRequest req) {
            try {
                queue.put(req);
            } catch (InterruptedException ignore) {
            }
        }

        @Override
        public void run() {
            while (running) {
                try {
                    ZNetTxRequest rx = queue.take();
                    boolean success = false;
                    while (!success) {
                        XBeeResponse r = host.sendSynchronous(rx, 5000);
                        if (r instanceof ZNetTxStatusResponse) {
                            success = ((ZNetTxStatusResponse)r).isSuccess();
                        }
                    }
                    logger.debug("Sent to "+rx.getDestAddr64()+": "+rx.getPayload());
                    watchdog.kick();
                } catch (InterruptedException ignore) {
                } catch (XBeeException e) {
                    logger.error("Failed to send message to "+address, e);
                }

            }
        }
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Client) {
            return address.equals(((Client)obj).address);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Client{" +
                "address=" + address +
                '}';
    }
}
