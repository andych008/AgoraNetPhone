package wang.unclecat.agora

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.ConnectionState
import wang.unclecat.agora.utils.Logger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 封装网络电话的操作
 *
 * @author 喵叔catuncle    19-2-28
 */
class NetPhone private constructor() {
    var mContext: Context? = null
    private var mRtmClient: RtmClient? = null
    private var mSendMsgOptions: SendMessageOptions? = null
    private var connectionState = ConnectionState.CONNECTION_STATE_DISCONNECTED
    private var appID: String? = null
    private var account: String? = null
    private var mRtcEngine // Tutorial Step 1
            : RtcEngine? = null
    private val gson = Gson()
    var testCase = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    var internal: InternalImpl? = null


    /**
     * 必须先初始化sdk
     *
     * @param context
     * @param appID
     * @param account 自己的账号，一般用uid
     */
    fun init(context: Context, appID: String?, account: String?) {
        mContext = context
        this.appID = appID
        this.account = account
        try {
            // 初始化信令 SDK
            try {
                mRtmClient = RtmClient.createInstance(mContext, appID, object : RtmClientListener {
                    override fun onConnectionStateChanged(state: Int, reason: Int) {
                        Logger.d("onConnectionStateChanged() called with: state = [$state], reason = [$reason]")
                        connectionState = state
                    }

                    override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {
                        Logger.d("onMessageReceived() called with: rtmMessage = [" + rtmMessage.text + "], peerId = [" + peerId + "]")
                        // TODO: by catuncle 19-12-20 信令消息，使用json是否合适？
                        val phoneMsg = gson.fromJson(rtmMessage.text, PhoneMsg::class.java)
                        if (phoneMsg.type == PhoneMsg.CALLING) {
                            val dialBean = gson.fromJson(phoneMsg.msg, DialBean::class.java)
                            internal!!.receive(peerId, dialBean)
                        } else if (phoneMsg.type == PhoneMsg.CALLING_test1) {
                            testCase = phoneMsg.type
                            val dialBean = gson.fromJson(phoneMsg.msg, DialBean::class.java)
                            internal!!.receive(peerId, dialBean)
                            mainHandler.postDelayed({ accept() }, 100)
                        } else if (phoneMsg.type == PhoneMsg.CALLING_test2) {
                            testCase = phoneMsg.type
                            val dialBean = gson.fromJson(phoneMsg.msg, DialBean::class.java)
                            internal!!.receive(peerId, dialBean)
                            mainHandler.postDelayed({ accept() }, 100)
                        } else if (phoneMsg.type == PhoneMsg.CALLING_test3) {
                            testCase = phoneMsg.type
                            val dialBean = gson.fromJson(phoneMsg.msg, DialBean::class.java)
                            internal!!.receive(peerId, dialBean)
                            mainHandler.postDelayed({ hangUp() }, 100)
                        } else if (phoneMsg.type == PhoneMsg.CALLING_test4) {
                            testCase = phoneMsg.type
                            val dialBean = gson.fromJson(phoneMsg.msg, DialBean::class.java)
                            internal!!.receive(peerId, dialBean)
                        } else if (phoneMsg.type == PhoneMsg.ACCEPT) {
                            internal!!.join(phoneMsg.msg)
                        } else if (phoneMsg.type == PhoneMsg.JOINED) {
                            internal!!.join3()
                        } else if (phoneMsg.type == PhoneMsg.HANGUP) {
                            hangUp2()
                        } else if (phoneMsg.type == PhoneMsg.HANGUP_AUTO) {
                            // TODO: by catuncle 19-12-27 回调通知
                            internal!!.hangUp3()
                        }
                    }

                    override fun onTokenExpired() {
                        Logger.d("onTokenExpired() called")
                    }

                    override fun onPeersOnlineStatusChanged(status: Map<String, Int>) {
                        Logger.d("onPeersOnlineStatusChanged() called with: status = [$status]")
                        //对方的连接状态
                    }
                })
                if (BuildConfig.DEBUG) {
                    mRtmClient!!.setParameters("{\"rtm.log_filter\": 65535}")
                }
            } catch (e: Exception) {
                Logger.e("init: ", e)
                throw RuntimeException("""
    NEED TO check rtm sdk init fatal error
    ${Log.getStackTraceString(e)}
    """.trimIndent())
            }

            // Global option, mainly used to determine whether
            // to support offline messages now.
            mSendMsgOptions = SendMessageOptions()


            // 登录 Agora 信令系统
            doLogin()
        } catch (e: Exception) {
            throw RuntimeException("""
    NEED TO check rtc sdk init fatal error
    ${Log.getStackTraceString(e)}
    """.trimIndent())
        }
        try {
            mRtcEngine = RtcEngine.create(context, appID, mRtcEventHandler)
            mRtcEngine!!.setDefaultAudioRoutetoSpeakerphone(false)
        } catch (e: Exception) {
            throw RuntimeException("""
    NEED TO check rtc sdk init fatal error
    ${Log.getStackTraceString(e)}
    """.trimIndent())
        }
        internal = InternalImpl(this, mRtcEngine!!)
    }

    fun destroy() {
        if (mRtmClient != null) {
            mRtmClient!!.logout(null)
        }
        if (mRtcEngine != null) {
            mRtcEngine!!.leaveChannel()
        }
    }

    fun checkStatus(): Boolean {
        if (connectionState == ConnectionState.CONNECTION_STATE_DISCONNECTED) {
            Logger.d("用户未登录---将要尝试登录")
            doLogin()
        } else {
            Logger.d("checkStatus = $connectionState")
        }
        return connectionState == ConnectionState.CONNECTION_STATE_CONNECTED
    }

    private fun doLogin() {
        // 登录 Agora 信令系统
        val deviceId = uniquePsuedoID
        Logger.d("deviceId: $deviceId")
        // TODO: by catuncle 19-2-27 token产生
        //用于登录 Agora RTM 系统的动态密钥。开启动态鉴权后可用。集成及测试阶段请将 token 设置为 null。
        mRtmClient!!.login(null, account, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                Logger.d("login success")
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                Logger.e("login error : $errorInfo")
            }
        })
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Tutorial Step 1
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Logger.d("onJoinChannelSuccess with: channel = $channel, uid = $uid, elapsed = $elapsed")
            if (internal!!.isACalling()) {
                internal!!.onAJoinChannelSuccess()
            } else {
                internal!!.onBJoinChannelSuccess(channel)
            }
        }

        override fun onUserJoined(remoteUid: Int, elapsed: Int) {
            Logger.d("onUserJoined with: remoteUid = $remoteUid, elapsed = $elapsed")
        }

        override fun onUserOffline(remoteUid: Int, reason: Int) { // Tutorial Step 4
            Logger.d("onUserOffline with: remoteUid = $remoteUid, reason = $reason")
            //            hangUp2();
//            listener.onRemoteHangUp(true, remoteUid + "");
        }

        override fun onLeaveChannel(stats: RtcStats) {
            Logger.d("onLeaveChannel with: stats = " + gson.toJson(stats) + "")
            internal!!.afterHangUp()
        }

        override fun onError(err: Int) {
            Logger.e("onError with: err = $err")
        }
    }

    /**
     * 拨打
     *
     * @param remoteAccount 被叫方id
     * @param dialBean      拨打参数
     */
    fun dial(remoteAccount: String?, dialBean: DialBean?): Boolean {
        return internal!!.dial(remoteAccount!!, dialBean)
    }

    /**
     * 接听
     */
    fun accept() {
        internal!!.accept()
    }

    /**
     * 进入房间
     *
     * @param channel 房间号
     */
    fun joinChannel(channel: String) {
        doJoinChannel(channel)
    }

    /**
     * 挂断
     */
    fun hangUp() {
        internal!!.hangUp()
        //        if (callingSide.isActive()) {
//            callingSide.hangUp();
//        }
//        if (internal.isActive()) {
//            internal.hangUp();
//        }
    }

    /**
     * 被告知挂断
     */
    private fun hangUp2() {
        internal!!.hangUp2()
        //        if (callingSide.isActive()) {
//            callingSide.hangUp2();
//        }
//        if (internal.isActive()) {
//            internal.hangUp2();
//        }
    }

    fun setEnableSpeakerphone(b: Boolean) {
        Logger.d("setEnableSpeakerphone with: b = $b")
        mRtcEngine!!.setEnableSpeakerphone(b)
    }

    val isSpeakerphoneEnabled: Boolean
        get() = mRtcEngine!!.isSpeakerphoneEnabled

    fun muteMic(b: Boolean) {
//        该方法需要在 joinChannel 之后调用才能生效。
//        开启或关闭本地语音采集及处理
        // onMicrophoneEnabled
        Logger.d("muteMic with: b = $b")
        mRtcEngine!!.enableLocalAudio(!b)
    }

    /**
     * API CALL: send message to peer
     */
    fun sendPeerMessage(peerId: String?, content: String) {
        Logger.d("sendPeerMessage with: peerId = $peerId, content = $content")
        // step 1: create a message
        val message = mRtmClient!!.createMessage()
        message.text = content
        //        remoteAccount, 0, PhoneMsg.createDialMsg(dialBean).toJsonString(), "");
        // step 2: send message to peer
        mRtmClient!!.sendMessageToPeer(peerId, message, mSendMsgOptions, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                // do nothing
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                Logger.e("onFailure: errorInfo = $errorInfo")
            }
        })
    }

    private fun doJoinChannel(channel: String) {
        val ret = mRtcEngine!!.joinChannel(null, channel, "Extra Optional Data", Integer.valueOf(account))
        if (ret != 0) {
            // TODO: by catuncle 异常处理
        }
    }

    private object SingletonHolder {
        val instance = NetPhone()
    }

    //用于单元测试
    val hostState: PhoneState
        get() = internal!!.state()

    private val listeners: MutableList<NetPhoneListener> = CopyOnWriteArrayList()
    var listener: NetPhoneListener = object : NetPhoneListener {
        override fun onHostDial(dialBean: DialBean) {
            for (listener in listeners) {
                listener.onHostDial(dialBean)
            }
        }

        override fun onClientReceiveDial(remoteAccount: String, bean: DialBean) {
            for (listener in listeners) {
                listener.onClientReceiveDial(remoteAccount, bean)
            }
        }

        override fun onReadyToSpeak(remoteAccount: String) {
            for (listener in listeners) {
                listener.onReadyToSpeak(remoteAccount)
            }
        }

        override fun onTick(elapsedTime: Int) {
            for (listener in listeners) {
                listener.onTick(elapsedTime)
            }
        }

        override fun onHangUp(connected: Boolean, totalDuration: Int) {
            for (listener in listeners) {
                listener.onHangUp(connected, totalDuration)
            }
        }

        override fun onRemoteHangUp(connected: Boolean, totalDuration: Int, remoteAccount: String) {
            for (listener in listeners) {
                listener.onRemoteHangUp(connected, totalDuration, remoteAccount)
            }
        }
    }

    fun addListener(listener: NetPhoneListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * 移除监听
     */
    fun removeListener(listener: NetPhoneListener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener)
        }
    }

    companion object {
        @JvmStatic
        val instance: NetPhone
            get() = SingletonHolder.instance//serial需要一个初始化
        // 随便一个初始化
        //使用硬件信息拼凑出来的15位号码
//API>=9 使用serial号//13 位

        //获得独一无二的Psuedo ID
        val uniquePsuedoID: String
            get() {
                val m_szDevIDShort = "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 + Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 + Build.TYPE.length % 10 + Build.USER.length % 10 //13 位
                var serial: String
                try {
                    serial = Build::class.java.getField("SERIAL")[null].toString()
                    //API>=9 使用serial号
                    return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
                } catch (exception: Exception) {
                    //serial需要一个初始化
                    serial = "serial" // 随便一个初始化
                }
                //使用硬件信息拼凑出来的15位号码
                return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
            }

        /**
         * 格式化通话时间
         *
         * @param cnt (以秒为单位)
         */
        fun fmtTime(cnt: Int): String {
            val hour = cnt / 3600
            val min = cnt % 3600 / 60
            val second = cnt % 60
            return String.format(Locale.CHINA, "%02d:%02d:%02d", hour, min, second)
        }
    }
}