package elki.clustering.kmedoids.initialization;

import java.util.ArrayList;
import java.util.stream.IntStream;

import elki.clustering.ClusteringAlgorithmUtil;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.kmedoids.FasterPAM;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

public class PreclusteredInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  KMedoidsInitialization<O> initializer;

  public PreclusteredInitialization(RandomFactory rnd, KMedoidsInitialization<O> initializer) {
    super(rnd);
    this.initializer = initializer;
  }

  @Override
  public int[] chooseInitialMedoids(int k, int noLabels, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids) {
    
        ArrayModifiableDBIDs initialMedoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distance));
        WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
        FasterPAM.Instance inst = new FasterPAM.Instance(distance, ids, assignment);
        inst.run(initialMedoids, 2);
        ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, assignment, initialMedoids.size());

        medoids.addDBIDs(initialMedoids);
        DBIDArrayMIter miter = medoids.iter();
        int[] clusterLabels = new int[k];
        int[] labelCount = new int[noLabels];
        int[][] preColors = new int[k][];

        for(int i = 0; i < k; miter.advance(), i++) {
          preColors[i] = clustercolors(clusters[i], labels, noLabels);
          final int label = labels.intValue(miter);
          // // color based on medoid
          // if (label !=0){
          //   clusterLabel[i] = label;
          //   labelCount[label] += 1;
          //   continue;
          // }
          // color based on cluster most frequent color
          if (preColors[i].length > 1  && preColors[i][0] == 0) {
            clusterLabels[i] = preColors[i][1];
            labelCount[preColors[i][1]] += 1;
            if (label != 0 && label != preColors[i][1]){
              DBIDs candidates = filterForColor(clusters[i], labels, preColors[i][1]);
              DBIDRef closest = findClosestPoint(candidates, miter, distance);
              miter.seek(i).setDBID(closest);
            }
          } else {
            // most frequent color is no color, pick the second most frequent
            clusterLabels[i] = preColors[i][0];
            labelCount[preColors[i][0]] += 1;
            if (label != 0 && label != preColors[i][0]){
              DBIDs candidates = filterForColor(clusters[i], labels, preColors[i][0]);
              DBIDRef closest = findClosestPoint(candidates, miter, distance);
              miter.seek(i).setDBID(closest);
            }
          }
        }
        // not every label is neccessarily the most frequent
        // find clusters for the remaining labels
        boolean allLabels = true;
        for(int i = 1; i < noLabels; i++) {
          if(labelCount[i] == 0) {
            allLabels = false;
            break;
          }
        }
        if (allLabels) {
          return clusterLabels;
        }

        // find labels with multiple Clusters
        ArrayList<Integer> multiColor = new ArrayList<>();
        for(int j = 1; j < noLabels; j++) {
          if(labelCount[j] > 1) {
            multiColor.add(j);
          }
        }

        for(int i = 1; i < noLabels; i++) {
          if (labelCount[i] > 0){
            continue;
          }
          // clusters that contain the label
          ArrayList<Integer> clusterWithColor = new ArrayList<>();
          for(int j = 0; j < k; j++) {
            for (int l = 0; l<preColors[j].length; l++){
              if(preColors[j][l] == i) {
                clusterWithColor.add(j);
                break;
              }
            }
          }
          int best_redundant = -1;
          int bestRank_redundant = noLabels+1;
          int best = -1;
          int bestRank = noLabels + 1;
          // search clusters with redundant labels and points in the missing color
          for (Integer j : clusterWithColor){
            for (int l = 0; l < preColors[j].length; l++){
              if(multiColor.contains(clusterLabels[j])) {
                if (preColors[j][l] == i && l < bestRank_redundant){
                  best_redundant = j;
                  bestRank_redundant = l;
                  break;
                  }
              }
              if (preColors[j][l] == i && l < bestRank){
                best = j;
                bestRank = l;
                break;
              }
            }
          }
          if (best_redundant==-1) {
            // replace random with point as best not redundant
            DBIDs coloredPoints = filterForColor(clusters[best], labels, i);
            ArrayModifiableDBIDs possibleMedoids = DBIDUtil.newArray();
            for (miter.seek(0); miter.valid(); miter.advance()){
              if (clusterLabels[miter.getOffset()] == 0 || multiColor.contains(clusterLabels[miter.getOffset()])){
                possibleMedoids.add(miter);
              }
            }
            DBIDRef closestMedoid = findClosestPoint(possibleMedoids, miter.seek(best), distance);
            DBIDRef clostestPoint = findClosestPoint(coloredPoints, closestMedoid, distance);

            int closestOffset = -1;
            for (miter.seek(0); miter.valid(); miter.advance()){
              if (DBIDUtil.equal(miter, closestMedoid)){
                closestOffset = miter.getOffset();
                miter.setDBID(clostestPoint);
                break;
              }
            }

            labelCount[clusterLabels[closestOffset]] -= 1;
            labelCount[i] += 1;
            if (labelCount[clusterLabels[closestOffset]] <= 1 && multiColor.contains(clusterLabels[closestOffset]) && clusterLabels[closestOffset] != 0){
              multiColor.remove(Integer.valueOf(clusterLabels[closestOffset]));
            }
            clusterLabels[closestOffset] = i;
            miter.seek(closestOffset).setDBID(clostestPoint);
            continue;
          }
          labelCount[clusterLabels[best_redundant]] -= 1;
          labelCount[i] += 1;
          if (labelCount[clusterLabels[best_redundant]] <= 1 && multiColor.contains(clusterLabels[best_redundant]) && clusterLabels[best_redundant] != 0){
            multiColor.remove(Integer.valueOf(clusterLabels[best_redundant]));
          }
          clusterLabels[best_redundant] = i;
          DBIDs candidates = filterForColor(clusters[best_redundant], labels, i);
          DBIDRef closest = findClosestPoint(candidates, miter.seek(best_redundant), distance);
          miter.setDBID(closest);
        }
        return clusterLabels;
  }

      private int[] clustercolors(DBIDs ids, WritableIntegerDataStore labels, int noLabels){
      int[] colors = new int[noLabels];
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
        colors[labels.intValue(iter)] += 1;
      }
      int[] sortedIndices = IntStream.range(0, colors.length)
                .boxed().sorted((i, j) -> Integer.compare(colors[j], colors[i])) // sort desc
                .mapToInt(ele -> ele).filter( id -> colors[id] > 0).toArray();
      return sortedIndices;
    }

    protected DBIDs filterForColor(DBIDs ids, WritableIntegerDataStore labels, int color) {
      ArrayModifiableDBIDs filtered = DBIDUtil.newArray();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(labels.intValue(iter) == color || labels.intValue(iter) == 0) {
          filtered.add(iter);
        }
      }
      return filtered;
    }

    protected DBIDRef findClosestPoint(DBIDs ids, DBIDRef ref, DistanceQuery<? super O> distQ){
      DBIDVar closest = DBIDUtil.newVar();
      double minDist = Double.MAX_VALUE;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
        double dist = distQ.distance(ref, iter);
        if(dist < minDist && !DBIDUtil.equal(ref, iter)){
          minDist = dist;
          closest.set(iter);
        }
      }
      return closest;
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
    public PreclusteredInitialization<?> make() {
      return new PreclusteredInitialization<>(rnd, initializer);
    }

  }
}
