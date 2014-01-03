package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.ClientMessageGenerator;
import com.rapplogic.xbee.api.*;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rembrand.vanlakwijk on 21-12-13.
 */
public class Host implements PacketListener {
    final Logger logger = Logger.getLogger(this.getClass());

    private final XBee xbee;
    final Set<Client> clients = new HashSet<Client>();

    final Set<ClientListener> clientListeners = new HashSet<ClientListener>();

    public Host(String port, int baudrate) throws XBeeException {
        XBeeConfiguration conf = new XBeeConfiguration();
        xbee = new XBee(conf);
        xbee.open(port, baudrate);
        xbee.addPacketListener(this);
    }

    public synchronized void broadcast(int[] data) {
        for (Client c : clients) {
            c.send(data);
        }
    }

    public synchronized void broadcast(ClientMessageGenerator message) {
        for (Client c : clients) {
            c.send(message);
        }
    }

    public void addClientListener(ClientListener cl) {
        synchronized(clientListeners) {
            clientListeners.add(cl);
        }
    }

    public void removeClientListener(ClientListener cl) {
        synchronized(clientListeners) {
            clientListeners.remove(cl);
        }
    }

    public synchronized void timedOut(Client c) {
        c.destroy();
        clients.remove(c);
        logger.info("Client "+c+" timed out");
        synchronized(clientListeners) {
            for (ClientListener cl : clientListeners) {
                cl.clientRemoved(c);
            }
        }
    }

    @Override
    public void processResponse(XBeeResponse xBeeResponse) {
        if (xBeeResponse instanceof ZNetRxResponse) {
            final ZNetRxResponse rx = (ZNetRxResponse)xBeeResponse;
            final int[] data = rx.getData();
            if (data.length == 3 && data[0] == 0xAB) {
                new Thread("Signup delegate "+rx.getRemoteAddress64().toString()) {
                    public void run() {
                        processSignup(rx.getRemoteAddress64(), (long)data[1] | ((long)data[2]<<8));
                    }
                }.start();
            }
        }
    }

    private void processSignup(XBeeAddress64 remoteAddress64, long ledCount) {
        logger.info("Acknowledging signup request from " + remoteAddress64);
        ZNetTxRequest tx = new ZNetTxRequest(remoteAddress64, new int[] {0xAC});
        try {
            XBeeResponse r = sendSynchronous(tx, 5000);
            if (r instanceof ZNetTxStatusResponse) {
                ZNetTxStatusResponse txr = (ZNetTxStatusResponse)r;
                if (txr.getDeliveryStatus() == ZNetTxStatusResponse.DeliveryStatus.SUCCESS) {
                    logger.info("Signed up " + remoteAddress64);
                    Client c = new Client(this, remoteAddress64, ledCount);
                    if (clients.contains(c)) {
                        for (Client c2 : clients) {
                            if (c2.equals(c)) {
                                c2.destroy();
                                clients.remove(c2);
                            }
                        }
                        synchronized(clientListeners) {
                            for (ClientListener cl : clientListeners) {
                                cl.clientRemoved(c);
                            }
                        }
                    }
                    clients.add(c);
                    synchronized(clientListeners) {
                        for (ClientListener cl : clientListeners) {
                            cl.clientAdded(c);
                        }
                    }
                } else {
                    logger.info("Signup of " + remoteAddress64 + " failed; status " + txr.getDeliveryStatus());
                }
            } else {
                logger.debug("Received UNEXPECTED "+r);
            }
        } catch (XBeeException e) {
            logger.error("Signup of "+remoteAddress64+" failed", e);
        }
    }

    int frameId = 128;
    public XBeeResponse sendSynchronous(ZNetTxRequest tx, int timeout) throws XBeeException {
        synchronized (xbee) {
            frameId = (frameId+1)%256;
            tx.setFrameId(frameId);
            return xbee.sendSynchronous(tx, timeout);
        }
    }
}
