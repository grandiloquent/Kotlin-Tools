
package psycho.euphoria.common.util;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}
