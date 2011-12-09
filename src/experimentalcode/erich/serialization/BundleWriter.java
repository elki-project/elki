package experimentalcode.erich.serialization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource.Event;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Write an object bundle stream to a file channel.
 * 
 * Bundles that add new columns are not supported.
 * 
 * @author Erich Schubert
 */
public class BundleWriter {
  /**
   * Initial buffer size
   */
  private static final int INITIAL_BUFFER = 4096;

  /**
   * Random magic number
   */
  public static final int MAGIC = 0xa8123b12;

  public void writeBundleStream(BundleStreamSource source, FileChannel output) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(INITIAL_BUFFER);

    ByteBufferSerializer<Object>[] serializers = null;
    loop: while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        break loop;
      case META_CHANGED:
        if(serializers != null) {
          throw new AbortException("Meta changes are not supported, once the block header has been written.");
        }
        break;
      case NEXT_OBJECT:
        if(serializers == null) {
          serializers = writeHeader(source, buffer, output);
        }
        for(int i = 0; i < serializers.length; i++) {
          int size = serializers[i].getByteSize(source.data(i));
          buffer = ensureBuffer(size, buffer, output);
          serializers[i].toByteBuffer(buffer, source.data(i));
        }
        break;
      default:
        LoggingUtil.warning("Unknown event: " + ev);
      }
    }
    if(buffer.position() > 0) {
      flushBuffer(buffer, output);
    }
  }

  private void flushBuffer(ByteBuffer buffer, FileChannel output) throws IOException {
    buffer.flip();
    output.write(buffer);
    buffer.flip();
    buffer.limit(buffer.capacity());
  }

  private ByteBuffer ensureBuffer(int size, ByteBuffer buffer, FileChannel output) throws IOException {
    if(buffer.remaining() >= size) {
      return buffer;
    }
    flushBuffer(buffer, output);
    if(buffer.remaining() >= size) {
      return buffer;
    }
    // Aggressively grow the buffer
    return ByteBuffer.allocateDirect(buffer.capacity() + size);
  }

  private ByteBufferSerializer<Object>[] writeHeader(BundleStreamSource source, ByteBuffer buffer, FileChannel output) throws IOException {
    final BundleMeta meta = source.getMeta();
    final int nummeta = meta.size();
    @SuppressWarnings("unchecked")
    final ByteBufferSerializer<Object>[] serializers = new ByteBufferSerializer[nummeta];
    // Write our magic ID first.
    assert (buffer.position() == 0) : "Buffer is supposed to be at 0.";
    buffer.putInt(MAGIC);
    // Write the number of metas next
    buffer.putInt(nummeta);
    for (int i = 0; i < nummeta; i++) {
      SimpleTypeInformation<?> type = meta.get(i);
      @SuppressWarnings("unchecked")
      ByteBufferSerializer<Object> ser = (ByteBufferSerializer<Object>) TypeSerializerRegistry.getSerializerForType(type);
      if (ser == null) {
        throw new AbortException("Cannot serialize - no serializer found for type: "+type.toString());
      }
      serializers[i] = ser;
    }
    return serializers;
  }
}