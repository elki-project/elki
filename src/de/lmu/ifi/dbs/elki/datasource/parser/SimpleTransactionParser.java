package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;

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
 * 
 * @apiviz.has BitVector
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
  TObjectIntMap<String> keymap;

  /**
   * Buffer reader.
   */
  private BufferedReader reader;

  /**
   * Metadata.
   */
  protected BundleMeta meta;

  /**
   * Event to report next.
   */
  Event nextevent;

  /**
   * Current line number, for error reporting.
   */
  int lineNumber;

  /**
   * Current vector.
   */
  BitVector curvec;

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quote character
   * @param comment Comment pattern
   */
  public SimpleTransactionParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
    keymap = new TObjectIntHashMap<>(1001, .5f, -1);
  }

  @Override
  public void initStream(InputStream in) {
    reader = new BufferedReader(new InputStreamReader(in));
    lineNumber = 0;
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
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        // Don't reuse bitsets, will not be copied by BitVector constructor.
        BitSet bits = new BitSet(keymap.size());
        for(tokenizer.initialize(line, 0, lengthWithoutLinefeed(line)); tokenizer.valid(); tokenizer.advance()) {
          String token = tokenizer.getSubstring();
          int t = keymap.get(token);
          if(t < 0) {
            t = keymap.size();
            keymap.put(token, t);
          }
          bits.set(t);
        }
        curvec = new BitVector(bits, keymap.size());
        return Event.NEXT_OBJECT;
      }
      reader.close();
      reader = null;
      nextevent = Event.END_OF_STREAM;
      // Construct final metadata:
      meta = new BundleMeta(1);
      String[] colnames = new String[keymap.size()];
      for(TObjectIntIterator<String> iter = keymap.iterator(); iter.hasNext();) {
        iter.advance();
        colnames[iter.value()] = iter.key();
      }
      meta.add(new VectorFieldTypeInformation<>(BitVector.FACTORY, colnames.length, colnames));
      return Event.META_CHANGED; // Force a final meta update.
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
  }

  @Override
  public Object data(int rnum) {
    if(rnum == 0) {
      return curvec;
    }
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      meta = new BundleMeta(1);
      meta.add(new VectorTypeInformation<>(BitVector.class));
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractStreamingParser.Parameterizer {
    @Override
    protected SimpleTransactionParser makeInstance() {
      return new SimpleTransactionParser(colSep, quoteChars, comment);
    }
  }
}
