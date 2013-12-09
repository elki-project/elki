package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.datasource.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides a parser for parsing one distance value per line.
 * <p/>
 * A line must have the following format: id1 id2 distanceValue, where id1 and
 * id2 are integers representing the two ids belonging to the distance value.
 * Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has NumberDistance
 * 
 * @param <D> distance type
 */
@Title("Number Distance Parser")
@Description("Parser for the following line format:\n" + "id1 id2 distanceValue, where id1 and is2 are integers representing the two ids belonging to the distance value.\n" + " The ids and the distance value are separated by whitespace. Empty lines and lines beginning with \"#\" will be ignored.")
public class NumberDistanceParser<D extends NumberDistance<D, ?>> extends AbstractParser implements DistanceParser<D> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(NumberDistanceParser.class);

  /**
   * The distance function.
   */
  private final D distanceFactory;

  /**
   * Constructor.
   * 
   * @param colSep Column separator pattern
   * @param quoteChars Quote characters
   * @param comment Comment pattern
   * @param distanceFactory Distance factory to use
   */
  public NumberDistanceParser(Pattern colSep, String quoteChars, Pattern comment, D distanceFactory) {
    super(colSep, quoteChars, comment);
    this.distanceFactory = distanceFactory;
  }

  @Override
  public DistanceParsingResult<D> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Parsing distance matrix", LOG) : null;
    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    Map<DBIDPair, D> distanceCache = new HashMap<>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(prog != null) {
          prog.incrementProcessed(LOG);
        }
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
          final D distance;
          if(distanceFactory == DoubleDistance.FACTORY) {
            @SuppressWarnings("unchecked")
            D dist = (D) DoubleDistance.FACTORY.fromDouble(tokenizer.getDouble());
            distance = dist;
          }
          else {
            distance = distanceFactory.parseString(tokenizer.getSubstring());
          }
          tokenizer.advance();
          put(id1, id2, distance, distanceCache);
          ids.add(id1);
          ids.add(id2);
        }
        catch(IllegalArgumentException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ":" + e.getMessage(), e);
        }
        if(tokenizer.valid()) {
          throw new IllegalArgumentException("More than three values in line " + lineNumber);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    if(prog != null) {
      prog.setCompleted(LOG);
    }

    // check if all distance values are specified
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        if(DBIDUtil.compare(iter2, iter) <= 0) {
          continue;
        }
        if(!containsKey(iter, iter2, distanceCache)) {
          throw new IllegalArgumentException("Distance value for " + DBIDUtil.toString(iter) + " - " + DBIDUtil.toString(iter2) + " is missing!");
        }
      }
    }
    return new DistanceParsingResult<>(distanceCache);
  }

  /**
   * Puts the specified distance value for the given ids to the distance cache.
   * 
   * @param id1 the first id
   * @param id2 the second id
   * @param distance the distance value
   * @param cache the distance cache
   */
  private void put(DBID id1, DBID id2, D distance, Map<DBIDPair, D> cache) {
    // the smaller id is the first key
    if(DBIDUtil.compare(id1, id2) > 0) {
      put(id2, id1, distance, cache);
      return;
    }

    D oldDistance = cache.put(DBIDUtil.newPair(id1, id2), distance);

    if(oldDistance != null) {
      throw new IllegalArgumentException("Distance value for specified ids is already assigned!");
    }
  }

  /**
   * Returns <tt>true</tt> if the specified distance cache contains a distance
   * value for the specified ids.
   * 
   * @param id1 the first id
   * @param id2 the second id
   * @param cache the distance cache
   * @return <tt>true</tt> if this cache contains a distance value for the
   *         specified ids, false otherwise
   */
  public boolean containsKey(DBIDRef id1, DBIDRef id2, Map<DBIDPair, D> cache) {
    if(DBIDUtil.compare(id1, id2) > 0) {
      return containsKey(id2, id1, cache);
    }

    return cache.containsKey(DBIDUtil.newPair(id1, id2));
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
  public static class Parameterizer<D extends NumberDistance<D, ?>> extends AbstractParser.Parameterizer {
    /**
     * The distance function.
     */
    protected D distanceFactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<D> distFuncP = new ObjectParameter<>(DISTANCE_ID, Distance.class);
      if(config.grab(distFuncP)) {
        distanceFactory = distFuncP.instantiateClass(config);
      }
    }

    @Override
    protected NumberDistanceParser<D> makeInstance() {
      return new NumberDistanceParser<>(colSep, quoteChars, comment, distanceFactory);
    }
  }
}
