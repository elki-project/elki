/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.datasource.parser.CSVReaderFormat;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parser for parsing one distance value per line.
 *
 * A line must have the following format: {@code id1 id2 distanceValue}, where
 * id1 and id2 are integers starting at 0 representing the two ids belonging to
 * the distance value. Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.2
 *
 * @apiviz.uses CSVReaderFormat
 * @apiviz.composedOf TokenizedReader
 * @apiviz.composedOf Tokenizer
 * @apiviz.uses DistanceCacheWriter
 */
@Title("Number Distance Parser")
@Description("Parser for the following line format:\n" //
    + "id1 id2 distanceValue, where id1 and is2 are integers starting at 0 representing the two ids belonging to the distance value.\n" //
    + "The ids and the distance value are separated by whitespace. Empty lines and lines beginning with \"#\" will be ignored.")
@Alias({ "de.lmu.ifi.dbs.elki.datasource.parser.NumberDistanceParser", //
    "de.lmu.ifi.dbs.elki.distance.distancefunction.external.NumberDistanceParser", //
    "de.lmu.ifi.dbs.elki.parser.NumberDistanceParser" })
public class AsciiDistanceParser implements DistanceParser {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AsciiDistanceParser.class);

  /**
   * Tokenized reader.
   */
  protected TokenizedReader reader;

  /**
   * Tokenizer.
   */
  protected Tokenizer tokenizer;

  /**
   * Constructor.
   *
   * @param format Input format
   */
  public AsciiDistanceParser(CSVReaderFormat format) {
    super();
    this.reader = format.makeReader();
    this.tokenizer = reader.getTokenizer();
  }

  @Override
  public void parse(InputStream in, DistanceCacheWriter cache) {
    reader.reset(in);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Parsing distance matrix", LOG) : null;
    try {
      int id1, id2;
      while(reader.nextLineExceptComments()) {
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + reader.getLineNumber());
        }
        try {
          id1 = tokenizer.getIntBase10();
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + reader.getLineNumber() + ": id1 is not an integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + reader.getLineNumber());
        }

        try {
          id2 = tokenizer.getIntBase10();
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + reader.getLineNumber() + ": id2 is not an integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + reader.getLineNumber());
        }

        try {
          cache.put(id1, id2, tokenizer.getDouble());
        }
        catch(IllegalArgumentException e) {
          throw new IllegalArgumentException("Error in line " + reader.getLineNumber() + ":" + e.getMessage(), e);
        }
        tokenizer.advance();
        if(tokenizer.valid()) {
          throw new IllegalArgumentException("More than three values in line " + reader.getLineNumber());
        }
        LOG.incrementProcessed(prog);
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + reader.getLineNumber() + ".");
    }

    LOG.setCompleted(prog);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Reader format.
     */
    protected CSVReaderFormat format;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      format = ClassGenericsUtil.parameterizeOrAbort(CSVReaderFormat.class, config);
    }

    @Override
    protected AsciiDistanceParser makeInstance() {
      return new AsciiDistanceParser(format);
    }
  }
}
