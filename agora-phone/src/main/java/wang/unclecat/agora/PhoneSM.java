package wang.unclecat.agora;

import android.os.Handler;
import android.util.Log;

import org.squirrelframework.foundation.fsm.AnonymousAction;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import lombok.experimental.Delegate;
import wang.unclecat.agora.utils.ObservableSupport;

/**
 * 电话状态机
 *
 * @author 喵叔catuncle    20-1-5
 */
class PhoneSM extends
        AbstractStateMachine<PhoneSM, PhoneState, PhoneSM.Event, DialBean> {


    @Delegate
    private final ObservableSupport observableSupport =
            new ObservableSupport();


    public static PhoneSM create() {
        StateMachineBuilder<PhoneSM, PhoneState, Event, DialBean> builder =
                StateMachineBuilderFactory.create(PhoneSM.class, PhoneState.class, Event.class, DialBean.class);

        final Handler handler = new Handler();
        //case 1: 拔打后接通
        builder.externalTransition().from(PhoneState.IDLE).to(PhoneState.CONNECTING).on(Event.Dial).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "拔打");
            }
        });//拔打、来电
        builder.externalTransition().from(PhoneState.IDLE).to(PhoneState.RINGING).on(Event.Receive).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "来电");
            }
        });//拔打、来电
        builder.externalTransition().from(PhoneState.RINGING).to(PhoneState.CONNECTING).on(Event.Accept).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "接听");
            }
        });//接听、对方接听

        //第一步：被叫加入房间（等待主叫加入房间，才可以通话）
        builder.externalTransition().from(PhoneState.CONNECTING).to(PhoneState.CONNECTED).on(Event.Join1).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "1. 被叫加入房间");
            }
        });
        //第二步：主叫加入房间
        //被叫加入房间后，主叫再加入房间。主叫加入房间后，直接从PhoneState.CONNECTING 到 PhoneState.SPEAKING 跳过 PhoneState.CONNECTED
        builder.externalTransition().from(PhoneState.CONNECTING).to(PhoneState.SPEAKING).on(Event.Join2).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "2. 自动进入-可通话");
            }
        });

        //第三步：被叫等待主叫加入房间
        builder.externalTransition().from(PhoneState.CONNECTED).to(PhoneState.SPEAKING).on(Event.Join3).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "3. 可通话");
            }
        });

        builder.externalTransition().from(PhoneState.SPEAKING).to(PhoneState.IDLE).on(Event.HangUp).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "挂断");
            }
        });//挂断、对方挂断

        //呼叫失败(拒接、取消拔打、占线。同步电话状态：
        //case 2: 取消拔打、拒接
        builder.externalTransition().from(PhoneState.RINGING).to(PhoneState.IDLE).on(Event.HangUp).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "取消拔打");
            }
        });
        builder.externalTransition().from(PhoneState.CONNECTING).to(PhoneState.IDLE).on(Event.HangUp).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "取消拔打");
            }
        });

        //case 4: 占线
        builder.externalTransition().from(PhoneState.CONNECTING).to(PhoneState.IDLE).on(Event.HangUp_Auto).perform(new FireEvent(handler) {
            @Override
            protected void doExecute(PhoneSM stateMachine) {
                Log.d("PhoneSM", "占线");
            }
        });

        return builder.newStateMachine(PhoneState.IDLE);
    }

    private static abstract class FireEvent extends AnonymousAction<PhoneSM, PhoneState, Event, DialBean> {

        private final Handler handler;

        public FireEvent(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void execute(PhoneState from, PhoneState to, Event event, DialBean context, PhoneSM stateMachine) {

            handler.post(() -> {
                stateMachine.notifyX(from, to, event, context);
                Log.d("PhoneSM", "execute() called with: from = [" + from + "], to = [" + to + "], event = [" + event + "], context = [" + context + "], stateMachine = [" + stateMachine + "]");
                doExecute(stateMachine);
            });
        }

        @Override
        public boolean isAsync() {
            return true;
        }

        protected abstract void doExecute(PhoneSM stateMachine);
    }


    /**
     * 主叫(动作)：        拨打                           进入2                             挂断
     * 主叫(状态)：初始 -------------------------连接中-------------------------------接通---------  初始
     * 被叫(状态)：初始 ------------- 响铃--------连接中---------连接上-----------------接通---------  初始
     * 被叫(动作)：            来电         接听           进入1               进入3          挂断
     */
    public enum Event {
        Dial,
        Receive,
        Accept,
        Join1,
        Join2,
        Join3,
        HangUp,//挂断
        HangUp_Auto,//占线
    }


}
