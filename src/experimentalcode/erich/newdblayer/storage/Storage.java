package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.DBID;

public interface Storage<T> {
  public T get(DBID id);
}
