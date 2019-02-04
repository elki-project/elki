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

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

import it.unimi.dsi.fastutil.longs.LongArrayList;

/**
 * Parser for parsing one BitVector per line, bits separated by whitespace.
 * <p>
 * Several labels may be given per BitVector. A label must not be parseable as
 * Bit. Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @has - - - BitVector
 */
@Title("Bit Vector Label Parser")
@Description("Parses the following format of lines:\n" + //
    "A single line provides a single BitVector. Bits are separated by whitespace. " + //
    "Any substring not containing whitespace is tried to be read as Bit. " + //
    "If this fails, it will be appended to a label. " + //
    "(Thus, any label must not be parseable as Bit.) " + //
    "Empty lines and lines beginning with \"#\" will be ignored.")
@Alias("de.lmu.ifi.dbs.elki.parser.BitVectorLabelParser")
public class BitVectorLabelParser extends NumberVectorLabelParser<BitVector> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(BitVectorLabelParser.class);

  /**
   * Buffer, will be reused.
   */
  LongArrayList buf = new LongArrayList();

  /**
   * Constructor.
   *
   * @param format Input format
   */
  public BitVectorLabelParser(CSVReaderFormat format) {
    super(format, null, BitVector.FACTORY);
  }

  @Override
  protected boolean parseLineInternal() {
    int curdim = 0;
    for(; tokenizer.valid(); tokenizer.advance()) {
      try {
        final int word = curdim >>> 6;
        final int off = curdim & 0x3F;
        if(word >= buf.size()) { // Ensure size.
          buf.add(0L);
        }
        if(tokenizer.getIntBase10() > 0) {
          buf.set(word, buf.getLong(word) | (1L << off));
        }
        ++curdim;
      }
      catch(NumberFormatException e) {
        labels.add(tokenizer.getSubstring());
      }
    }
    if(curdim == 0) { // Maybe a label row
      return false;
    }

    curvec = new BitVector(buf.toLongArray(), curdim);
    curlbl = LabelList.make(labels);
    buf.clear();
    labels.clear();
    return true;
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
    protected BitVectorLabelParser makeInstance() {
      return new BitVectorLabelParser(format);
    }
  }
}
