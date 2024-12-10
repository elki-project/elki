package elki.clustering.kmedoids;

import java.util.Arrays;
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
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

public class COPKmedoids<O> extends FastPAM1<O> {

    private final Random rnd;

    private WritableIntegerDataStore labelsMaps;

    /**x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(COPKmedoids.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = COPKmedoids.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public COPKmedoids(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer, RandomFactory rnd) {
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
        int[] clusterLabel = new int[k];

        ArrayModifiableDBIDs medoids = initMedoids(ids, k, clusterLabel);
        WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
        Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();

        new Instance(distQ, ids, assignment, labelsMaps, clusterLabel).run(medoids, maxiter);
        getLogger().statistics(optd.end());
        areAllMedsdPartOfClusters(medoids, assignment);


        return wrapResult(ids, assignment, medoids, "Seeded K-medoids Clustering (semi-supervised)");
    }

    public boolean areAllMedsdPartOfClusters(ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment){
        DBIDArrayMIter miter = medoids.iter();
        for (; miter.valid(); miter.advance()){
            if (assignment.intValue(miter) != miter.getOffset()){
                System.out.println("ERROR: i'm loosing medoids");
                return false;
            }
        }
        return true;
    }

    /**
     * Init medoids, so that each label is present at least once in the list
     *
     */
    protected ArrayModifiableDBIDs initMedoids(DBIDs ids, int k, int[] clusterLabel) {
        if(getLogger().isStatistics()) {
            getLogger().statistics(new StringStatistic(getClass().getName() + ".initialization", initializer.toString()));
        }
        Duration initd = getLogger().newDuration(getClass().getName() + ".initialization-time").begin();
        // init medoids
        ArrayModifiableDBIDs medoids = DBIDUtil.newArray(k);
        initRandomMedoids(ids, k, medoids, clusterLabel);

        getLogger().statistics(initd.end());
        if(medoids.size() != k) {
            throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
        }
        return medoids;
    }

    private void initRandomMedoids(DBIDs ids, int k, ArrayModifiableDBIDs medoids, int[] clusterLabel){
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
        else if (k == labelToDataMap.size()){
            int pos = 0;
            for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
                DBIDRef seed = getRandomSeed(cluster.getValue());
                medoids.add(seed);
                clusterLabel[pos++] = cluster.getKey();
            }
            return;
        }
        // k is larger than number of classes
        // randomly separate data in each cluster and pick the medoids
        if ((k % labelToDataMap.size()) != 0){

            int splitsPerCluster = k / labelToDataMap.size();
            int missingSeedsNum = k % labelToDataMap.size();
            int pos = 0;
            Set<Integer> clustersWithMoreSeeds = new HashSet<>();
            while(clustersWithMoreSeeds.size() < missingSeedsNum){
                clustersWithMoreSeeds.add(rnd.nextInt(labelToDataMap.size()));
            }
            int clId = 0;
            // pick k random clusters that will get more data
            for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
                int myClusterSplits = clustersWithMoreSeeds.contains(clId++) ?
                        splitsPerCluster + 1 :
                        splitsPerCluster;
                ArrayDBIDs[] arrayDBIDs = DBIDUtil.randomSplit(cluster.getValue(), myClusterSplits, rnd);

                for (int i = 0; i < arrayDBIDs.length; i++) {
                    DBIDRef seed = getRandomSeed(arrayDBIDs[i]);
                    medoids.add(seed);
                    clusterLabel[pos++] = cluster.getKey();
                }
            }
            return;
        }
        int splitsPerCluster = k / labelToDataMap.size();
        int pos = 0;
        for (Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()){
            if(pos > 190){
                System.out.println("Cluster id " + cluster.getKey());
            }
            ArrayDBIDs[] arrayDBIDs = DBIDUtil.randomSplit(cluster.getValue(), splitsPerCluster, rnd);
            for (int i = 0; i < arrayDBIDs.length; i++) {
              //deal with empty clusters
              if (arrayDBIDs[i].size() == 0) {
                while(true) {
                  DBIDRef seed = getRandomSeed(unlabelledData);
                  if(!medoids.contains(seed)) {
                    medoids.add(seed);
                    clusterLabel[pos++] = -1;
                    break;
                  }
                }
              }
              // deal with non-empty clusters
              else {
                DBIDRef seed = getRandomSeed(arrayDBIDs[i]);
                medoids.add(seed);
                clusterLabel[pos++] = cluster.getKey();
              }
            }

        }
    }


    private void getUnlabelledData(DBIDs ids, ArrayModifiableDBIDs unlabelledData) {
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        int myLabel = labelsMaps.intValue(iter);
        if(myLabel == -1)
          unlabelledData.add(iter);
      }
    }

    private DBIDRef getRandomSeed(ArrayDBIDs dbiDs){
        DBIDVar medoid = DBIDUtil.randomSample(dbiDs, rnd);
        return medoid;
    }

    private void clusterIdObjectsMap(DBIDs ids, Map<Integer, ArrayModifiableDBIDs> labelToDataMap){
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

    protected static class Instance extends FastPAM1.Instance {
        private WritableIntegerDataStore labelsMaps;
        private int[] clusterLabel;
        private int[] countLabelledInCl;

        /**
         * Constructor.
         *
         * @param distQ      Distance query
         * @param ids        IDs to process
         * @param assignment Cluster assignment
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel) {
            super(distQ, ids, assignment);
            this.labelsMaps = labelsMaps;
            this.clusterLabel = clusterLabel;
            this.countLabelledInCl = new int[clusterLabel.length];
        }

        /**
         * Run the PAM optimization phase.
         *
         * @param medoids Medoids list
         * @param maxiter
         * @return final cost
         */
        @Override
        protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
            // Swap phase
            IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("COP-Kmedoids iteration", LOG) : null;
            double tc = 0;
            int iteration = 0, swaps = 0;
            while (iteration < maxiter) {
                ++iteration;
                LOG.incrementProcessed(prog);
                tc = assignToNearestCluster(medoids);

                if (LOG.isStatistics()) {
                    LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
                }
                ArrayModifiableDBIDs updatedMedoids =  DBIDUtil.newArray();
                int newSwaps = recomputeMedoids(medoids, updatedMedoids);
                if (newSwaps == 0) {
                    areAllMedsdPartOfClusters(medoids);
                    break; // Converged
                }
                DBIDArrayMIter newMedsIter = updatedMedoids.iter();
                for (; newMedsIter.valid(); newMedsIter.advance()) {
                    medoids.set(newMedsIter.getOffset(), newMedsIter);
                }
                swaps += newSwaps;
                areAllMedsdPartOfClusters(medoids);
            }
            areAllMedsdPartOfClusters(medoids);
            // TODO: we may have accumulated some error on tc.
            LOG.setCompleted(prog);
            if (LOG.isStatistics()) {
                LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
                LOG.statistics(new LongStatistic(KEY + ".swaps", swaps));
                LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
            }
            // Cleanup
            for (DBIDIter it = ids.iter(); it.valid(); it.advance()) {
                assignment.putInt(it, assignment.intValue(it));
            }
            areAllMedsdPartOfClusters(medoids);

            for (int i = 0; i < clusterLabel.length; i++) {
                System.out.println("Cluster id " + i + " label " + clusterLabel[i]);
            }
            return tc;
        }

        public boolean areAllMedsdPartOfClusters(ArrayModifiableDBIDs medoids){
            DBIDArrayMIter miter = medoids.iter();
            ArrayDBIDs[] clusters = getClusters();

            for (; miter.valid(); miter.advance()) {
                // System.out.println(miter.toString());
                // System.out.println("is present in cluster " +
                // miter.getOffset());
                if (assignment.intValue(miter) != miter.getOffset()) {
                    System.out.println("ERROR: i'm loosing medoids");
                    return false;
                }
            }

            for (int i = 0; i < clusters.length; i++){
                System.out.println("cluster number " + i);
                DBIDRef medoid = miter.seek(i);
                DBIDIter iter = clusters[i].iter();
                boolean b = false;
                for (; iter.valid(); iter.advance()){
                    if(DBIDUtil.equal(medoid, iter)) {
                        b = true;
                    }
                }
                if (!b){
                    System.out.println("ERROR: i am somehow in the assignment, but not in the cluster???");
                }
            }
            return true;
        }

        private ArrayDBIDs[] getClusters(){
            int numOfCls = clusterLabel.length;
            ArrayModifiableDBIDs[] result = new ArrayModifiableDBIDs[numOfCls];
            for (int i = 0; i < numOfCls; i++){
                result[i] = DBIDUtil.newArray();
            }

            DBIDIter iter = ids.iter();
            for (; iter.valid(); iter.advance()){
                int clId = assignment.intValue(iter);
                result[clId].add(iter);
            }
            return result;
        }

        private int recomputeMedoids(ArrayModifiableDBIDs medoids, ArrayModifiableDBIDs updatedMedoids){
            ArrayDBIDs[] clusters = getClusters();
            DBIDArrayMIter medIter = medoids.iter();
            int swaps = 0;
            for (int i = 0; i < clusters.length; i++){
                DBIDRef newMedoid = findMedoid(clusters[i]);
                DBIDRef oldMedoid = medIter.seek(i);
                if (!DBIDUtil.equal(newMedoid, oldMedoid)){
                    swaps++;
                }
                updatedMedoids.add(newMedoid);
            }
            return swaps;
        }

        private DBIDRef findMedoid(ArrayDBIDs cluster){
            double minSum = Double.MAX_VALUE;
            DBIDVar medoid = DBIDUtil.newVar();
            for (DBIDIter iter = cluster.iter(); iter.valid(); iter.advance()){
                double sum = 0;
                for (DBIDIter other = cluster.iter(); other.valid(); other.advance()){
                    double d = distQ.distance(iter, other);
                    sum += d;
                }
                if (sum < minSum){
                    minSum = sum;
                    medoid.set(iter);
                }
            }
            return medoid;
        }

        private boolean validObjClusterPair(DBIDRef objIdx, int clusterId) {
            if (labelsMaps.intValue(objIdx) == -1){
                return true; // object does not have a label, so we do not care where its clustered
            }
            if (clusterLabel[clusterId] == -1){
                // the cluster itself has no label, so we can assign the object here
                return true;
            }
            // both object and cluster have labels, so we compare if they are the same
            return labelsMaps.intValue(objIdx) == clusterLabel[clusterId];
        }

        @Override
        protected double assignToNearestCluster(ArrayDBIDs medoids) {
            DBIDArrayIter miter = medoids.iter();
            Arrays.fill(countLabelledInCl, 0);
            double cost = 0.;

            for (DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
                double mindist = Double.MAX_VALUE;
                int minindx = -1;
                for(miter.seek(0); miter.valid(); miter.advance()) {
                    // invalid combination of object and medoid, so they can
                    // never end up in the same cluster
                    if (!validObjClusterPair(iditer, miter.getOffset())){
                        continue;
                    }
                    final double dist = distQ.distance(iditer, miter);
                    if (dist < mindist) {
                        minindx = miter.getOffset();
                        mindist = dist;
                    }
                }
                if (minindx < 0) {
                    throw new AbortException("Too many infinite distances. Cannot assign objects.");
                }
                int objLabel = labelsMaps.intValue(iditer);
                countLabelledInCl[minindx] += objLabel == -1 ? 0 : 1;
                assignment.put(iditer, minindx);
                // if cluster does not have label, and object does, then update clusterLabel
                if (clusterLabel[minindx] == -1 && objLabel != -1) {
                    clusterLabel[minindx] = objLabel;
                }
                cost += mindist;
            }
            for (int i = 0; i < clusterLabel.length; i++){
                // nullify the label of the cluster
                if (countLabelledInCl[i] == 0){
                    clusterLabel[i] = -1;
                }
            }
            return cost;
        }
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
    public static class Par<O> extends FastPAM1.Par<O> {

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
        public COPKmedoids<O> make() {
            return new COPKmedoids<>(distance, k, maxiter, initializer, rnd);
        }
    }

}
