package experimentalcode.frankenb.algorithms.pairing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.PositionedPartition;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairing;

/**
 * This implementation uses the additional information provided by the {@link PositionedPartition}
 * to pair each partition with the partitions which are within a distance of 1.
 * <p/>
 * Note: This implementation will throw an UnsupportedOperationException if the given partitions are not all
 * {@link PositionedPartition}s 
 * 
 * @author Florian Frankenberger
 */
public class NearFieldPartitionPairing implements IPartitionPairing {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(NearFieldPartitionPairing.class);

  public static final OptionID ADD_DIAGONAL_ID = OptionID.getOrCreateOptionID("adddiagonal", "if false only the direct neighbors will be paired");
  private final Flag ADD_DIAGONAL_PARAM = new Flag(ADD_DIAGONAL_ID);
  
  private static class Position {
    int[] position;
    
    public Position(int[] position) {
      this.position = position;
    }
    
    @Override
    public int hashCode() {
      int hash = 0;
      for (int i : position) {
        hash ^= i;
      }
      return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Position)) return false;
      Position other = (Position) obj;
      return Arrays.equals(this.position, other.position);
    }
  }
  
  boolean addDiagonal;
  
  public NearFieldPartitionPairing(Parameterization config) {
    if (config.grab(ADD_DIAGONAL_PARAM)) {
      addDiagonal = ADD_DIAGONAL_PARAM.getValue();
    }
  }

  @Override
  public List<PartitionPairing> makePairings(IDataSet dataSet, List<IPartition> partitions, int packageQuantity) throws UnableToComplyException {
    //we assume that all positionedPartitions have the same dimensionality
    if (partitions.size() < 1) throw new RuntimeException("Can't work with 0 partitions!");
    
    if (logger.isVerbose()) {
      logger.verbose("Add diagonals: " + Boolean.toString(addDiagonal));
    }
    
    IPartition prototypePartition = partitions.get(0);
    checkPartition(prototypePartition);
    
    PositionedPartition prototypePositionedPartition = (PositionedPartition) prototypePartition;
    int dimensionality = prototypePositionedPartition.getDimensionality();
    
    int[] minValues = new int[dimensionality];
    int[] maxValues = new int[dimensionality];
    Arrays.fill(minValues, Integer.MAX_VALUE);
    Arrays.fill(maxValues, Integer.MIN_VALUE);
    
    Map<Position, PositionedPartition> partitionMap = new HashMap<Position, PositionedPartition>();
    for (IPartition partition : partitions) {
      checkPartition(partition);
      
      PositionedPartition positionedPartition = (PositionedPartition) partition;
      for (int dim = 0; dim < dimensionality; ++dim) {
        int pos = positionedPartition.getPosition(dim + 1);
        minValues[dim] = Math.min(pos, minValues[dim]);
        maxValues[dim] = Math.max(pos, maxValues[dim]);
      }
      partitionMap.put(new Position(positionedPartition.getPositionArray()), positionedPartition);
    }
    
    int[] extents = new int[dimensionality];
    int totalSpace = 1;
    for (int dim = 0; dim < dimensionality; ++dim) {
      extents[dim] = (maxValues[dim] - minValues[dim]) + 1;
      totalSpace *= extents[dim];
    }
    
    List<PartitionPairing> result = new ArrayList<PartitionPairing>();
    
    int[] positionArray = new int[dimensionality];
    for (int i = 0; i < totalSpace; ++i) {
      int positionCounter = i;
      int totalExtents = 1;
      for (int dim = dimensionality - 1; dim >= 0; --dim) {
        totalExtents *= extents[dim];
        int acFactor = totalSpace / totalExtents;
        positionArray[dim] = positionCounter / acFactor + minValues[dim];
        positionCounter = positionCounter % acFactor;
      }
      
      Position position = new Position(positionArray);
      if (partitionMap.containsKey(position)) {
        PositionedPartition partition = partitionMap.get(position);
        
        if (!addDiagonal) {
          //1. we pair with every next partition in each dimension
          result.add(new PartitionPairing(partition, partition));
          int[] otherPartitionsPositionArray = Arrays.copyOf(positionArray, dimensionality);
          for (int dim = 0; dim < dimensionality; ++dim) {
            otherPartitionsPositionArray[dim] ++;
            Position otherPosition = new Position(otherPartitionsPositionArray); 
            if (partitionMap.containsKey(otherPosition)) {
              PositionedPartition otherPartition = partitionMap.get(otherPosition);
              result.add(new PartitionPairing(partition, otherPartition));
            }
            otherPartitionsPositionArray[dim] --;
          }
        } else {
          //2. we pair with each diagonal partition and with each neighbor partition
          for (long j = ((long) Math.pow(3, dimensionality) / 2); j < Math.pow(3, dimensionality); ++j) {
            int[] otherPartitionsPositionArray = Arrays.copyOf(positionArray, dimensionality);
            long permutation = j;
            for (int dim = dimensionality-1; dim >= 0; --dim) {
              long thisFactor = (long) Math.pow(3, dim);
              int aPosition = (int) (permutation / thisFactor - 1);
              permutation = permutation % thisFactor;
              otherPartitionsPositionArray[dim] += aPosition;
            }
            
            
            Position otherPosition = new Position(otherPartitionsPositionArray);
            if (partitionMap.containsKey(otherPosition)) {
              PositionedPartition otherPartition = partitionMap.get(otherPosition);
              result.add(new PartitionPairing(partition, otherPartition));
            }
          }
        }
        
      }
    }
    
    return result;
  }
  
  private void checkPartition(IPartition partition) {
    if (!(partition instanceof PositionedPartition)) {
      throw new UnsupportedOperationException("This pairing only works with PositionedPartitions");
    }
  }

}
