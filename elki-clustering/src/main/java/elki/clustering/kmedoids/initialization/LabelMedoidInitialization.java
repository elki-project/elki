package elki.clustering.kmedoids.initialization;

import java.util.HashMap;
import java.util.Map;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

public class LabelMedoidInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  KMedoidsInitialization<O> initializer;

  public LabelMedoidInitialization(RandomFactory rnd, KMedoidsInitialization<O> initializer) {
    super(rnd);
    this.initializer = initializer;
  }

  @Override
  public int[] chooseInitialMedoids(int k, int noLabels, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids) {
    int[] clusterLabels = new int[k];
    // make a map label -> objects
    Map<Integer, ArrayModifiableDBIDs> labelToDataMap = new HashMap<>();
    clusterIdObjectsMap(ids, labelToDataMap, labels);
    // we do not know which "classes" to forget .. abort
    if(k < labelToDataMap.size() - 1) {
      throw new RuntimeException("Number of expected clusters in labelled data is larger than k. Consider increasing k.");
    }
    // if k = the number of labels, then for each partition find medoid
    int i = 0;
    for(Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()) {
      if(cluster.getKey() == 0) {
        continue;
      }
      DBIDRef medoid = findMedoidFromData(cluster.getValue(), distance);
      medoids.add(medoid);
      clusterLabels[i++] = cluster.getKey();
    }
    // future check - when all is fixed if choosing uncolored means is
    // better
    while(medoids.size() < k) {
      DBIDVar medoid = DBIDUtil.randomSample(ids, rnd);
      if(!medoids.contains(medoid)) {
        medoids.add(medoid);
        clusterLabels[i++] = labels.intValue(medoid);
      }
    }
    return clusterLabels;
  }

  protected DBIDRef findMedoidFromData(ArrayDBIDs cluster, DistanceQuery<? super O> distQ) {
    DBIDArrayIter otherIt = cluster.iter();
    double minSum = Double.MAX_VALUE;
    DBIDVar medoid = DBIDUtil.newVar();

    for(DBIDIter iter = cluster.iter(); iter.valid(); iter.advance()) {
      double sum = 0;
      for(otherIt.seek(0); otherIt.valid(); otherIt.advance()) {
        double d = distQ.distance(iter, otherIt);
        sum += d;
      }
      if(sum < minSum) {
        minSum = sum;
        medoid.set(iter);
      }
    }
    return medoid;
  }

  protected void clusterIdObjectsMap(DBIDs ids, Map<Integer, ArrayModifiableDBIDs> labelToDataMap, WritableIntegerDataStore labels) {
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      int myLabel = labels.intValue(iter);
      if(labelToDataMap.containsKey(myLabel)) {
        labelToDataMap.get(myLabel).add(iter);
      }
      else {
        ArrayModifiableDBIDs curLabelCl = DBIDUtil.newArray();
        curLabelCl.add(iter);
        labelToDataMap.put(myLabel, curLabelCl);
      }
    }
  }


    /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par<O> extends SemiSupervisedKMedoidsInitialization.Par {

    /**
     * Method to choose means without looking at labels.
     */
    protected KMedoidsInitialization<O> initializer;


    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<KMedoidsInitialization<O>>(KMeans.INIT_ID, KMedoidsInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
    }

    @Override
    public UnsupervisedInitialization<?> make() {
      return new UnsupervisedInitialization<>(rnd, initializer);
    }


  }
}
