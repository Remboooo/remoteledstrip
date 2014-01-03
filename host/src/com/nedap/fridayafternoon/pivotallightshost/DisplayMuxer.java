package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.ClientMessageGenerator;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class DisplayMuxer implements ClientListener, DisplayMessageListener {
    private ClientMessageGenerator lastIncoming = null;
    private final Host host;

    public DisplayMuxer(Host host) {
        this.host = host;
        host.addClientListener(this);
    }

    @Override
    public synchronized void clientAdded(Client client) {
        if (lastIncoming != null) {
            client.send(lastIncoming);
        }
    }

    @Override
    public synchronized void clientRemoved(Client client) {
        // nothing to do
    }

    @Override
    public synchronized void updateDisplay(ClientMessageGenerator cm) {
        lastIncoming = cm;
        host.broadcast(cm);
    }
}
