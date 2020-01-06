package wang.unclecat.agora;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

import io.agora.rtc.RtcEngine;
import lombok.experimental.Delegate;
import wang.unclecat.agora.utils.Logger;

/**
 * 被叫
 *
 * @author 喵叔catuncle    19-12-28
 */
class CalledSide implements Internal {

    private final NetPhone netPhone;

    private final RtcEngine mRtcEngine;

    private final PhoneSM phoneSM;

    private String remoteAccount;

    private MediaPlayer mMediaPlayer;

    @Delegate
    private final InternalImpl internal;

    public void onStateUpdate(PhoneState from, PhoneState to, PhoneSM.Event event, DialBean dialBean) {
        Logger.d("onStateUpdate() called with: from = [" + from + "], to = [" + to + "], event = [" + event + "], dialBean = [" + dialBean + "]");
        switch (to) {
            case IDLE:
                stopMediaPlayer();
                if (from != null) {
                    internal.handleHangUp(from);
                }
                break;
            case RINGING:
                startMediaPlayer();
                netPhone.listener.onClientReceiveDial(remoteAccount, dialBean);
                break;
            case CONNECTING:
                stopMediaPlayer();
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

    public CalledSide(NetPhone netPhone, RtcEngine rtcEngine) {
        this.netPhone = netPhone;
        this.mRtcEngine = rtcEngine;
        phoneSM = PhoneSM.create();
        phoneSM.addObserver(this, "onStateUpdate", PhoneState.class, PhoneState.class, PhoneSM.Event.class, DialBean.class);
        phoneSM.start();
        internal = new InternalImpl(netPhone, mRtcEngine, phoneSM);
    }

    /**
     * 来电
     *
     * @param remoteAccount 主叫方id
     */
    void receive(String remoteAccount) {
        Logger.d("receive() called with: remoteAccount = [" + remoteAccount + "]");
        if (phoneSM.getCurrentState() != PhoneState.IDLE) {
            Logger.d("告诉主叫：占线");
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpAutoMsg().toJsonString());
        } else {
            this.remoteAccount = remoteAccount;
            internal.setRemoteAccount(remoteAccount);
            phoneSM.fire(PhoneSM.Event.Receive);
        }
    }

    /**
     * 接听
     */
    void accept() {
        Logger.d("accept()");
        if (remoteAccount != null) {

            phoneSM.fire(PhoneSM.Event.Accept);

            netPhone.joinChannel(createChannel());
        }
    }


    void handleOnJoinChannelSuccess(String channel, int uid, int elapsed) {
        phoneSM.fire(PhoneSM.Event.Join1);
        netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createAcceptMsg(channel).toJsonString());
    }

    /**
     * 触发Join3，即将进入通话状态
     */
    void join3() {
        phoneSM.fire(PhoneSM.Event.Join3);
    }

    // TODO: by catuncle 19-12-24 模拟生成房间号
    private String createChannel() {
        return "channel" + System.currentTimeMillis();
    }


    private void startMediaPlayer() {
        if (NetPhone.getInstance().getTestCase() > 0) {
            //单元测试时不开启声音
            return;
        }

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            try {
                AssetFileDescriptor fileDescriptor = netPhone.mContext.getAssets().openFd("voice.mp3");
                mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Logger.e(e.getMessage());
            }

        }

        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                mMediaPlayer.start();
                mMediaPlayer.setLooping(true);
            });
        }
    }

    private void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }
}
