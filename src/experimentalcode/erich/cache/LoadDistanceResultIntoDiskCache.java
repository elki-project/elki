package experimentalcode.erich.cache;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.FileBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.wrapper.AbstractWrapper;

/**
 * Wrapper to convert a traditional text-serialized result into a on-disk matrix
 * for random access.
 * 
 * @author Erich Schubert
 * 
 */
public class LoadDistanceResultIntoDiskCache extends AbstractWrapper {
  /**
   * OptionID for {@link #CACHE_PARAM}
   */
  public static final OptionID CACHE_ID = OptionID.getOrCreateOptionID("loader.diskcache", "File name of the disk cache to create.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -distance.matrix}
   * </p>
   */
  private final FileParameter CACHE_PARAM = new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE);

  /**
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckWrites = false;

  /**
   * Distance function that is to be cached.
   */
  private FileBasedDoubleDistanceFunction<DatabaseObject> distance = new FileBasedDoubleDistanceFunction<DatabaseObject>();

  /**
   * Constructor.
   */
  public LoadDistanceResultIntoDiskCache() {
    super();
    addOption(CACHE_PARAM);
  }

  @Override
  public void run() {
    Collection<Integer> ids = distance.getIDs();
    int matrixsize = 0;
    for(Integer id : ids) {
      matrixsize = Math.max(matrixsize, id + 1);
    }

    File out;
    try {
      out = CACHE_PARAM.getValue();
    }
    catch(ParameterException e) {
      throw new AbortException("Output filename not given.", e);
    }
    UpperTriangleMatrix matrix;
    try {
      matrix = new UpperTriangleMatrix(out, DiskCacheBasedDoubleDistanceFunction.DOUBLE_CACHE_MAGIC, 0, 8, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix: " + e.getMessage(), e);
    }

    for(Integer id1 : distance.getIDs()) {
      for(Integer id2 : distance.getIDs()) {
        if(id2 >= id1) {
          byte[] data = new byte[8];
          double d = distance.distance(id1, id2).getValue();
          ByteArrayUtil.writeDouble(data, 0, d);
          try {
            matrix.writeRecord(id1, id2, data);
            if(debugExtraCheckWrites) {
              byte[] data2 = matrix.readRecord(id1, id2);
              double test = ByteArrayUtil.readDouble(data2, 0);
              if(test != d) {
                logger.warning("Distance read from file differs!" + test + " vs. " + d);
              }
            }
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + id1 + "," + id2 + " to matrix: " + e.getMessage(), e);
          }
        }
      }
    }
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    super.setParameters(args);
    String[] remainingParameters = super.getRemainingParameters().toArray(new String[0]);

    // Pass on parameters to distance function.
    remainingParameters = distance.setParameters(remainingParameters);
    addParameterizable(distance);
    
    super.rememberParametersExcept(args, remainingParameters);

    if(remainingParameters.length != 0) {
      LoggingUtil.warning("Unnecessary parameters specified: " + Arrays.asList(remainingParameters));
    }

    return remainingParameters;
  }

  @Override
  public String parameterDescription() {
    StringBuffer buf = new StringBuffer();
    buf.append(super.parameterDescription());
    buf.append(distance.parameterDescription());
    return buf.toString();
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args
   */
  public static void main(String[] args) {
    new LoadDistanceResultIntoDiskCache().runCLIWrapper(args);
  }
}
