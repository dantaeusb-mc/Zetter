package com.dantaeusb.immersivemp.locks.network;

import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;

public class ClientHandler {
    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ModLockNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
