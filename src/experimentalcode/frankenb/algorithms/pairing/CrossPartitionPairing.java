/**
 * 
 */
package experimentalcode.frankenb.algorithms.pairing;

import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairing;
import experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage;

/**
 * Creates pairings according to a simple scheme where the distance of each item of each partition is at least
 * calculated to each item of the same partition. The higher the <i>crosspairings</i> value (between 0.0 and 1.0) is the more
 * cross pairings between different partitions will be made (1.0 = all pairings are made).
 * <p/>
 * For example if we consider these partitions: A, B, C - then we have with crosspairings at 0.0 these pairings:
 * <pre>
 *  A with A, B with B, C with C
 * </pre>
 * at 1.0 we would have:
 * <pre>
 * A with A, A with B, A with C, B with B, B with C, C with C
 * </pre>
 * 
 * @author Florian Frankenberger
 */
public class CrossPartitionPairing implements IPartitionPairing {

  private static final Logging LOG = Logging.getLogger(CrossPartitionPairing.class);
  
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
  
  public CrossPartitionPairing(Parameterization config) {
    LoggingConfiguration.setLevelFor(CrossPartitionPairing.class.getCanonicalName(), Level.ALL.getName());
    
    config = config.descend(this);
    if (config.grab(CROSSPAIRINGS_PARAM)) {
      crossPairingsPercent = CROSSPAIRINGS_PARAM.getValue();
      if (crossPairingsPercent > 100 || crossPairingsPercent < 0) throw new IllegalArgumentException("deviation is a percent value an should be between 0 and 100");
    }    
  }
  
  @Override
  public final void makePairings(IDataSet dataSet, List<IPartition> partitions, IPartitionPairingStorage partitionPairingStorage, int packageQuantity) throws UnableToComplyException {
    
    int deviationMax = (int) (Math.max(1, crossPairingsPercent * (float) partitions.size()) - 1);
    int pairingsTotal = getAmountOfPairings(partitions.size());
    int pairingsRemoved = getAmountOfPairings(partitions.size() - (deviationMax + 1));
    
    Log.info(String.format("Percentage of cross pairings: %5.2f%%", crossPairingsPercent * 100f));
    Log.info(String.format("Amount of total pairings: %d", pairingsTotal));
    Log.info(String.format("Amount of removed pairings: %d", pairingsRemoved));
    Log.info(String.format("Amount of pairings: %d", (pairingsTotal - pairingsRemoved)));
    
    partitionPairingStorage.setPartitionPairings(pairingsTotal - pairingsRemoved);
    
    for (int i = 0; i < partitions.size(); ++i) {
      for (int j = i; j >= Math.max(0, i - deviationMax); --j) {
        partitionPairingStorage.add(new PartitionPairing(partitions.get(i), partitions.get(j)));
      }
    }

    //displayPairings(partitions.size(), deviationMax);    
    partitionPairingStorage.close();
  }  
  
  private static int getAmountOfPairings(int partitions) {
    return (int) (((partitions + 1) / 2.0f) * partitions);
  }
  
  @SuppressWarnings("unused")
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
