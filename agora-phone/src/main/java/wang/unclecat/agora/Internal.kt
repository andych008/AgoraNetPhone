package wang.unclecat.agora

/**
 * AGCalling和AGCalled公共实现
 *
 * @author 喵叔catuncle    19-12-28
 */
internal interface Internal {
    /**
     * 是否被激活(即非idle状态)
     */
    fun isActive(): Boolean?

    /**
     * 当前状态
     */
    fun state(): PhoneState

    fun transAndCheck(event: InternalImpl.Event): Boolean

    /**
     * 挂断
     */
    fun hangUp()

    /**
     * 被告知挂断
     */
    fun hangUp2()

    /**
     * 挂断后更新状态
     */
    fun afterHangUp()
}