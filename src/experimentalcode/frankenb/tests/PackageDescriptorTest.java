package experimentalcode.frankenb.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import experimentalcode.frankenb.model.DiskBackedPartition;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IPartition;


/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PackageDescriptorTest {

  @Test
  public void simpleTest() throws Exception {
    int itemsPerDimension = 10;
    
    File packageDescriptorFile = File.createTempFile("packagedescriptor", ".dat");
    packageDescriptorFile.deleteOnExit();
    
    PackageDescriptor packageDescriptor = new PackageDescriptor(0, 2, new BufferedDiskBackedDataStorage(packageDescriptorFile));
    
    List<IPartition> partitions = new ArrayList<IPartition>();
    int counter = 0;
    for (int i = 0; i < 2; ++i) {
      DiskBackedPartition partition = new DiskBackedPartition(i, 2);
      for (int x = i*itemsPerDimension; x < (i+1) * itemsPerDimension; ++x) {
        for (int y = i*itemsPerDimension; y < (i+1) * itemsPerDimension; ++y) {
          DoubleVector vector = new DoubleVector(new double[] {x, y});
          partition.addVector(counter++, vector);
        }
      }
      partitions.add(partition);
    }
    
    packageDescriptor.addPartitionPairing(new PartitionPairing(partitions.get(0), partitions.get(0)));
    packageDescriptor.addPartitionPairing(new PartitionPairing(partitions.get(1), partitions.get(0)));
    packageDescriptor.addPartitionPairing(new PartitionPairing(partitions.get(1), partitions.get(1)));
    
    packageDescriptor.close();
    
    packageDescriptor = PackageDescriptor.readFromStorage(new BufferedDiskBackedDataStorage(packageDescriptorFile));
    assertEquals(3, packageDescriptor.getPairings());
    
    Set<Integer> idSet = new HashSet<Integer>();
    for (PartitionPairing pairing : packageDescriptor) {
      checkPartition(pairing.getPartitionOne(), itemsPerDimension, idSet);
      checkPartition(pairing.getPartitionTwo(), itemsPerDimension, idSet);
    }
    
    assertEquals(counter, idSet.size());
    for (int i = 0; i < counter; ++i) {
      assertTrue(idSet.contains(i));
    }
    
    packageDescriptor.close();
  }  
  
  private static void checkPartition(IPartition partition, int itemsPerDimension, Set<Integer> idSet) throws Exception {
    int i = partition.getId();
    assertEquals(partition.getSize(), itemsPerDimension * itemsPerDimension);
    Iterator<Pair<Integer, NumberVector<?, ?>>> iterator = partition.iterator();
    for (int x = i*itemsPerDimension; x < (i+1) * itemsPerDimension; ++x) {
      for (int y = i*itemsPerDimension; y < (i+1) * itemsPerDimension; ++y) {
        assertTrue(iterator.hasNext());
        Pair<Integer, NumberVector<?, ?>> next = iterator.next();
        int id = next.getFirst();
        idSet.add(id);
        
        NumberVector<?, ?> vector = next.getSecond();
        
        double[] checkValues = new double[] {x, y};
        for (int dim = 0; dim < 2; ++dim) {
          double value = vector.doubleValue(dim + 1);
          assertEquals(checkValues[dim], value, 0);
        }
      }
    }
    
  }
}
