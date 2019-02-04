/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformationSerializer;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Write an object bundle stream to a file channel.
 * 
 * Bundle streams that add new columns are not supported.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @assoc - reads - BundleStreamSource
 * @assoc - writes - WritableByteChannel
 */
public class BundleWriter {
  /**
   * Class logger for the bundle writer.
   */
  private static final Logging LOG = Logging.getLogger(BundleWriter.class);

  /**
   * Initial buffer size.
   */
  private static final int INITIAL_BUFFER = 4096;

  /**
   * Random magic number.
   */
  public static final int MAGIC = 0xa8123b12;

  /**
   * Write a bundle stream to a file output channel.
   * 
   * @param source Data source
   * @param output Output channel
   * @throws IOException on IO errors
   */
  public void writeBundleStream(BundleStreamSource source, WritableByteChannel output) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(INITIAL_BUFFER);

    DBIDVar var = DBIDUtil.newVar();
    ByteBufferSerializer<?>[] serializers = null;
    loop: while(true) {
      BundleStreamSource.Event ev = source.nextEvent();
      switch(ev){
      case NEXT_OBJECT:
        if(serializers == null) {
          serializers = writeHeader(source, buffer, output);
        }
        if(serializers[0] != null) {
          if(!source.assignDBID(var)) {
            throw new AbortException("An object did not have an DBID assigned.");
          }
          DBID id = DBIDUtil.deref(var);
          @SuppressWarnings("unchecked")
          ByteBufferSerializer<DBID> ser = (ByteBufferSerializer<DBID>) serializers[0];
          int size = ser.getByteSize(id);
          buffer = ensureBuffer(size, buffer, output);
          ser.toByteBuffer(buffer, id);
        }
        for(int i = 1, j = 0; i < serializers.length; ++i, ++j) {
          @SuppressWarnings("unchecked")
          ByteBufferSerializer<Object> ser = (ByteBufferSerializer<Object>) serializers[i];
          int size = ser.getByteSize(source.data(j));
          buffer = ensureBuffer(size, buffer, output);
          ser.toByteBuffer(buffer, source.data(j));
        }
        break; // switch
      case META_CHANGED:
        if(serializers != null) {
          throw new AbortException("Meta changes are not supported, once the block header has been written.");
        }
        break; // switch
      case END_OF_STREAM:
        break loop;
      default:
        LOG.warning("Unknown bundle stream event. API inconsistent? " + ev);
        break; // switch
      }
    }
    if(buffer.position() > 0) {
      flushBuffer(buffer, output);
    }
  }

  /**
   * Flush the current write buffer to disk.
   * 
   * @param buffer Buffer to write
   * @param output Output channel
   * @throws IOException on IO errors
   */
  private void flushBuffer(ByteBuffer buffer, WritableByteChannel output) throws IOException {
    buffer.flip();
    output.write(buffer);
    buffer.flip();
    buffer.limit(buffer.capacity());
  }

  /**
   * Ensure the buffer is large enough.
   * 
   * @param size Required size to add
   * @param buffer Existing buffer
   * @param output Output channel
   * @return Buffer, eventually resized
   * @throws IOException on IO errors
   */
  private ByteBuffer ensureBuffer(int size, ByteBuffer buffer, WritableByteChannel output) throws IOException {
    if(buffer.remaining() >= size) {
      return buffer;
    }
    flushBuffer(buffer, output);
    if(buffer.remaining() >= size) {
      return buffer;
    }
    // Aggressively grow the buffer
    return ByteBuffer.allocateDirect(Math.max(buffer.capacity() << 1, buffer.capacity() + size));
  }

  /**
   * Write the header for the given stream to the stream.
   * 
   * @param source Bundle stream
   * @param buffer Buffer to use for writing
   * @param output Output channel
   * @return Array of serializers
   * @throws IOException on IO errors
   */
  private ByteBufferSerializer<?>[] writeHeader(BundleStreamSource source, ByteBuffer buffer, WritableByteChannel output) throws IOException {
    final BundleMeta meta = source.getMeta();
    final int nummeta = meta.size();
    @SuppressWarnings("rawtypes")
    final ByteBufferSerializer[] serializers = new ByteBufferSerializer[1 + nummeta];
    // Write our magic ID first.
    assert (buffer.position() == 0) : "Buffer is supposed to be at 0.";
    buffer.putInt(MAGIC);
    // Write the number of metas next.
    // For compatibility with earlier versions, treat DBIDs as extra type
    if(source.hasDBIDs()) {
      buffer.putInt(1 + nummeta);
      ByteBufferSerializer<DBID> ser = DBIDFactory.FACTORY.getDBIDSerializer();
      TypeInformationSerializer.STATIC.toByteBuffer(buffer, new SimpleTypeInformation<>(DBID.class, ser));
      serializers[0] = ser;
    }
    else {
      buffer.putInt(nummeta);
    }
    for(int i = 0; i < nummeta; i++) {
      SimpleTypeInformation<?> type = meta.get(i);
      ByteBufferSerializer<?> ser = type.getSerializer();
      if(ser == null) {
        throw new AbortException("Cannot serialize - no serializer found for type: " + type.toString());
      }
      TypeInformationSerializer.STATIC.toByteBuffer(buffer, type);
      serializers[i + 1] = ser;
    }
    return serializers;
  }
}
