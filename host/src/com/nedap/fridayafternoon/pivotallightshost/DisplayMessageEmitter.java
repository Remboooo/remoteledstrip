package com.nedap.fridayafternoon.pivotallightshost;

import com.nedap.fridayafternoon.pivotallightshost.messages.ClientMessageGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public class DisplayMessageEmitter {
    final Set<DisplayMessageListener> listeners = new HashSet<DisplayMessageListener>();

    public synchronized void addDisplayMessageListener(DisplayMessageListener l) {
        listeners.add(l);
    }

    public synchronized void removeDisplayMessageListener(DisplayMessageListener l) {
        listeners.remove(l);
    }

    public synchronized void sendDisplayMessage(ClientMessageGenerator cm) {
        for (DisplayMessageListener l : listeners) {
            l.updateDisplay(cm);
        }
    }
}
