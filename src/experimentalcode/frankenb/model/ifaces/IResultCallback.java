package experimentalcode.frankenb.model.ifaces;

public interface IResultCallback<T, R> {

  public R call(T value);
  
}
