package experimentalcode.frankenb.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.utils.DataSetUtils;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class KDTree {

  public static class Measure {
    
    private int calculations = 0;
    
    private Measure() {
      
    }
    
    public int getCalculations() {
      return this.calculations;
    }
    
  }
  
  private static class Node {
    
    final int dimension;
    final Node parent;
    final double splitPoint;
    final ArrayModifiableDBIDs ids = DBIDUtil.newArray();
    Node leftChild, rightChild;
    
    Node(Node parent, int dimension, double splitPoint) {
      this.parent = parent;
      this.dimension = dimension;
      this.splitPoint = splitPoint;
    }
    
    boolean isRoot() {
      return this.parent == null;
    }
    
    boolean isLeaf() {
      return this.leftChild == null && this.rightChild == null;
    }
    
  }
  
  private final IDataSet dataSet;
  private final Node root;
  private int totalNodes = 0;
  
  private Measure measure = null;
  
  public KDTree(IDataSet dataSet) {
    this.dataSet = dataSet;
    this.root = buildTree(null, dataSet, 0);
  }
  
  private Node buildTree(Node parent, IDataSet dataSet, int depth) {
    if (dataSet.getSize() == 0) return null;
    
//    Log.debug("Depth: " + depth);
    int dimension = (depth % dataSet.getDimensionality()) + 1;
//    Log.debug("Dimension: " + dimension);
    DBID medianId = median(dataSet, dimension);
    
    NumberVector<?, ?> medianVector = dataSet.get(medianId);
    double median = medianVector.doubleValue(dimension);
//    Log.debug("Median: (" + medianVector + ") [" + medianId + "]");
    
    Node newNode = new Node(parent, dimension, median);
    totalNodes++;
    
    ReferenceDataSet newDataSet = new ReferenceDataSet(dataSet);
    for (DBID id : dataSet.getIDs()) {
        NumberVector<?, ?> vector = dataSet.get(id);
        if (!vector.equals(medianVector)) {
          newDataSet.add(id);
        } else {
          newNode.ids.add(id);
        }
    }
//    Log.debug("IDs: " + newNode.ids);
    
    Pair<IDataSet, IDataSet> newDataSets = DataSetUtils.split(newDataSet, dimension, median);
//    Log.debug("Building left tree " + newDataSets.first);
    newNode.leftChild = buildTree(newNode, newDataSets.first, depth + 1);
    
//    Log.debug("Building right tree " + newDataSets.second);
    newNode.rightChild = buildTree(newNode, newDataSets.second, depth + 1);
    
    return newNode;
  }  
  
  /**
   * Finds the k neighborhood.
   * 
   * @param id
   * @param k
   * @param distanceFunction
   * @return
   */
  public DistanceList findNearestNeighbors(DBID id, int k, RawDoubleDistance<NumberVector<?, ?>> distanceFunction) {
    NumberVector<?, ?> vector = this.dataSet.get(id);
    Node node = searchNodeFor(vector, this.root);
    
    DistanceList distanceList = new DistanceList(id, k);
    Set<Node> alreadyVisited = new HashSet<Node>();

    this.measure = new Measure();
    findNeighbors(k, distanceFunction, vector, node, distanceList, alreadyVisited, measure);
//    Log.debug("Visited: " + alreadyVisited.size() + " of " + totalNodes);
    return distanceList;
  }
  
  public Measure getLastMeasure() {
    return this.measure;
  }
  
  private void findNeighbors(int k, RawDoubleDistance<NumberVector<?, ?>> distanceFunction, NumberVector<?, ?> queryVector, Node currentNode, DistanceList distanceList, Set<Node> alreadyVisited, Measure measure) {
    for (DBID id : currentNode.ids) {
      double maxDistance = (distanceList.getSize() >= k ? distanceList.getLast().second : Double.POSITIVE_INFINITY);
      double distanceToId = distanceFunction.doubleDistance(queryVector, dataSet.get(id));
      measure.calculations += 1;
      
      if (distanceToId <= maxDistance) {
        distanceList.addDistance(id, distanceToId);
      }

    }
    
    alreadyVisited.add(currentNode);
    if (!currentNode.isLeaf()) {
      double splitDistance = Math.abs(currentNode.splitPoint - queryVector.doubleValue(currentNode.dimension));
      
      if (splitDistance <= (distanceList.getSize() >= k ? distanceList.getLast().second : Double.POSITIVE_INFINITY)) {
        if (currentNode.leftChild != null && !alreadyVisited.contains(currentNode.leftChild)) {
          findNeighbors(k, distanceFunction, queryVector, currentNode.leftChild, distanceList, alreadyVisited, measure);
        }
      }
      if (splitDistance <= (distanceList.getSize() >= k ? distanceList.getLast().second : Double.POSITIVE_INFINITY)) {
        if (currentNode.rightChild != null && !alreadyVisited.contains(currentNode.rightChild)) {
          findNeighbors(k, distanceFunction, queryVector, currentNode.rightChild, distanceList, alreadyVisited, measure);
        }
      }
    }    
    
    if (!currentNode.isRoot() && !alreadyVisited.contains(currentNode.parent)) {
      findNeighbors(k, distanceFunction, queryVector, currentNode.parent, distanceList, alreadyVisited, measure);
    }
    
  }
  
  private Node searchNodeFor(NumberVector<?, ?> vector, Node node) {
    if (!node.isLeaf()) {
      int dimension = node.dimension;
      double value = vector.doubleValue(dimension);
      
      if (value < node.splitPoint && node.leftChild != null) {
        return searchNodeFor(vector, node.leftChild);
      }
      
      if (value >= node.splitPoint && node.rightChild != null){
        return searchNodeFor(vector, node.rightChild);
      }
    }
    return node;
  }  
  
  private static DBID median(IDataSet dataSet, int dimension) {
    List<Pair<DBID, Double>> items = new ArrayList<Pair<DBID, Double>>();
    for (DBID id : dataSet.getIDs()) {
      items.add(new Pair<DBID, Double>(id, dataSet.get(id).doubleValue(dimension)));
    }
    
    Collections.sort(items, new Comparator<Pair<DBID, Double>>() {

      @Override
      public int compare(Pair<DBID, Double> o1, Pair<DBID, Double> o2) {
        return o1.second.compareTo(o2.second);
      }
      
    });
    
    return items.get(((items.size() + 1) / 2) - 1).first;
    
  }
  
}
