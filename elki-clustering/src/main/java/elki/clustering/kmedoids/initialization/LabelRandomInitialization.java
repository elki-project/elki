package elki.clustering.kmedoids.initialization;

import java.util.HashMap;
import java.util.Map;

import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.random.RandomFactory;

public class LabelRandomInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  public LabelRandomInitialization(RandomFactory rnd) {
    super(rnd);
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
    // if k = the number of labels, then for each partition find a random object
    int i = 0;
    for(Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()) {
      if(cluster.getKey() == 0) {
        continue;
      }
      DBIDRef random_obj = DBIDUtil.randomSample(cluster.getValue(), rnd);
      medoids.add(random_obj);
      clusterLabels[i++] = cluster.getKey();
    }
    // future check - when all is fixed if choosing uncolored means is
    // better
    int trys = 0;
    while(medoids.size() < k && trys < k) {
      for(Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()) {
        
        if (medoids.size() >= k) {
          break;
        }
        if(cluster.getKey() == 0) {
          continue;
        }
        trys++;
        DBIDRef random_obj = DBIDUtil.randomSample(cluster.getValue(), rnd);
        // continue if the object is already in medoids
        if(medoids.contains(random_obj)) {
          continue;
        }
        medoids.add(random_obj);
        clusterLabels[i++] = cluster.getKey();
      }
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


    @Override
    public void configure(Parameterization config) {
      super.configure(config);
    }

    @Override
    public LabelRandomInitialization<?> make() {
      return new LabelRandomInitialization<>(rnd);
    }


  }
}
