package wang.unclecat.agora;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode.ConnectionState;
import io.agora.rtm.SendMessageOptions;
import wang.unclecat.agora.utils.Logger;

/**
 * 封装网络电话的操作
 *
 * @author 喵叔catuncle    19-2-28
 */
public class NetPhone {


    Context mContext;
    private RtmClient mRtmClient;
    private SendMessageOptions mSendMsgOptions;

    private int connectionState = ConnectionState.CONNECTION_STATE_DISCONNECTED;


    private String appID;
    private String account;

    private CallingSide callingSide;
    private CalledSide calledSide;


    private RtcEngine mRtcEngine;// Tutorial Step 1


    private final Gson gson = new Gson();

    int testCase = 0;

    public int getTestCase() {
        return testCase;
    }

    public void setTestCase(int testCase) {
        this.testCase = testCase;
    }

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public static NetPhone getInstance() {
        return SingletonHolder.instance;
    }

    InternalImpl internal;
    /**
     * 必须先初始化sdk
     *
     * @param context
     * @param appID
     * @param account 自己的账号，一般用uid
     */
    public void init(Context context, final String appID, final String account) {
        this.mContext = context;
        this.appID = appID;
        this.account = account;
        try {
            // 初始化信令 SDK

            try {
                mRtmClient = RtmClient.createInstance(mContext, appID, new RtmClientListener() {
                    @Override
                    public void onConnectionStateChanged(int state, int reason) {
                        Logger.d("onConnectionStateChanged() called with: state = [" + state + "], reason = [" + reason + "]");
                        connectionState = state;
                    }

                    @Override
                    public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                        Logger.d("onMessageReceived() called with: rtmMessage = [" + rtmMessage.getText() + "], peerId = [" + peerId + "]");
                        // TODO: by catuncle 19-12-20 信令消息，使用json是否合适？

                        final PhoneMsg phoneMsg = gson.fromJson(rtmMessage.getText(), PhoneMsg.class);
                        if (phoneMsg.getType() == PhoneMsg.CALLING) {

                            DialBean dialBean = gson.fromJson(phoneMsg.getMsg(), DialBean.class);
                            calledSide.receive(peerId, dialBean);

                        } else if (phoneMsg.getType() == PhoneMsg.CALLING_test1) {
                            testCase = phoneMsg.getType();
                            DialBean dialBean = gson.fromJson(phoneMsg.getMsg(), DialBean.class);
                            calledSide.receive(peerId, dialBean);
                            mainHandler.postDelayed(() -> accept(), 100);

                        } else if (phoneMsg.getType() == PhoneMsg.CALLING_test2) {
                            testCase = phoneMsg.getType();
                            DialBean dialBean = gson.fromJson(phoneMsg.getMsg(), DialBean.class);
                            calledSide.receive(peerId, dialBean);

                            mainHandler.postDelayed(() -> accept(), 100);

                        } else if (phoneMsg.getType() == PhoneMsg.CALLING_test3) {
                            testCase = phoneMsg.getType();
                            DialBean dialBean = gson.fromJson(phoneMsg.getMsg(), DialBean.class);
                            calledSide.receive(peerId, dialBean);

                            mainHandler.postDelayed(() -> hangUp(), 100);

                        } else if (phoneMsg.getType() == PhoneMsg.CALLING_test4) {
                            testCase = phoneMsg.getType();
                            DialBean dialBean = gson.fromJson(phoneMsg.getMsg(), DialBean.class);
                            calledSide.receive(peerId, dialBean);

                        } else if (phoneMsg.getType() == PhoneMsg.ACCEPT) {

                            callingSide.join(phoneMsg.getMsg());

                        } else if (phoneMsg.getType() == PhoneMsg.JOINED) {

                            calledSide.join3();


                        } else if (phoneMsg.getType() == PhoneMsg.HANGUP) {
                            hangUp2();
                        } else if (phoneMsg.getType() == PhoneMsg.HANGUP_AUTO) {
                            // TODO: by catuncle 19-12-27 回调通知
                            hangUp2();
                        }
                    }

                    @Override
                    public void onTokenExpired() {
                        Logger.d("onTokenExpired() called");
                    }

                    @Override
                    public void onPeersOnlineStatusChanged(Map<String, Integer> status) {
                        Logger.d("onPeersOnlineStatusChanged() called with: status = [" + status + "]");
                        //对方的连接状态
                    }
                });

                if (BuildConfig.DEBUG) {
                    mRtmClient.setParameters("{\"rtm.log_filter\": 65535}");
                }
            } catch (Exception e) {
                Logger.e("init: ", e);
                throw new RuntimeException("NEED TO check rtm sdk init fatal error\n" + Log.getStackTraceString(e));
            }

            // Global option, mainly used to determine whether
            // to support offline messages now.
            mSendMsgOptions = new SendMessageOptions();


            // 登录 Agora 信令系统
            doLogin();


        } catch (Exception e) {
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }

        try {
            mRtcEngine = RtcEngine.create(context, appID, mRtcEventHandler);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(false);
        } catch (Exception e) {
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }

        internal= new InternalImpl(this, mRtcEngine);
        callingSide = new CallingSide(this, mRtcEngine, internal);
        calledSide = new CalledSide(this, mRtcEngine, internal);
    }

    public void destroy() {
        if (mRtmClient != null) {
            mRtmClient.logout(null);
        }

        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }

    public boolean checkStatus() {
        if (connectionState == ConnectionState.CONNECTION_STATE_DISCONNECTED) {
            Logger.d("用户未登录---将要尝试登录");
            doLogin();
        } else {
            Logger.d("checkStatus = " + connectionState);
        }
        return connectionState == ConnectionState.CONNECTION_STATE_CONNECTED;
    }

    private void doLogin() {
        // 登录 Agora 信令系统
        String deviceId = getUniquePsuedoID();
        Logger.d("deviceId: " + deviceId);
        // TODO: by catuncle 19-2-27 token产生
        //用于登录 Agora RTM 系统的动态密钥。开启动态鉴权后可用。集成及测试阶段请将 token 设置为 null。
        mRtmClient.login(null, account, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Logger.d("login success");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Logger.e("login error : " + errorInfo.toString());
            }
        });
    }


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Logger.d("onJoinChannelSuccess with: channel = " + channel + ", uid = " + uid + ", elapsed = " + elapsed + "");
            if (callingSide.isActive()) {
                callingSide.handleOnJoinChannelSuccess();
            }

            if (calledSide.isActive()) {
                calledSide.handleOnJoinChannelSuccess(channel);
            }
        }

        @Override
        public void onUserJoined(int remoteUid, int elapsed) {
            Logger.d("onUserJoined with: remoteUid = " + remoteUid + ", elapsed = " + elapsed + "");
        }

        @Override
        public void onUserOffline(final int remoteUid, final int reason) { // Tutorial Step 4
            Logger.d("onUserOffline with: remoteUid = " + remoteUid + ", reason = " + reason + "");
//            hangUp2();
//            listener.onRemoteHangUp(true, remoteUid + "");
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            Logger.d("onLeaveChannel with: stats = " + gson.toJson(stats) + "");
            if (callingSide.isActive()) {
                callingSide.afterHangUp();
            }
            if (calledSide.isActive()) {
                calledSide.afterHangUp();
            }
        }

        @Override
        public void onError(int err) {
            Logger.e("onError with: err = " + err + "");
        }

    };


    /**
     * 拨打
     *
     * @param remoteAccount 被叫方id
     * @param dialBean      拨打参数
     */
    public boolean dial(String remoteAccount, DialBean dialBean) {
        return callingSide.dial(remoteAccount, dialBean);
    }

    /**
     * 接听
     */
    public void accept() {
        calledSide.accept();
    }

    /**
     * 进入房间
     *
     * @param channel 房间号
     */
    void joinChannel(String channel) {
        doJoinChannel(channel);
    }

    /**
     * 挂断
     */
    public void hangUp() {
        if (callingSide.isActive()) {
            callingSide.hangUp();
        }
        if (calledSide.isActive()) {
            calledSide.hangUp();
        }
    }

    /**
     * 被告知挂断
     */
    private void hangUp2() {
        if (callingSide.isActive()) {
            callingSide.hangUp2();
        }
        if (calledSide.isActive()) {
            calledSide.hangUp2();
        }
    }

    //获得独一无二的Psuedo ID
    public static String getUniquePsuedoID() {


        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +

                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +

                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +

                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +

                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +

                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +

                Build.USER.length() % 10; //13 位

        String serial;
        try {
            serial = Build.class.getField("SERIAL").get(null).toString();
            //API>=9 使用serial号
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            //serial需要一个初始化
            serial = "serial"; // 随便一个初始化
        }
        //使用硬件信息拼凑出来的15位号码
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    public void setEnableSpeakerphone(boolean b) {
        Logger.d("setEnableSpeakerphone with: b = " + b + "");
        mRtcEngine.setEnableSpeakerphone(b);
    }

    public boolean isSpeakerphoneEnabled() {
        return mRtcEngine.isSpeakerphoneEnabled();
    }

    public void muteMic(boolean b) {
//        该方法需要在 joinChannel 之后调用才能生效。
//        开启或关闭本地语音采集及处理
        // onMicrophoneEnabled
        Logger.d("muteMic with: b = " + b + "");
        mRtcEngine.enableLocalAudio(!b);
    }


    /**
     * 格式化通话时间
     *
     * @param cnt (以秒为单位)
     */
    public static String fmtTime(int cnt) {
        int hour = cnt / 3600;
        int min = cnt % 3600 / 60;
        int second = cnt % 60;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hour, min, second);
    }


    /**
     * API CALL: send message to peer
     */
    void sendPeerMessage(String peerId, String content) {
        Logger.d("sendPeerMessage with: peerId = " + peerId + ", content = " + content + "");
        // step 1: create a message
        RtmMessage message = mRtmClient.createMessage();
        message.setText(content);
//        remoteAccount, 0, PhoneMsg.createDialMsg(dialBean).toJsonString(), "");
        // step 2: send message to peer
        mRtmClient.sendMessageToPeer(peerId, message, mSendMsgOptions, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // do nothing
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Logger.e("onFailure: errorInfo = " + errorInfo);
            }
        });
    }


    private void doJoinChannel(String channel) {
        int ret = mRtcEngine.joinChannel(null, channel, "Extra Optional Data", Integer.valueOf(account));
        if (ret != 0) {
            // TODO: by catuncle 异常处理
        }
    }



    private NetPhone() {
    }

    private static class SingletonHolder {
        private final static NetPhone instance = new NetPhone();
    }

    //用于单元测试
    PhoneState getHostState() {
        return callingSide.getState();
    }

    //用于单元测试
    PhoneState getClientState() {
        return calledSide.getState();
    }


    private List<NetPhoneListener> listeners = new CopyOnWriteArrayList<>();

    NetPhoneListener listener = new NetPhoneListener() {
        @Override
        public void onHostDial(DialBean dialBean) {
            for (NetPhoneListener listener : listeners) {
                listener.onHostDial(dialBean);
            }
        }

        @Override
        public void onClientReceiveDial(String remoteAccount, DialBean bean) {
            for (NetPhoneListener listener : listeners) {
                listener.onClientReceiveDial(remoteAccount, bean);
            }
        }

        @Override
        public void onReadyToSpeak(String remoteAccount) {
            for (NetPhoneListener listener : listeners) {
                listener.onReadyToSpeak(remoteAccount);
            }
        }

        @Override
        public void onTick(int elapsedTime) {
            for (NetPhoneListener listener : listeners) {
                listener.onTick(elapsedTime);
            }
        }

        @Override
        public void onHangUp(boolean connected, int totalDuration) {
            for (NetPhoneListener listener : listeners) {
                listener.onHangUp(connected, totalDuration);
            }
        }

        @Override
        public void onRemoteHangUp(boolean connected, int totalDuration, String remoteAccount) {
            for (NetPhoneListener listener : listeners) {
                listener.onRemoteHangUp(connected, totalDuration , remoteAccount);
            }
        }
    };

    public void addListener(NetPhoneListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除监听
     */
    public void removeListener(NetPhoneListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
}
