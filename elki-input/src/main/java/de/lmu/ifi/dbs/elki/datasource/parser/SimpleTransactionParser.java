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
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Simple parser for transactional data, such as market baskets.
 *
 * To keep the input format simple and readable, all tokens are assumed to be of
 * text and separated by whitespace, and each transaction is on a separate line.
 *
 * An example file containing two transactions looks like this
 *
 * <pre>
 * bread butter milk
 * paste tomato basil
 * </pre>
 *
 * TODO: add a parameter to e.g. use the first or last entry as labels instead
 * of tokens.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - BitVector
 */
public class SimpleTransactionParser extends AbstractStreamingParser {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SimpleTransactionParser.class);

  /**
   * Number of different terms observed.
   */
  int numterms;

  /**
   * Map.
   */
  Object2IntOpenHashMap<String> keymap;

  /**
   * Metadata.
   */
  protected BundleMeta meta;

  /**
   * Event to report next.
   */
  Event nextevent;

  /**
   * Current vector.
   */
  BitVector curvec;

  /**
   * Buffer, will be reused.
   */
  LongArrayList buf = new LongArrayList();

  /**
   * Constructor.
   *
   * @param format Input format
   */
  public SimpleTransactionParser(CSVReaderFormat format) {
    super(format);
    keymap = new Object2IntOpenHashMap<>();
    keymap.defaultReturnValue(-1);
  }

  @Override
  public void initStream(InputStream in) {
    super.initStream(in);
    nextevent = Event.META_CHANGED; // Initial event.
  }

  @Override
  public Event nextEvent() {
    if(nextevent != null) {
      Event ret = nextevent;
      nextevent = null;
      return ret;
    }
    try {
      while(reader.nextLineExceptComments()) {
        // Don't reuse bitsets, will not be copied by BitVector constructor.
        buf.clear();
        for(/* initialized by nextLineExceptComments() */; tokenizer.valid(); tokenizer.advance()) {
          String token = tokenizer.getSubstring();
          int t = keymap.getInt(token);
          if(t < 0) {
            t = keymap.size();
            keymap.put(token, t);
          }
          final int word = t >>> 6;
          final int off = t & 0x3F;
          while(word >= buf.size()) { // Ensure size.
            buf.add(0L);
          }
          buf.set(word, buf.getLong(word) | (1L << off));
        }
        curvec = new BitVector(buf.toLongArray(), keymap.size());
        return Event.NEXT_OBJECT;
      }
      nextevent = Event.END_OF_STREAM;
      // Construct final metadata:
      meta = new BundleMeta(1);
      String[] colnames = new String[keymap.size()];
      for(ObjectIterator<Object2IntMap.Entry<String>> iter = keymap.object2IntEntrySet().fastIterator(); iter.hasNext();) {
        Object2IntMap.Entry<String> entry = iter.next();
        colnames[entry.getIntValue()] = entry.getKey();
      }
      meta.add(new VectorFieldTypeInformation<>(BitVector.FACTORY, colnames.length, colnames));
      return Event.META_CHANGED; // Force a final meta update.
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + reader.getLineNumber() + ".");
    }
  }

  @Override
  public void cleanup() {
    super.cleanup();
    curvec = null;
  }

  @Override
  public Object data(int rnum) {
    if(rnum != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return curvec;
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      meta = new BundleMeta(1);
      meta.add(new VectorTypeInformation<>(BitVector.FACTORY, BitVector.SHORT_SERIALIZER, 0, numterms));
    }
    return meta;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractStreamingParser.Parameterizer {
    @Override
    protected SimpleTransactionParser makeInstance() {
      return new SimpleTransactionParser(format);
    }
  }
}
