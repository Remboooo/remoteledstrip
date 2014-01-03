package com.nedap.fridayafternoon.pivotallightshost.messages;

import com.nedap.fridayafternoon.pivotallightshost.Client;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public abstract class ClientMessageGenerator {
    public final int id;

    protected ClientMessageGenerator(int id) {
        this.id = id;
    }

    public ZNetTxRequest buildTxRequest(XBeeAddress64 destination, Client client) {
        int[] payload = buildPayload(client);
        int[] msgContents = new int[payload.length+1];
        msgContents[0] = id;
        System.arraycopy(payload, 0, msgContents, 1, payload.length);
        return new ZNetTxRequest(destination, msgContents);
    }

    public abstract int[] buildPayload(Client client);
}
