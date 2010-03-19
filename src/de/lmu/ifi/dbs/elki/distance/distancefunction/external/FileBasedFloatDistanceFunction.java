package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.FloatDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractFloatDistanceFunction;
import de.lmu.ifi.dbs.elki.parser.DistanceParser;
import de.lmu.ifi.dbs.elki.parser.DistanceParsingResult;
import de.lmu.ifi.dbs.elki.parser.NumberDistanceParser;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a DistanceFunction that is based on float distances given by a
 * distance matrix of an external file.
 * 
 * @author Elke Achtert
 * @param <V> object type
 */
@Title("File based float distance for database objects.")
@Description("Loads float distance values from an external text file.")
public class FileBasedFloatDistanceFunction<V extends DatabaseObject> extends AbstractFloatDistanceFunction<V> implements Parameterizable {
  /**
   * OptionID for {@link #MATRIX_PARAM}
   */
  public static final OptionID MATRIX_ID = OptionID.getOrCreateOptionID("distance.matrix", "The name of the file containing the distance matrix.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -distance.matrix}
   * </p>
   */
  private final FileParameter MATRIX_PARAM = new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * OptionID for {@link #PARSER_PARAM}
   */
  public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID("distance.parser", "Parser used to load the distance matrix.");

  /**
   * Optional parameter to specify the parsers to provide a database, must
   * extend {@link DistanceParser}. If this parameter is not set,
   * {@link NumberDistanceParser} is used as parser for all input files.
   * <p>
   * Key: {@code -distance.parser}
   * </p>
   */
  private final ObjectParameter<DistanceParser<V, FloatDistance>> PARSER_PARAM = new ObjectParameter<DistanceParser<V, FloatDistance>>(PARSER_ID, DistanceParser.class, NumberDistanceParser.class);

  private DistanceParser<V, FloatDistance> parser = null;

  private Map<Pair<Integer, Integer>, FloatDistance> cache = null;
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FileBasedFloatDistanceFunction(Parameterization config) {
    super();
    if(config.grab(MATRIX_PARAM)) {
      File matrixfile = MATRIX_PARAM.getValue();
      try {
        loadCache(matrixfile);
      }
      catch(IOException e) {
        config.reportError(new WrongParameterValueException(MATRIX_PARAM, matrixfile.toString(), e));
      }
    }
    if(config.grab(PARSER_PARAM)) {
      parser = PARSER_PARAM.instantiateClass(config);
    }
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   * 
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObject according to this
   *         distance function
   */
  public FloatDistance distance(V o1, V o2) {
    return distance(o1.getID(), o2.getID());
  }

  /**
   * Returns the distance between the two specified objects.
   * 
   * @param id1 first object id
   * @param o2 second DatabaseObject
   * @return the distance between the two objects specified by their objects ids
   */
  @Override
  public FloatDistance distance(Integer id1, V o2) {
    return distance(id1, o2.getID());
  }

  /**
   * Returns the distance between the two objects specified by their objects
   * ids. If a cache is used, the distance value is looked up in the cache. If
   * the distance does not yet exists in cache, it will be computed an put to
   * cache. If no cache is used, the distance is computed.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their objects ids
   */
  @Override
  public FloatDistance distance(Integer id1, Integer id2) {
    if (id1 == null) {
      return undefinedDistance();
    }
    if (id2 == null) {
      return undefinedDistance();
    }
    // the smaller id is the first key
    if (id1 > id2) {
      return distance(id2, id1);
    }

    return cache.get(new Pair<Integer, Integer>(id1, id2));
  }
  
  private void loadCache(File matrixfile) throws IOException {
    InputStream in = FileUtil.tryGzipInput(new FileInputStream(matrixfile));
    DistanceParsingResult<V, FloatDistance> res = parser.parse(in);
    cache = res.getDistanceCache();
  }
}