package wang.unclecat.agora

import android.media.MediaPlayer
import com.tinder.StateMachine
import io.agora.rtc.RtcEngine
import wang.unclecat.agora.utils.Logger
import java.io.IOException

class InternalImpl(private val netPhone: NetPhone,
                            private val rtcEngine: RtcEngine):Internal {

    var remoteAccount: String? = null
    var dialBean: DialBean? = null
    var startTime: Long = 0
    private val mMediaPlayer:MediaPlayer = MediaPlayer()


    /**
     * 是否被激活(即非idle状态)
     */
    override fun isActive(): Boolean {
        return phoneSM.state !is PhoneState.Idle
    }

    fun isACalling(): Boolean {
        return phoneSM.state is PhoneState.ACalling
    }
    /**
     * 当前状态
     */
    override fun state(): PhoneState {
        return phoneSM.state
    }

    override fun transAndCheck(event: Event): Boolean {
        val b = phoneSM.transition(event) is StateMachine.Transition.Valid
        if(!b) {
            val ca = phoneSM.state
            Logger.e("transAndCheck() failed with: ca = $ca, event = $event")
        }
        return b
    }


    /**
     * 拨打
     *
     * @param remoteAccount 被叫方id
     * @param dialBean      拨打参数
     */
    fun dial(remoteAccount: String, dialBean: DialBean?): Boolean {
        Logger.d("dial with: remoteAccount = $remoteAccount, dialBean = $dialBean")
        if (dialBean != null) {
            // TODO: by catuncle 19-12-28 对方未登录，怎么处理
            if (netPhone.checkStatus()) {
                rtcEngine.setDefaultAudioRoutetoSpeakerphone(false)
                netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createDialMsg(dialBean).toJsonString())
                this.remoteAccount = remoteAccount
                transAndCheck(InternalImpl.Event.ADial)
            } else {
                Logger.d("用户未登录")
            }
            return true
        }
        return false
    }

    fun join(channel: String) {
        netPhone.joinChannel(channel)
    }

    /**
     * 来电
     *
     * @param remoteAccount 主叫方id
     */
    fun receive(remoteAccount: String, dialBean: DialBean) {
        Logger.d("receive() called with: remoteAccount = [$remoteAccount]")
        if (this.isActive()) {
            Logger.d("告诉主叫：占线")
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpAutoMsg().toJsonString())
        } else {
            this.remoteAccount = remoteAccount
            this.dialBean = dialBean
            transAndCheck(InternalImpl.Event.BReceive)
        }
    }

    /**
     * 接听
     */
    fun accept() {
        Logger.d("accept()")
        if (remoteAccount != null) {
            netPhone.joinChannel(createChannel())
            transAndCheck(InternalImpl.Event.BAccept)
        }
    }


    /**
     * 触发Join3，即将进入通话状态
     */
    fun join3() {
        transAndCheck(InternalImpl.Event.BJoin3)
    }

    // TODO: by catuncle 19-12-24 模拟生成房间号
    private fun createChannel(): String {
        return "channel" + System.currentTimeMillis()
    }

    /**
     * 挂断
     */
    override fun hangUp() {
        Logger.d("hangUp() called")

        //告诉对方挂断呼叫
        if (isActive()) {
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpMsg().toJsonString())
        }
        if (phoneSM.state is PhoneState.ACalling ||phoneSM.state is PhoneState.Speaking) {
            if (transAndCheck(Event.HangUp)) {
                rtcEngine.leaveChannel()
            }
        }
        if (phoneSM.state is PhoneState.BRinging) {
            if (transAndCheck(Event.BReject)) {
                rtcEngine.leaveChannel()
            }
        }
    }

    /**
     * 被告知挂断
     */
    override fun hangUp2() {
        Logger.d("hangUp2() called")

        if (transAndCheck(Event.BeHangUped)) {
            rtcEngine.leaveChannel()
        }
    }
    fun hangUp3() {
        Logger.d("hangUp3() called")
        if (transAndCheck(Event.ARejectedAuto)) {
            rtcEngine.leaveChannel()
        }
    }

    override fun afterHangUp() {
        netPhone.testCase = 0
    }


    fun onBJoinChannelSuccess(channel: String?) {
        transAndCheck(Event.BJoin1)
        netPhone.sendPeerMessage(remoteAccount,
                PhoneMsg.createAcceptMsg(channel).toJsonString())
    }

    fun onAJoinChannelSuccess() {
        transAndCheck(Event.AJoin2)
        netPhone.sendPeerMessage(remoteAccount,
                PhoneMsg.createJoinedMsg().toJsonString())
    }

    private val speakTime: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000).toInt()


//    /**
//     * 主叫(动作)：        拨打                                 进入2                             挂断
//     * 主叫(状态)：初始 -------------------------连接中(呼叫中)-------------------------------接通---------  初始
//     * 被叫(状态)：初始 ------------- 响铃--------连接中(接听中)---------连接上----------------接通---------  初始
//     * 被叫(动作)：            来电         接听                进入1               进入3          挂断
//     */
    //状态切换，及切换成功后的回调。
    val phoneSM = StateMachine.create<PhoneState, Event, SideEffect> {
        initialState(PhoneState.Idle)

        state<PhoneState.Idle> {
            on<Event.ADial> {
                Logger.d("主叫：拔打 --->")
                transitionTo(PhoneState.ACalling, SideEffect.LogADial)
            }
            on<Event.BReceive> {
                Logger.d("被叫：来电 --->")
                transitionTo(PhoneState.BRinging, SideEffect.LogBReceive)
            }
        }
        state<PhoneState.ACalling> {
            on<Event.HangUp> {
                Logger.d("主叫：<--- 主动结束通话-未接通")
                transitionTo(PhoneState.Idle, SideEffect.LogHangUp)
            }
            on<Event.ARejected> {
                Logger.d("主叫：<--- 被拒接")
                transitionTo(PhoneState.Idle, SideEffect.LogARejected)
            }
            on<Event.ARejectedAuto> {
                Logger.d("主叫：<--- 占线")
                transitionTo(PhoneState.Idle, SideEffect.LogARejectedAuto)
            }

            //被叫加入房间后，主叫再加入房间。主叫加入房间后，直接从PhoneState.CONNECTING 到 State.SPEAKING 跳过 State.CONNECTED
            on<Event.AJoin2> {
                Logger.d("主叫：AJoin2加入房间-可通话状态 --->")
                transitionTo(PhoneState.Speaking, SideEffect.LogAJoin2)
            }
        }
        state<PhoneState.Speaking> {
            on<Event.HangUp> {
                Logger.d("主动挂断 --->")
                transitionTo(PhoneState.Idle, SideEffect.LogHangUp)
            }
            on<Event.BeHangUped> {
                Logger.d("对方挂断 --->")
                transitionTo(PhoneState.Idle, SideEffect.LogBeHangUped)
            }
        }
        state<PhoneState.BRinging> {
            on<Event.BReject> {
                Logger.d("被叫：<--- 拒接")
                transitionTo(PhoneState.Idle, SideEffect.LogBReject)
            }
            on<Event.BeHangUped> {
                Logger.d("对方挂断 --->")
                transitionTo(PhoneState.Idle, SideEffect.LogBeHangUped)
            }
            on<Event.BAccept> {
                Logger.d("被叫：接听 --->")
                transitionTo(PhoneState.BAccepting, SideEffect.LogBAccept)
            }
        }
        state<PhoneState.BAccepting> {
            on<Event.BJoin1> {
                //（等待主叫加入房间，才可以通话）
                Logger.d("被叫：BJoin1进入房间 --->")
                transitionTo(PhoneState.BJoined, SideEffect.LogBJoin1)
            }
        }
        state<PhoneState.BJoined> {
            on<Event.BJoin3> {
                //（等待主叫加入房间，才可以通话）
                Logger.d("被叫：BJoin3可通话状态 --->")
                transitionTo(PhoneState.Speaking, SideEffect.LogBJoin3)
            }
        }
        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            when (validTransition.sideEffect) {
                SideEffect.LogADial -> {
                    netPhone.listener.onHostDial(dialBean)
                }
                SideEffect.LogARejected -> {
                    netPhone.listener.onHangUp(false, 0)
                }
                SideEffect.LogARejectedAuto -> {
                    netPhone.listener.onHangUp(false, 0)
                }
                SideEffect.LogAJoin2 -> {
                    rtcEngine.setEnableSpeakerphone(false)
                    startTime = System.currentTimeMillis()
                    netPhone.listener.onReadyToSpeak(remoteAccount)
                }

                SideEffect.LogBReceive -> {
                    startMediaPlayer()
                    netPhone.listener.onClientReceiveDial(remoteAccount, dialBean)
                }
                SideEffect.LogBReject -> {
                    stopMediaPlayer()
                    netPhone.listener.onHangUp(false, 0)
                }
                SideEffect.LogBJoin1 -> {
                    stopMediaPlayer()
                }
                SideEffect.LogBJoin3 -> {
                    rtcEngine.setEnableSpeakerphone(false)
                    startTime = System.currentTimeMillis()
                    netPhone.listener.onReadyToSpeak(remoteAccount)
                }
                SideEffect.LogHangUp -> {
                    netPhone.listener.onHangUp(true, speakTime)
                }
                SideEffect.LogBeHangUped -> {
                    stopMediaPlayer()
                    netPhone.listener.onRemoteHangUp(true, speakTime, remoteAccount)
                }
            }
        }
    }


    sealed class Event {
        /** 拨打 */
        object ADial : Event()
        /** 被拒接 */
        object ARejected : Event()
        /** 占线 */
        object ARejectedAuto : Event()
        /** 主叫进入房间 */
        object AJoin2 : Event()

        /** 来电话了 */
        object BReceive : Event()
        /** 拒接 */
        object BReject : Event()
        /** 接听 */
        object BAccept : Event()
        /** 被叫进入房间 */
        object BJoin1 : Event()
        /** 确认主叫已进入房间 */
        object BJoin3 : Event()

        /** 主动结束通话 */
        object HangUp : Event()
        /** 对方结束通话 */
        object BeHangUped : Event()
    }

    sealed class SideEffect() {
        object LogADial : SideEffect()
        object LogARejected : SideEffect()
        object LogARejectedAuto : SideEffect()
        object LogAJoin2 : SideEffect()


        object LogBReceive : SideEffect()
        object LogBReject : SideEffect()
        object LogBAccept : SideEffect()
        object LogBJoin1 : SideEffect()
        object LogBJoin3 : SideEffect()

        //接通后挂断
        object LogHangUp : SideEffect()

        //接通后被挂断
        object LogBeHangUped : SideEffect()
    }



    private fun startMediaPlayer() {
        if (NetPhone.instance.testCase > 0) {
            //单元测试时不开启声音
            return
        }

        try {
            val fileDescriptor = netPhone.mContext!!.assets.openFd("voice.mp3")
            mMediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
            mMediaPlayer.prepare()
        } catch (e: IOException) {
            Logger.e(e.message)
        }

        mMediaPlayer.start()
        mMediaPlayer.setOnCompletionListener { mediaPlayer: MediaPlayer? ->
            mMediaPlayer.start()
            mMediaPlayer.isLooping = true
        }
    }

    private fun stopMediaPlayer() {
        if (mMediaPlayer.isPlaying) {
            mMediaPlayer.reset()
        }
    }
}