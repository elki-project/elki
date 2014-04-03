package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.datasource.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a parser for parsing one distance value per line.
 * <p/>
 * A line must have the following format: id1 id2 distanceValue, where id1 and
 * id2 are integers representing the two ids belonging to the distance value.
 * Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 */
@Title("Number Distance Parser")
@Description("Parser for the following line format:\n" + "id1 id2 distanceValue, where id1 and is2 are integers representing the two ids belonging to the distance value.\n" + " The ids and the distance value are separated by whitespace. Empty lines and lines beginning with \"#\" will be ignored.")
public class NumberDistanceParser extends AbstractParser implements DistanceParser {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(NumberDistanceParser.class);

  /**
   * Constructor.
   * 
   * @param colSep Column separator pattern
   * @param quoteChars Quote characters
   * @param comment Comment pattern
   */
  public NumberDistanceParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
  }

  @Override
  public void parse(InputStream in, DistanceCacheWriter cache) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Parsing distance matrix", LOG) : null;
    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        LOG.incrementProcessed(prog);
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        tokenizer.initialize(line, 0, lengthWithoutLinefeed(line));

        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }
        DBID id1, id2;
        try {
          id1 = DBIDUtil.importInteger((int) tokenizer.getLongBase10());
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ": id1 is no integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }

        try {
          id2 = DBIDUtil.importInteger((int) tokenizer.getLongBase10());
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ": id2 is no integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }

        try {
          double distance = tokenizer.getDouble();
          cache.put(id1, id2, distance);
          ids.add(id1);
          ids.add(id2);
        }
        catch(IllegalArgumentException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ":" + e.getMessage(), e);
        }
        tokenizer.advance();
        if(tokenizer.valid()) {
          throw new IllegalArgumentException("More than three values in line " + lineNumber);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    LOG.setCompleted(prog);

    // check if all distance values are specified
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        if(DBIDUtil.compare(iter2, iter) <= 0) {
          continue;
        }
        if(!cache.containsKey(iter, iter2)) {
          throw new IllegalArgumentException("Distance value for " + DBIDUtil.toString(iter) + " - " + DBIDUtil.toString(iter2) + " is missing!");
        }
      }
    }
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
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected NumberDistanceParser makeInstance() {
      return new NumberDistanceParser(colSep, quoteChars, comment);
    }
  }
}
