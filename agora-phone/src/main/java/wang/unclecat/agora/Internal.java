package wang.unclecat.agora;

/**
 * AGCalling和AGCalled公共实现
 *
 * @author 喵叔catuncle    19-12-28
 */
interface Internal {

    /**
     * 是否被激活(即非idle状态)
     */
    boolean isActive();

    /**
     * 当前状态
     */
    PhoneState getState();

    boolean transAndCheck(InternalImpl.Event event);
    /**
     * 挂断
     */
    void hangUp();


    /**
     * 被告知挂断
     */
    void hangUp2();


    /**
     * 挂断后更新状态
     */
    void afterHangUp();
}
