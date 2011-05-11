package experimentalcode.frankenb.algorithms.partitioning;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurve;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * This class orders the items of the data set according to their z-curve value
 * and splits them in ascending order to their z-curve value into a given amount
 * of partitions.
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioning<V extends NumberVector<?, ?>> extends AbstractFixedAmountPartitioning<V> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ZCurvePartitioning.class);

  public ZCurvePartitioning(Parameterization config) {
    super(config);
  }

  @Override
  protected List<DBIDPartition> makePartitions(Relation<? extends V> relation, int partitionQuantity) throws UnableToComplyException {
    try {
      ZCurve.Transformer zcurve = new ZCurve.Transformer(relation, relation.getDBIDs());
      
      List<Pair<DBID, BigInteger>> projection = new ArrayList<Pair<DBID, BigInteger>>(relation.size());
      for (DBID id : relation.iterDBIDs()) {
        projection.add(new Pair<DBID, BigInteger>(id, zcurve.asBigInteger(relation.get(id))));
      }
      Collections.sort(projection, new Comparator<Pair<DBID, BigInteger>>() {
        @Override
        public int compare(Pair<DBID, BigInteger> o1, Pair<DBID, BigInteger> o2) {
          int result = o1.second.compareTo(o2.second);
          if (result == 0) {
            result = o1.first.compareTo(o2.first);
          }
          return result;
        }
      });

      int itemsPerPartition = relation.size() / partitionQuantity;
      int addItemsUntilPartition = relation.size() % partitionQuantity;
      
      logger.verbose(String.format("Items per partition about: %d", itemsPerPartition));
      
      Iterator<Pair<DBID, BigInteger>> projectionIterator = projection.iterator();
      List<DBIDPartition> partitions = new ArrayList<DBIDPartition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        ModifiableDBIDs partids = DBIDUtil.newArray(); 
        for (int j = 0; j < itemsPerPartition + (i + 1 < addItemsUntilPartition ? 1 : 0); ++j) {
          DBID id = projectionIterator.next().first;
          partids.add(id);
        }
        logger.verbose(String.format("\tCreated partition %d with %d items.", i, partids.size()));
        partitions.add(new DBIDPartition(partids));
      }
      
      return partitions;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}