package elki.clustering.kmedoids.initialization;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

public class UnsupervisedInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  KMedoidsInitialization<O> initializer;

  public UnsupervisedInitialization(RandomFactory rnd, KMedoidsInitialization<O> initializer) {
    super(rnd);
    this.initializer = initializer;
  }

  @Override
  public int[] chooseInitialMedoids(int k, int noLabels, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids) {
    medoids.addDBIDs(initializer.chooseInitialMedoids(k, ids, distance));
    int[] clusterLabels = new int[k];
    DBIDArrayIter miter = medoids.iter();
    int[] labelCount = new int[noLabels + 1];

    for (int i = 0; miter.valid(); miter.advance(),i++) {
      final int label = labels.intValue(miter);
      clusterLabels[i] = label;
      labelCount[label] += 1; 
    }

    int l = 0;
    int p = 0;
    labelloop:
    for (int i = 1; i < noLabels + 1; i++) {
      if (labelCount[i] == 0) {
        // if(labelCount[0]>0){
        //   labelCount[0]-=1;
        //   labelCount[i]+=1;
        //   for(;p<k;p++){
        //     if (clusterLabels[p] == 0) {
        //       clusterLabels[p] = i;
        //       continue labelloop;
        //     }
        //   }
        // }
        while(labelCount[l]<2 && l > 0 || labelCount[l]==0){
          l++;
          p=0;
        }
        assert l < noLabels + 1;
        labelCount[l] -=1;
        labelCount[i] +=1;
        for(;p<k;p++){
          if (clusterLabels[p] == l) {
            clusterLabels[p] = i;
            if(l != 0){
              medoids.set(p, findPointOfColor(ids, l, medoids, labels));
            }
            continue labelloop;
          }
        }
      }
    }
    return clusterLabels;
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
