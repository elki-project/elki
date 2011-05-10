package experimentalcode.frankenb.model;

import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

import experimentalcode.frankenb.model.ifaces.IConstantSizeByteBufferSerializer;

/**
 * Serializer for integer objects
 * 
 * @author Erich Schubert
 *         Florian Frankenberger
 */
public class ConstantSizeIntegerDBIDSerializer implements IConstantSizeByteBufferSerializer<DBID> {
  
    /**
     * Constructor.
     */
    public ConstantSizeIntegerDBIDSerializer() {
      super();
    }

    @Override
    public DBID fromByteBuffer(ByteBuffer buffer) {
      return DBIDUtil.importInteger(buffer.getInt());
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DBID obj) {
      buffer.putInt(obj.getIntegerID());
    }

    @Override
    public int getByteSize(DBID object) {
      return Integer.SIZE / 8;
    }

    @Override
    public int getConstantByteSize() {
      return Integer.SIZE / 8;
    }
}
