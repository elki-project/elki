package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IConstantSizeByteBufferSerializer<T> extends ByteBufferSerializer<T> {

  /**
   * Returns the constant size of an element 
   * @return
   */
  public int getConstantByteSize();
  
}
