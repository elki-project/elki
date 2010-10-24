/**
 * 
 */
package experimentalcode.frankenb.model;

import java.nio.ByteBuffer;

import experimentalcode.frankenb.model.ifaces.ConstantSizeByteBufferSerializer;

/**
 * Serializer for integer objects
 * 
 * @author Erich Schubert
 *         Florian Frankenberger
 */
public class ConstantSizeIntegerSerializer implements ConstantSizeByteBufferSerializer<Integer> {
  
    /**
     * Constructor.
     */
    public ConstantSizeIntegerSerializer() {
      super();
    }

    @Override
    public Integer fromByteBuffer(ByteBuffer buffer) {
      return buffer.getInt();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Integer obj) {
      buffer.putInt(obj);
    }

    @Override
    public int getByteSize(Integer object) {
      return Integer.SIZE / 8;
    }

    /* (non-Javadoc)
     * @see experimentalcode.frankenb.model.ifaces.ConstantSizeByteBufferSerializer#getConstantByteSize()
     */
    @Override
    public int getConstantByteSize() {
      return Integer.SIZE / 8;
    }
}
