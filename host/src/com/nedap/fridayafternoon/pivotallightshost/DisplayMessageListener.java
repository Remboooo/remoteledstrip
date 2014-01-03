package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.ClientMessageGenerator;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public interface DisplayMessageListener {
    public void updateDisplay(ClientMessageGenerator cm);
}
