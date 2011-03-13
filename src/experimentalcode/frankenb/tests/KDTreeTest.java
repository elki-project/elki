/**
 * 
 */
package experimentalcode.frankenb.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.KDTree;
import experimentalcode.frankenb.model.ListDataSet;
import experimentalcode.frankenb.model.ifaces.IDataSet;


/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class KDTreeTest {

  @Test
  public void simpleTest() {
    List<NumberVector<?, ?>> items = new ArrayList<NumberVector<?, ?>>();
    items.add(new IntegerVector(new int[] {-4, -5}));
    items.add(new IntegerVector(new int[] {-9, 3}));
    items.add(new IntegerVector(new int[] {9, -1}));
    items.add(new IntegerVector(new int[] {8, 6}));
    items.add(new IntegerVector(new int[] {6, -7}));
    items.add(new IntegerVector(new int[] {-6, -5}));
    items.add(new IntegerVector(new int[] {3, 8}));
    items.add(new IntegerVector(new int[] {4, 1}));
    items.add(new IntegerVector(new int[] {7, -6}));
    items.add(new IntegerVector(new int[] {9, -3}));
    IDataSet dataSet = new ListDataSet(2, items);

    KDTree tree = new KDTree(dataSet);
    
    DistanceList list = tree.findNearestNeighbors(1, 3, EuclideanDistanceFunction.STATIC);
    
    int counter = 0;
    assertEquals(3, list.getSize());
    for (Pair<Integer, Double> distanceEntry : list) {
      if (counter == 0) {
        assertEquals(1, (int) distanceEntry.first);
      } else
        if (counter == 1) {
          assertEquals(5, (int) distanceEntry.first);
        } else
          if (counter == 2) {
            assertEquals(0, (int) distanceEntry.first);
          }
      counter++;
    }
  }
  
}
