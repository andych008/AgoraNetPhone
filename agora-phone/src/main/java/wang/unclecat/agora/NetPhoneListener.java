package wang.unclecat.agora;


/**
 * NetPhone回调
 *
 * @author 喵叔catuncle    19-2-28
 */
public interface NetPhoneListener {
    /**
     * 主叫拔打成功(成功拔打出去，对方还未接听)
     * @param dialBean
     */
    void onHostDial(DialBean dialBean);

    /**
     * 被叫来电
     *
     * @param remoteAccount 主叫方id
     * @param bean          主叫方传来的参数（主叫方的头像、性别等信息）
     */
    void onClientReceiveDial(String remoteAccount, DialBean bean);

    /**
     * 连接成功-可以通话
     *
     * @param remoteAccount 被叫方id
     */
    void onReadyToSpeak(String remoteAccount);

    /**
     * 计时器回调
     *  @param elapsedTime 已通话时长(单位秒)
     *
     */
    void onTick(int elapsedTime);

    /**
     * 主动挂断
     *
     * @param connected     true:接通后挂断  false:取消呼叫|拒接
     * @param totalDuration 通话时长（秒）
     */
    void onHangUp(boolean connected, int totalDuration);

    /**
     * 对方挂断
     *
     * @param connected     true:接通后挂断  false:取消呼叫|拒接
     * @param totalDuration 通话时长（秒）
     * @param remoteAccount 对方的id
     */
    void onRemoteHangUp(boolean connected, int totalDuration, String remoteAccount);


}
