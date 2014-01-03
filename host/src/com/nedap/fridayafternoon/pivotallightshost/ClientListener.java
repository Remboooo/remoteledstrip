package com.nedap.fridayafternoon.pivotallightshost;

/**
 * Created by rembrand.vanlakwijk on 22-12-13.
 */
public interface ClientListener {
    public void clientAdded(Client client);
    public void clientRemoved(Client client);
}
