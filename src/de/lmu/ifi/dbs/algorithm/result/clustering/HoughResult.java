package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.tree.interval.IntervalTree;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HoughResult extends AbstractResult<ParameterizationFunction> {

  private Database<RealVector> intervalDB;

  /**
   * todo
   *
   * @param db
   */
  public HoughResult(Database<ParameterizationFunction> db,
                     Database<RealVector> intervalDB,
                     HierarchicalAxesParallelClusters<RealVector, PreferenceVectorBasedCorrelationDistance> clusters) {
    super(db);
    this.intervalDB = intervalDB;
  }

  /**
   * todo
   * Writes the clustering result to the given stream.
   *
   * @param outStream     the stream to write to
   * @param normalization Normalization to restore original values according to, if this action is supported
   *                      - may remain null.
   * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
   *                      no header will be written
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values initialized during normalization
   */
  public void output(PrintStream outStream, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    writeHeader(outStream, settings, null);
    for (Iterator<Integer> it = intervalDB.iterator(); it.hasNext();) {
      Integer id = it.next();
      RealVector interval = intervalDB.get(id);
      IntervalTree tree = (IntervalTree) intervalDB.getAssociation(AssociationID.INTERVAL_TREE, id);
      outStream.println(Format.format(interval.getRowVector().getColumnPackedCopy(), " ", 8) + " " +
                        "(" + tree.getIds().size() + ")");
    }
  }
}
