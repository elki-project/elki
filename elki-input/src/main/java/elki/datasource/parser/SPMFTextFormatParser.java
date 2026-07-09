/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
package elki.datasource.parser;

import java.io.IOException;
import java.io.InputStream;

import elki.data.IntegerVector;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.bundle.BundleMeta;
import elki.logging.Logging;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Parser for the SPMF text input format.
 * <p>
 * The SPMF format uses {@code -2} to separate records (usually followed by a
 * newline). Either a newline or a {@code -2} token indicates a new record.
 * Empty records are silently ignored. Within a record, all non-separator
 * integers are buffered into an {@code IntegerVector} of variable length.
 * The value {@code -1} serves as a separator within the integer array (i.e.,
 * it is kept as a regular element, not interpreted as a record boundary).
 * <p>
 * This parser implements the streaming API, allowing filters to be composed
 * on top of it later.
 *
 * @author Erich Schubert
 */
public class SPMFTextFormatParser extends AbstractStreamingParser {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SPMFTextFormatParser.class);

  /**
   * Metadata, built lazily.
   */
  protected BundleMeta meta;

  /**
   * Event to report next.
   */
  protected Event nextevent;

  /**
   * Current vector being built or last produced.
   */
  protected IntegerVector curvec;

  /**
   * Buffer for integer values, reused across records.
   */
  protected IntArrayList buf = new IntArrayList();

  /**
   * Maximum dimensions seen so far (for metadata inference).
   */
  protected int maxdim;

  /**
   * Constructor.
   *
   * @param format Input format
   */
  public SPMFTextFormatParser(CSVReaderFormat format) {
    super(format);
  }

  @Override
  public void initStream(InputStream in) {
    super.initStream(in);
    nextevent = Event.META_CHANGED; // Initial event to force metadata emission.
    maxdim = 0;
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
        buf.clear();
        boolean hasAnyValue = false;
        for(; tokenizer.valid(); tokenizer.advance()) {
          final String tok = tokenizer.getSubstring();
          try {
            int val = Integer.parseInt(tok);
            if(val == -2) {
              // Record separator: emit current record if non-empty.
              if(hasAnyValue) {
                curvec = new IntegerVector(buf.toIntArray());
                maxdim = Math.max(maxdim, buf.size());
                return Event.NEXT_OBJECT;
              }
              // Empty record (only -2), skip silently.
              buf.clear();
              hasAnyValue = false;
            }
            else {
              buf.add(val);
              hasAnyValue = true;
            }
          }
          catch(NumberFormatException e) {
            // Skip non-integer tokens.
            continue;
          }
        }
        // End of line with buffered values: emit as a record.
        if(hasAnyValue) {
          curvec = new IntegerVector(buf.toIntArray());
          maxdim = Math.max(maxdim, buf.size());
          return Event.NEXT_OBJECT;
        }
      }
      // Stream exhausted: emit final metadata update if we saw any records.
      if(maxdim > 0) {
        nextevent = Event.END_OF_STREAM;
        return Event.META_CHANGED; // Final meta update with actual maxdim.
      }
      return Event.END_OF_STREAM;
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
      // Use range [0, maxdim] to indicate variable-length integers.
      // SHORT_SERIALIZER can handle any dimensionality up to Short.MAX_VALUE.
      meta = new BundleMeta(1);
      meta.add(new VectorFieldTypeInformation<>(IntegerVector.FACTORY, 0, maxdim, IntegerVector.SHORT_SERIALIZER));
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
   * @author Schubert
   */
  public static class Par extends AbstractStreamingParser.Par {
    @Override
    public SPMFTextFormatParser make() {
      return new SPMFTextFormatParser(format);
    }
  }
}
