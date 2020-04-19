package wang.unclecat.agora

import io.agora.rtc.RtcEngine
import wang.unclecat.agora.utils.Logger

/**
 * 被叫
 *
 * @author 喵叔catuncle    19-12-28
 */
internal class CalledSide(private val netPhone: NetPhone, private val mRtcEngine: RtcEngine,
                          private val internal: InternalImpl) : Internal by internal {

    /**
     * 来电
     *
     * @param remoteAccount 主叫方id
     */
    fun receive(remoteAccount: String, dialBean: DialBean) {
        Logger.d("receive() called with: remoteAccount = [$remoteAccount]")
        if (internal.isActive) {
            Logger.d("告诉主叫：占线")
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpAutoMsg().toJsonString())
        } else {
            internal.remoteAccount = remoteAccount
            internal.dialBean = dialBean
            internal.phoneSM.transition(InternalImpl.Event.OnReceive)
        }
    }

    /**
     * 接听
     */
    fun accept() {
        Logger.d("accept()")
        if (internal.remoteAccount != null) {
            internal.phoneSM.transition(InternalImpl.Event.OnAccept)
            netPhone.joinChannel(createChannel())
        }
    }

    fun handleOnJoinChannelSuccess(channel: String?) {
        netPhone.sendPeerMessage(internal.remoteAccount, PhoneMsg.createAcceptMsg(channel).toJsonString())
        internal.phoneSM.transition(InternalImpl.Event.OnJoin1)
    }

    /**
     * 触发Join3，即将进入通话状态
     */
    fun join3() {
        internal.phoneSM.transition(InternalImpl.Event.OnJoin3)
    }

    // TODO: by catuncle 19-12-24 模拟生成房间号
    private fun createChannel(): String {
        return "channel" + System.currentTimeMillis()
    }

}