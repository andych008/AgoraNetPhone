package wang.unclecat.agora.utils;

public interface ObservableInter {

    void addObserver(Object object, String methodName, Class... args);

    //简化处理，只判断object(多数情况足够)
    void removeObserver(Object object);

    void removeObservers();

    void notifyX(Object... params);
}
