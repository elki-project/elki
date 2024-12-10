package elki.clustering.kmedoids;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Clustering;
import elki.data.LabelList;
import elki.data.model.MedoidModel;
import elki.data.type.NoSupportedDataTypeException;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.logging.statistics.StringStatistic;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;


public class SeededKmedoids<O> extends FasterPAM<O> {

    private final Random rnd;

    private WritableIntegerDataStore labelsMaps;

    /**x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(SeededKmedoids.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = SeededKmedoids.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public SeededKmedoids(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer, RandomFactory rnd) {
        super(distance, k, maxiter, initializer);
        this.rnd = rnd.getSingleThreadedRandom();
    }

    @Override
    public Clustering<MedoidModel> autorun(Database database) {
        // Prefer a true class label
        try {
            return run(database.getRelation(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH), database.getRelation(TypeUtil.LABELLIST));
        }
        catch(NoSupportedDataTypeException e) {
            // Otherwise, try any label-like.
            return run(database.getRelation(getInputTypeRestriction()[0]));
        }
    }

    public Clustering<MedoidModel> run(Relation<O> relation, Relation<LabelList> labels) {
        return run(relation, labels, k, new QueryBuilder<>(relation, distance)
                .precomputed()
                .distanceQuery());
    }

    private void saveLabels(Relation<LabelList> labels){
        DBIDs labelsIds = labels.getDBIDs();
        DBIDIter iter = labelsIds.iter();
        for (; iter.valid(); iter.advance()) {
//      System.out.println(iter);
            LabelList o = labels.get(iter);
//      String s = o.get(0);
            String[] splitLabels = (labels.get(iter)).get(0).split("_"); // this retrieves the labels :)
            int classId = Integer.parseInt(splitLabels[1]); // TODO change this back
            String trainDataFlag = splitLabels[2]; // TODO change this back

            if (trainDataFlag.equals("1")){
                labelsMaps.put(iter, classId);
            }
            // default is handled when setting up the map
        }
    }

    public Clustering<MedoidModel> run(Relation<O> relation, Relation<LabelList> labels, int k, DistanceQuery<? super O> distQ) {

        DBIDs ids = relation.getDBIDs();
        // remove this as a propery of an algo
        this.labelsMaps = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
        saveLabels(labels);

        ArrayModifiableDBIDs medoids = initMedoids(ids, k, distQ);
        WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
        Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();

        new FasterPAM.Instance(distQ, ids, assignment).run(medoids, maxiter);
        getLogger().statistics(optd.end());

        return wrapResult(ids, assignment, medoids, "Seeded K-medoids Clustering (semi-supervised)");
    }

    /**
     * Init medoids, so that each label is present at least once in the list
     *
     */
    protected ArrayModifiableDBIDs initMedoids(DBIDs ids, int k, DistanceQuery<? super O> distQ) {
        if(getLogger().isStatistics()) {
            getLogger().statistics(new StringStatistic(getClass().getName() + ".initialization", initializer.toString()));
        }
        Duration initd = getLogger().newDuration(getClass().getName() + ".initialization-time").begin();
        // init medoids
        ArrayModifiableDBIDs medoids = DBIDUtil.newArray(k);
        initialSeededMedoids(ids, k, medoids, distQ);

        getLogger().statistics(initd.end());
        if(medoids.size() != k) {
            throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
        }
        return medoids;
    }

    private void initialSeededMedoids(DBIDs ids, int k, ArrayModifiableDBIDs medoids, DistanceQuery<? super O> distQ) {
        // make a map label -> objects
        Map<Integer, ArrayModifiableDBIDs> labelToDataMap = new HashMap<>();
        clusterIdObjectsMap(ids, labelToDataMap);
        // get unlabelled data
        ArrayModifiableDBIDs unlabelledData = DBIDUtil.newArray();  
        getUnlabelledData(ids, unlabelledData);

        // we do not know which "classes" to forget .. abort
        if (k < labelToDataMap.size()){
          throw new RuntimeException("Number of expected clusters in labelled data is larger than k. Consider increasing k.");
        }
        // if k is the same as the number of labels, then for each partition find the medoid
        else if (k == labelToDataMap.size()) {
            for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
                DBIDRef medoid = findMedoidFromData(cluster.getValue(),distQ);
                medoids.add(medoid);
            }
            return;
        }
        // k is larger than number of classes
        // randomly separate data in each cluster and pick the medoids
        else if ((k % labelToDataMap.size()) != 0){
            int splitsPerCluster = k / labelToDataMap.size();
            int missingSeedsNum = k % labelToDataMap.size();
            Set<Integer> clustersWithMoreSeeds = new HashSet<>();
            while(clustersWithMoreSeeds.size() < missingSeedsNum){
                clustersWithMoreSeeds.add(rnd.nextInt(labelToDataMap.size()));
            }
            int pos = 0;
            // pick k random clusters that will get more data
            for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
                int myClusterSplits = clustersWithMoreSeeds.contains(pos++) ?
                        splitsPerCluster + 1 :
                        splitsPerCluster;
                ArrayDBIDs[] arrayDBIDs = DBIDUtil.randomSplit(cluster.getValue(), myClusterSplits, rnd);

                for (int i = 0; i < arrayDBIDs.length; i++) {
                    DBIDRef medoid = findMedoidFromData(arrayDBIDs[i],distQ);
                    medoids.add(medoid);
                }
            }
            return;
        }
        int splitsPerCluster = k / labelToDataMap.size();
        for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
            ArrayDBIDs[] arrayDBIDs = DBIDUtil.randomSplit(cluster.getValue(), splitsPerCluster, rnd);
            for (int i = 0; i < arrayDBIDs.length; i++) {
              //deal with empty clusters
              if (arrayDBIDs[i].size() == 0) {
                while(true) {
                  ModifiableDBIDs potentialObjs = DBIDUtil.randomSample(unlabelledData, 1, rnd);
                  DBIDRef medoid = potentialObjs.iter();
                  if(!medoids.contains(medoid)) {
                    medoids.add(medoid);
                    break;
                  }
                }
              }
              // deal with non-empty clusters
              else {
                DBIDRef medoid = findMedoidFromData(arrayDBIDs[i],distQ);
                medoids.add(medoid);
              }
            }
        }
    }

    private void getUnlabelledData(DBIDs ids, ArrayModifiableDBIDs unlabelledData){
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
            int myLabel = labelsMaps.intValue(iter);
            if (myLabel == -1) unlabelledData.add(iter);
        }
    }

    private void clusterIdObjectsMap(DBIDs ids,Map<Integer, ArrayModifiableDBIDs> labelToDataMap){
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
            int myLabel = labelsMaps.intValue(iter);
            if (myLabel == -1) continue; // we don't want to use unlabelled data
            if (labelToDataMap.containsKey(myLabel)){
                labelToDataMap.get(myLabel).add(iter);
            }
            else {
                ArrayModifiableDBIDs curLabelCl = DBIDUtil.newArray();
                curLabelCl.add(iter);
                labelToDataMap.put(myLabel,curLabelCl);
            }
        }
    }


    private DBIDRef findMedoidFromData(ArrayDBIDs cluster, DistanceQuery<? super O> distQ){
        DBIDArrayIter otherIt = cluster.iter();
        double minSum = Double.MAX_VALUE;
        DBIDVar medoid = DBIDUtil.newVar();

        for (DBIDIter iter = cluster.iter(); iter.valid(); iter.advance()){
            double sum = 0;
            for (otherIt.seek(0); otherIt.valid(); otherIt.advance()){
                double d = distQ.distance(iter, otherIt);
                sum += d;
            }
            if (sum < minSum){
                minSum = sum;
                medoid.set(iter);
            }
        }
        return medoid;
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends FasterPAM.Par<O> {
        OptionID SEED_ID = new OptionID("kmedoids.seed", "The random number generator seed.");
        /**
         * Random generator
         */
        protected RandomFactory rnd;

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
            new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
        }
        @Override
        public SeededKmedoids<O> make() {
            return new SeededKmedoids<>(distance, k, maxiter, initializer, rnd);
        }
    }
}
