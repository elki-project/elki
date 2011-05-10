package experimentalcode.frankenb.algorithms.partitioning;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This abstract class represents a partitioning algorithm with a fixed amount
 * of partitions.
 * 
 * @author Florian Frankenberger
 */
public abstract class AbstractFixedAmountPartitioning<V> implements IPartitioning<V> {
  /**
   * OptionID for {@link #PARTITIONS_PARAM}
   */
  public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("partitions", "Amount of partitions to create");

  /**
   * Parameter that specifies the percentage of deviations
   * <p>
   * Key: {@code -partitions}
   * </p>
   */
  private final IntParameter PARTITIONS_PARAM = new IntParameter(PARTITIONS_ID, false);

  private int partitionQuantity;

  public AbstractFixedAmountPartitioning(Parameterization config) {
    if(config.grab(PARTITIONS_PARAM)) {
      partitionQuantity = PARTITIONS_PARAM.getValue();
    }
  }

  @Override
  public List<DBIDPartition> makePartitions(Relation<? extends V> dataSet, int packageQuantity) throws UnableToComplyException {
    getLogger().verbose("partition quantity: " + partitionQuantity);

    return makePartitions(dataSet, packageQuantity, partitionQuantity);
  }

  protected abstract List<DBIDPartition> makePartitions(Relation<? extends V> dataSet, int packageQuantity, int partitionQuantity) throws UnableToComplyException;

  protected abstract Logging getLogger();
}
