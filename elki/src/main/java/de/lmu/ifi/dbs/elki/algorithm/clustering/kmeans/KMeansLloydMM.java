package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.awt.image.DataBufferUShort;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The k-means-- algorithm, using Lloyd-style bulk iterations.
 * 
 * 
 * @author Jonas Steinke
 * 
 * @apiviz.landmark
 * @apiviz.has KMeansModel
 * 
 * @param <V> vector datatype
 */
@Title("K-Means--")
@Description("Finds a least-squared partitioning into k clusters.")

public class KMeansLloydMM<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansLloyd.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansLloyd.class.getName();
  
  /**
   * MinHeap
   */
  public DoubleMinHeap minHeap;
  
  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansLloydMM(PrimitiveDistanceFunction<NumberVector> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    
    //Intialisieren der means
    List<Vector> means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);
    
    //initialisieren vom Heap
    minHeap = new DoubleMinHeap();
    
    // Setup cluster assignment store
    List<ModifiableDoubleDBIDList> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
    }
    
//    //creating the noise cluster
//    clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
//    
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;
    
    //5% der Punkte werden nicht betrachtet
    //TODO über Gui wert abfangen
//    int heapsize = relation.size()/20;
    int heapsize = 9;
    
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      minHeap.clear();
      LOG.incrementProcessed(prog);
      boolean changed = assignToNearestCluster(relation, means, clusters, assignment, varsum, heapsize);
      logVarstat(varstat, varsum);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      
      //Punkte aus der Relation nehmen welche nicht beachtet werden
      List<ModifiableDBIDs> computingClusters = new ArrayList<>();
      for(int i=0; i < clusters.size(); i++)
      {
        computingClusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
        for(int j=0; j < clusters.get(i).size(); j++)
        {
          if(clusters.get(i).get(j).doubleValue()< minHeap.peek())
          {
            computingClusters.get(i).add(clusters.get(i).get(j));
          }else
          { 
          }
              
        }
      }
      
      
      // Recompute means.
      means = means(computingClusters, means, relation);
      computingClusters.clear();
    }
    ArrayList<Double> all = new ArrayList<Double>();
    ArrayList<Double> deleted = new ArrayList<Double>();
    //creating the noise cluster
    clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
      
    //vor dem ausgeben der Resultate den noise bestimmen
    for(int i = 0; i < clusters.size()-1; i++)
    {
      for(int j = 0; j < clusters.get(i).size(); j++)
      {
        all.add(clusters.get(i).get(j).doubleValue());
        if(clusters.get(i).get(j).doubleValue() >= minHeap.peek())
        {
          deleted.add(clusters.get(i).get(j).doubleValue());
          //zuweisung an den noise Cluster
          clusters.get(k).add(clusters.get(i).get(j));
          assignment.putInt(clusters.get(i).get(j), k);
          clusters.get(i).remove(j);
          j--;
        }
      }
    }
    
    all.sort(null);
    deleted.sort(null);
    
    System.out.println(all);
    System.out.println("Und hier die gelöschten");
    System.out.println(deleted);
    
    double[] computingMeans = {0.0, 0.0} ;
    Vector v = new Vector(computingMeans) ;
    means.add(v);
    double[] noisevarsum = new double[varsum.length+1];
    for(int i=0; i< varsum.length; i++)
    {
      noisevarsum[i] = varsum[i];
    }
    noisevarsum[varsum.length] = 0;
    
//    means = means(clusters, means, relation);
    
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      KMeansModel model = new KMeansModel(means.get(i), noisevarsum[i]);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }
    return result;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * And saves the distance in a MinHeap
   * 
   * 
   * @param relation the database to cluster
   * @param means a list of k means
   * @param clusters cluster assignment
   * @param assignment Current cluster assignment
   * @param varsum Variance sum output
   * @param heapsize the size of the minheap
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<? extends V> relation, List<? extends NumberVector> means, List<? extends ModifiableDoubleDBIDList> clusters, WritableIntegerDataStore assignment, double[] varsum, int heapsize) {
    assert(k == means.size());
    boolean changed = false;
    Arrays.fill(varsum, 0.);
    final PrimitiveDistanceFunction<? super NumberVector> df = getDistanceFunction();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      //Optimieren, indem man nur eine mindist hat
      double mindist = Double.POSITIVE_INFINITY;
      
      //fv ist der aktuelle Pkt
      V fv = relation.get(iditer);
      
      //Index des Clusterpunktes
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        double dist = df.distance(fv, means.get(i));
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      
      //zuweisung zum Heap
      minHeap.add(mindist, heapsize);
      
      varsum[minIndex] += mindist;
      changed |= updateAssignmentWithDistance(iditer, clusters, assignment, minIndex, mindist);
    }
    return changed;
  }
  
  
  protected boolean updateAssignmentWithDistance(DBIDIter iditer, List<? extends ModifiableDoubleDBIDList> clusters, WritableIntegerDataStore assignment, int newA, double mindist) {
    final int oldA = assignment.intValue(iditer);
    
    //falls keine veränderung der Cluster
    if(oldA == newA) {
      //Suche den index der DBID im Cluster
      for(int i = 0; i < clusters.get(oldA).size(); i++)
      {
        if(DBIDUtil.equal(clusters.get(oldA).get(i), iditer))
        {
            //"update" die distance
            clusters.get(oldA).remove(i);
            clusters.get(oldA).add(mindist, iditer);
        }
      }
      return false;
    }
    //zuweisung des Punktes an den neuen Cluster
    clusters.get(newA).add(mindist, iditer);
    assignment.putInt(iditer, newA);
    if(oldA >= 0) {
      for(int i = 0; i< clusters.get(oldA).size(); i++)
      {
        if(DBIDUtil.equal(clusters.get(oldA).get(i), iditer))
        {
          clusters.get(oldA).remove(i);
        }
      }
    }
    return true;
  }
  
  
  //*************************************************************************************//
  
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    //TODO Description ergänzen
    public static final OptionID RATE_ID = new OptionID("K-MeansMM-Rate", "description");
    private double rate;
    @Override
    protected Logging getLogger() {
      return LOG;
    }
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter rateP = new DoubleParameter(RATE_ID, 0.05)//
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE)//
      .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(rateP)) {
        rate = rateP.doubleValue();
      }
    }

    @Override
    protected KMeansLloydMM<V> makeInstance() {
      //TODO rate übergeben
      return new KMeansLloydMM<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
