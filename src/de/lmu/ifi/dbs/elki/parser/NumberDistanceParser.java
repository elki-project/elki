package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.ExternalObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a parser for parsing one distance value per line.
 * <p/>
 * A line must have the following format: id1 id2 distanceValue, where id1 and
 * id2 are integers representing the two ids belonging to the distance value.
 * Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 * @param <D> distance type
 * @param <N> number type
 */
@Title("Number Distance Parser")
@Description("Parser for the following line format:\n" + "id1 id2 distanceValue, where id1 and is2 are integers representing the two ids belonging to the distance value.\n" + " The ids and the distance value are separated by whitespace. Empty lines and lines beginning with \"#\" will be ignored.")
public class NumberDistanceParser<D extends NumberDistance<D, N>, N extends Number> extends AbstractParser<ExternalObject> implements DistanceParser<ExternalObject, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(NumberDistanceParser.class);

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("parser.distancefunction", "Distance function used for parsing values.");

  /**
   * Parameter for distance function.
   */
  private ObjectParameter<DistanceFunction<?, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<?, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class);

  /**
   * The distance function.
   */
  private DistanceFunction<?, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public NumberDistanceParser(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  @Override
  public DistanceParsingResult<ExternalObject, D> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<Pair<ExternalObject, List<String>>> objectAndLabelsList = new ArrayList<Pair<ExternalObject, List<String>>>();

    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    Map<Pair<DBID, DBID>, D> distanceCache = new HashMap<Pair<DBID, DBID>, D>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(lineNumber % 10000 == 0 && logger.isDebugging()) {
          logger.debugFine("parse " + lineNumber / 10000);
          // logger.fine("parse " + lineNumber / 10000);
        }
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          List<String> entries = tokenize(line);
          if(entries.size() != 3) {
            throw new IllegalArgumentException("Line " + lineNumber + " does not have the " + "required input format: id1 id2 distanceValue! " + line);
          }

          DBID id1, id2;
          try {
            id1 = DBIDUtil.importInteger(Integer.parseInt(entries.get(0)));
          }
          catch(NumberFormatException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ": id1 is no integer!");
          }

          try {
            id2 = DBIDUtil.importInteger(Integer.parseInt(entries.get(1)));
          }
          catch(NumberFormatException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ": id2 is no integer!");
          }

          try {
            D distance = distanceFunction.getDistanceFactory().parseString(entries.get(2));
            put(id1, id2, distance, distanceCache);
            ids.add(id1);
            ids.add(id2);
          }
          catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ":" + e.getMessage(), e);
          }
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    if(logger.isDebugging()) {
      logger.debugFine("check");
    }

    // check if all distance values are specified
    for(DBID id1 : ids) {
      for(DBID id2 : ids) {
        if(id2.getIntegerID() < id1.getIntegerID()) {
          continue;
        }
        if(!containsKey(id1, id2, distanceCache)) {
          throw new IllegalArgumentException("Distance value for " + id1 + " - " + id2 + " is missing!");
        }
      }
    }

    if(logger.isDebugging()) {
      logger.debugFine("add to objectAndLabelsList");
    }
    for(DBID id : ids) {
      objectAndLabelsList.add(new Pair<ExternalObject, List<String>>(new ExternalObject(id), new ArrayList<String>()));
    }

    return new DistanceParsingResult<ExternalObject, D>(objectAndLabelsList, distanceCache);
  }

  /**
   * Puts the specified distance value for the given ids to the distance cache.
   * 
   * @param id1 the first id
   * @param id2 the second id
   * @param distance the distance value
   * @param cache the distance cache
   */
  private void put(DBID id1, DBID id2, D distance, Map<Pair<DBID, DBID>, D> cache) {
    // the smaller id is the first key
    if(id1.getIntegerID() > id2.getIntegerID()) {
      put(id2, id1, distance, cache);
      return;
    }

    D oldDistance = cache.put(new Pair<DBID, DBID>(id1, id2), distance);

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
  public boolean containsKey(DBID id1, DBID id2, Map<Pair<DBID, DBID>, D> cache) {
    if(id1.getIntegerID() > id2.getIntegerID()) {
      return containsKey(id2, id1, cache);
    }

    return cache.containsKey(new Pair<DBID, DBID>(id1, id2));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}