package wang.unclecat.agora;

import io.agora.rtc.RtcEngine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import wang.unclecat.agora.utils.Logger;

@RequiredArgsConstructor
class InternalImpl implements Internal {

    private final NetPhone netPhone;

    private final RtcEngine rtcEngine;

    private final PhoneSM phoneSM;

    @Setter(AccessLevel.PACKAGE)
    private String remoteAccount;

    @Setter(AccessLevel.PACKAGE)
    private long startTime;

    private boolean fromRemoteHangUp = false;

    /**
     * 是否被激活(即非idle状态)
     */
    @Override
    public boolean isActive() {
        PhoneState ca = phoneSM.getCurrentState();
        return ca.compareTo(PhoneState.IDLE) > 0;
    }

    /**
     * 当前状态
     */
    @Override
    public PhoneState getState() {
        return phoneSM.getCurrentState();
    }

    /**
     * 挂断
     */
    @Override
    public void hangUp() {
        fromRemoteHangUp = false;
        PhoneState ca = phoneSM.getCurrentState();
        Logger.d("hangUp() called with: ca = " + ca + "");

        //告诉对方挂断呼叫
        if (ca.compareTo(PhoneState.IDLE) > 0) {
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpMsg().toJsonString());
        }

        //挂断
        doHangUp(ca);
    }


    /**
     * 被告知挂断
     */
    @Override
    public void hangUp2() {
        fromRemoteHangUp = true;
        PhoneState ca = phoneSM.getCurrentState();
        Logger.d("hangUp2() called with: ca = " + ca + "");

        //挂断
        doHangUp(ca);
    }


    private void doHangUp(PhoneState ca) {
        if (ca.compareTo(PhoneState.CONNECTED) >= 0) {
            rtcEngine.leaveChannel();
        } else {
            onHangUp();
        }
    }

    /**
     * 挂断后更新状态
     */
    @Override
    public void onHangUp() {
        Logger.d("onHangUp() called");
        netPhone.setTestCase(0);
        phoneSM.fire(PhoneSM.Event.HangUp);
    }



    int getSpeakTime() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }


    void handleHangUp(PhoneState from) {
        boolean connected = (from == PhoneState.SPEAKING);
        int totalDuration = connected ? getSpeakTime() : 0;
        if (fromRemoteHangUp) {
            netPhone.listener.onRemoteHangUp(connected, totalDuration, remoteAccount);
        } else {
            netPhone.listener.onHangUp(connected, totalDuration);
        }
    }
}
