package wang.unclecat.agora.utils;

import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通用的观察者模式，通过委托实现
 *
 * @author 喵叔catuncle    20-1-5
 */
public class ObservableSupport {

    private CopyOnWriteArrayList<Event> objects;

    public ObservableSupport() {
        objects = new CopyOnWriteArrayList<>();
    }

    public void addObserver(Object object, String methodName, Class... args) {
        objects.add(new Event(object, methodName, args));
    }

    //简化处理，只判断object(多数情况足够)
    public void removeObserver(Object object) {
        for (Event event : objects) {
            if (event.object == object) {
                objects.remove(event);
                break;
            }
        }
    }

    public void removeObservers() {
        objects.clear();
    }

    public void notifyX(Object... params) {
        try {
            for (Event event : objects) {
                event.invoke(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Event {
        Object object;

        private String methodName;

        private Class[] paramTypes;

        public Event(Object object, String method, Class... args) {
            this.object = object;
            this.methodName = method;
            this.paramTypes = args;
        }


        public void invoke(Object... params) throws Exception {
            Method method = object.getClass().getMethod(this.methodName, this.paramTypes);//判断是否存在这个函数
            if (null == method) {
                return;
            }
            method.invoke(this.object, params);//利用反射机制调用函数
        }
    }
}
