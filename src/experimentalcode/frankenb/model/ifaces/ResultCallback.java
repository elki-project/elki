package experimentalcode.frankenb.model.ifaces;

public interface ResultCallback<T, R> {

  public R call(T value);
  
}
