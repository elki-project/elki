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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformationSerializer;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Read an ELKI bundle file into a data stream.
 * 
 * TODO: resize buffer when necessary?
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @assoc - reads - MappedByteBuffer
 * @assoc - reads - FileChannel
 */
public class BundleReader implements BundleStreamSource {
  /**
   * Magic number, shared with {@link BundleReader}.
   */
  public static final int MAGIC = BundleWriter.MAGIC;

  /**
   * The stream buffer.
   */
  MappedByteBuffer buffer = null;

  /**
   * Bundle metadata.
   */
  BundleMeta meta = null;

  /**
   * Input channel.
   */
  FileChannel input = null;

  /**
   * Serializers to use.
   */
  ByteBufferSerializer<?>[] sers;

  /**
   * Current object.
   */
  Object[] data;

  /**
   * Whether or not we have DBIDs.
   */
  boolean hasids = false;

  /**
   * Constructor.
   * 
   * @param buffer Input buffer
   */
  public BundleReader(MappedByteBuffer buffer) {
    super();
    this.buffer = buffer;
    this.input = null;
  }

  /**
   * Constructor.
   * 
   * @param input Input channel
   */
  public BundleReader(FileChannel input) {
    super();
    this.input = input;
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      openBuffer();
      readMeta();
    }
    return meta;
  }

  /**
   * Map the input file.
   */
  void openBuffer() {
    if(buffer == null) {
      try {
        buffer = input.map(MapMode.READ_ONLY, 0, input.size());
      }
      catch(IOException e) {
        throw new AbortException("Cannot map input bundle.", e);
      }
    }
  }

  /**
   * Read the metadata.
   */
  void readMeta() {
    final int check = buffer.getInt();
    if(check != MAGIC) {
      throw new AbortException("File does not start with expected magic.");
    }
    final int nummeta = buffer.getInt();
    assert (nummeta > 0) : "Empty bundle?";
    meta = new BundleMeta(nummeta);
    sers = new ByteBufferSerializer<?>[nummeta];
    data = new Object[nummeta];
    for(int i = 0; i < nummeta; i++) {
      try {
        @SuppressWarnings("unchecked")
        SimpleTypeInformation<? extends Object> type = (SimpleTypeInformation<? extends Object>) TypeInformationSerializer.STATIC.fromByteBuffer(buffer);
        sers[i] = type.getSerializer();
        if(i == 0 && DBID.class.isAssignableFrom(type.getRestrictionClass())) {
          hasids = true;
        }
        else {
          meta.add(type);
        }
      }
      catch(UnsupportedOperationException e) {
        throw new AbortException("Deserialization failed: " + e.getMessage(), e);
      }
      catch(IOException e) {
        throw new AbortException("IO error", e);
      }
    }
  }

  /**
   * Read an object.
   */
  void readObject() {
    for(int i = 0; i < sers.length; ++i) {
      try {
        data[i] = sers[i].fromByteBuffer(buffer);
      }
      catch(UnsupportedOperationException e) {
        throw new AbortException("Deserialization failed.", e);
      }
      catch(IOException e) {
        throw new AbortException("IO error", e);
      }
    }
  }

  @Override
  public Event nextEvent() {
    // Send initial meta
    if(meta == null) {
      return Event.META_CHANGED;
    }
    if(buffer.remaining() == 0) {
      ByteArrayUtil.unmapByteBuffer(buffer);
      return Event.END_OF_STREAM;
    }
    readObject();
    return Event.NEXT_OBJECT;
  }

  @Override
  public Object data(int rnum) {
    return data[!hasids ? rnum : (rnum + 1)];
  }

  @Override
  public boolean hasDBIDs() {
    return hasids;
  }

  @Override
  public boolean assignDBID(DBIDVar var) {
    if(!hasids) {
      return false;
    }
    var.set((DBID) data[0]);
    return true;
  }

  @Override
  public MultipleObjectsBundle asMultipleObjectsBundle() {
    return MultipleObjectsBundle.fromStream(this);
  }
}
