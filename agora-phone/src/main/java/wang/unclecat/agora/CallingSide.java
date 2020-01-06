package wang.unclecat.agora;

import io.agora.rtc.RtcEngine;
import lombok.experimental.Delegate;
import wang.unclecat.agora.utils.Logger;

/**
 * 主叫
 *
 * @author 喵叔catuncle    19-12-28
 */
class CallingSide implements Internal {

    private final NetPhone netPhone;

    private final RtcEngine mRtcEngine;

    private final PhoneSM phoneSM;

    private String remoteAccount;

    @Delegate
    private final InternalImpl internal;

    public void onStateUpdate(PhoneState from, PhoneState to, PhoneSM.Event event, DialBean dialBean) {
        Logger.d("onStateUpdate() called with: from = [" + from + "], to = [" + to + "], event = [" + event + "], dialBean = [" + dialBean + "]");
        switch (to) {
            case IDLE:
                if (from != null) {
                    internal.handleHangUp(from);
                }
                break;
            case RINGING:
                break;
            case CONNECTING:
                netPhone.listener.onHostDial(dialBean);
                break;
            case CONNECTED:
                break;
            case SPEAKING:
                mRtcEngine.setEnableSpeakerphone(false);
                internal.setStartTime(System.currentTimeMillis());
                netPhone.listener.onReadyToSpeak(remoteAccount);
                break;
        }
    }

    public CallingSide(NetPhone netPhone, RtcEngine rtcEngine) {
        this.netPhone = netPhone;
        this.mRtcEngine = rtcEngine;
        phoneSM = PhoneSM.create();
        phoneSM.addObserver(this, "onStateUpdate", PhoneState.class, PhoneState.class, PhoneSM.Event.class, DialBean.class);
        phoneSM.start();
        internal = new InternalImpl(netPhone, mRtcEngine, phoneSM);
    }

    /**
     * 拨打
     *
     * @param remoteAccount 被叫方id
     * @param dialBean      拨打参数
     */
    boolean dial(String remoteAccount, DialBean dialBean) {
        Logger.d("dial with: remoteAccount = " + remoteAccount + ", dialBean = " + dialBean + "");
        if (dialBean != null) {
            phoneSM.fire(PhoneSM.Event.Dial, dialBean);
            this.remoteAccount = remoteAccount;
            internal.setRemoteAccount(remoteAccount);
            // TODO: by catuncle 19-12-28 对方未登录，怎么处理
            if (netPhone.checkStatus()) {
                mRtcEngine.setDefaultAudioRoutetoSpeakerphone(false);
                netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createDialMsg(dialBean).toJsonString());
            } else {
                Logger.d("用户未登录");
            }
            return true;
        }
        return false;
    }

    void join(String channel) {

        netPhone.joinChannel(channel);
    }

    void handleOnJoinChannelSuccess(String channel, int uid, int elapsed) {
        phoneSM.fire(PhoneSM.Event.Join2);
        netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createJoinedMsg().toJsonString());
    }
}
