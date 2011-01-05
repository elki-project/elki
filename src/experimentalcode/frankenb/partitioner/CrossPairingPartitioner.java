/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.Partition;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public abstract class CrossPairingPartitioner extends FixedPartitionsAmountPartitioner {

  private static final Logging LOG = Logging.getLogger(CrossPairingPartitioner.class);
  
  /**
   * OptionID for {@link #CROSSPAIRINGS_PARAM}
   */
  public static final OptionID CROSSPAIRINGS_ID = OptionID.getOrCreateOptionID("crosspairings", "The percent of pairings to calculate - the more the more accurate the result will be [1.0 = full; 0.0 = only diagonal pairings]");
  
  /**
   * Parameter that specifies the percentage of deviations
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  private final DoubleParameter CROSSPAIRINGS_PARAM = new DoubleParameter(CROSSPAIRINGS_ID, false);  
  
  private double crossPairingsPercent;
  
  public CrossPairingPartitioner(Parameterization config) {
    super(config);
    LoggingConfiguration.setLevelFor(CrossPairingPartitioner.class.getCanonicalName(), Level.ALL.getName());
    
    config = config.descend(this);
    if (config.grab(CROSSPAIRINGS_PARAM)) {
      crossPairingsPercent = CROSSPAIRINGS_PARAM.getValue();
      if (crossPairingsPercent > 100 || crossPairingsPercent < 0) throw new IllegalArgumentException("deviation is a percent value an should be between 0 and 100");
    }    
  }
  
  
  public final List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    List<Partition> partitions = makePartitions(dataBase, partitionQuantity);
    
    int deviationMax = (int) (Math.max(1, crossPairingsPercent * (float) partitions.size()) - 1);
    int pairingsTotal = getAmountOfPairings(partitions.size());
    int pairingsRemoved = getAmountOfPairings(partitions.size() - (deviationMax + 1));
    
    LOG.log(Level.INFO, String.format("\tPercentage of cross pairings: %5.2f%%", crossPairingsPercent * 100f));
    LOG.log(Level.INFO, String.format("\tAmount of total pairings: %d", pairingsTotal));
    LOG.log(Level.INFO, String.format("\tAmount of removed pairings: %d", pairingsRemoved));
    LOG.log(Level.INFO, String.format("\tAmount of pairings: %d", (pairingsTotal - pairingsRemoved)));
    
    List<PartitionPairing> pairings = new ArrayList<PartitionPairing>();
    
    for (int i = 0; i < partitions.size(); ++i) {
      for (int j = i; j >= Math.max(0, i - deviationMax); --j) {
        pairings.add(new PartitionPairing(partitions.get(i), partitions.get(j)));
      }
    }

    displayPairings(partitions.size(), deviationMax);    
    
    return pairings;
    
  }  
  
  protected abstract List<Partition> makePartitions(Database<NumberVector<?, ?>> dataBase, int partitionQuantity) throws UnableToComplyException;
  
  private static int getAmountOfPairings(int partitions) {
    return (int) (((partitions + 1) / 2.0f) * partitions);
  }
  
  private static void displayPairings(int partitions, int deviationMax) {
    LOG.log(Level.INFO, "");
    
    StringBuilder sb = new StringBuilder();
    sb.append("   ");
    for (int i = 0; i < partitions; ++i) {
      sb.append(String.format("%02d ", i));
    }
    LOG.log(Level.INFO, sb.toString());
    StringBuilder line1 = null;
    for (int i = 0; i < partitions; ++i) {
      line1 = new StringBuilder("  ");
      StringBuilder line2 = new StringBuilder(String.format("%02d", i));
      int acMin = Math.max(0, i - deviationMax);
      for (int j = 0; j < partitions; ++j) {
        
        line1.append("===");
        line2.append("|");
        line2.append(j >= acMin && j <= i ? "XX" : "  ");
      }
      
      line1.append("=");
      line2.append("|");
      LOG.log(Level.INFO, line1.toString());
      LOG.log(Level.INFO, line2.toString());
    }
    LOG.log(Level.INFO, line1.toString());
    LOG.log(Level.INFO, "");
  }  
  
}
