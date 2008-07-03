package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.EMClusters;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * todo Arthur comment
 *
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class SubspaceEM<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V> {

    /**
     * OptionID for {@link #DELTA_PARAM}
     */
    public static final OptionID SUBSPACE_EM_DELTA = OptionID.getOrCreateOptionID(
        "subspaceem.delta",
        "The termination criterion for maximization of E(M): " +
            "E(M) - E(M') < subspaceem.delta"
    );

    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID SUBSPACE_EM_K = OptionID.getOrCreateOptionID(
        "subspaceem.k",
        "The number of clusters to find."
    );

    /**
     * Small value to increment diagonally of a matrix
     * in order to avoid singularity befor building the inverse.
     */
    private static final double SINGULARITY_CHEAT = 1E-9;

    /**
     * Parameter to specify the number of clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -subspaceem.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(
        SUBSPACE_EM_K,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the termination criterion for maximization of E(M): E(M) - E(M') < subspaceem.delta,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.0} </p>
     * <p>Key: {@code -subspaceem.delta} </p>
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(
        SUBSPACE_EM_DELTA,
        new GreaterEqualConstraint(0.0),
        0.0);

    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;

    /**
     * Keeps delta - a small value as termination criterion in expectation maximization
     */
    private double delta;

    /**
     * Stores the result.
     */
    private Clusters<V> result;

    /**
     * todo arthur comment
     */
    public SubspaceEM() {
        super();
        debug = true;
        addOption(K_PARAM);
        addOption(DELTA_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering#getResult()
     */
    public ClusteringResult<V> getResult() {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("SubspaceEM", "SubspaceEM", "", "");
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
     */
    @Override
    public void runInTime(Database<V> database) throws IllegalStateException {
        if (database.size() == 0) {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
//        int n = database.size();
        // initial models
        if (isVerbose()) {
            verbose("initializing " + k + " models");
        }

        List<V> means = initialMeans(database);
        int dimensionality = means.get(0).getDimensionality();
        Matrix[] eigensystems = initialEigensystems(dimensionality);
        Matrix selectionWeak = Matrix.zeroMatrix(dimensionality);
        selectionWeak.set(dimensionality - 1, dimensionality - 1, 1);
        Matrix selectionStrong = Matrix.unitMatrix(dimensionality);
        selectionStrong.set(dimensionality - 1, dimensionality - 1, 0);

        double[] standardDeviation = new double[k];
        double[] normDistributionFactor = new double[k];
        double[] clusterWeight = new double[k];

        for (int i = 0; i < k; i++) {
            standardDeviation[i] = 1;
            clusterWeight[i] = 1.0 / k;
            normDistributionFactor[i] = 1.0 / (standardDeviation[i] * Math.sqrt(2 * Math.PI));

        }
        // assign probabilities to database objects
        assignProbabilities(database, normDistributionFactor, standardDeviation, clusterWeight, means, eigensystems, selectionStrong);
        double emNew = expectationOfMixture(database);
        // iteration unless no change
        if (isVerbose()) {
            verbose("iterating subspace EM");
        }
        double em;
        int it = 0;
        do {
            it++;
            gnuplot("Iteration_" + it + "_", database, means, eigensystems);
            if (isVerbose()) {
                verbose("iteration " + it + " - expectation value: " + emNew);
            }
            em = emNew;

            // recompute models
            List<V> meanSums = new ArrayList<V>(k);
            double[] sumOfClusterProbabilities = new double[k];
            Matrix[] covarianceMatrix = new Matrix[k];
            for (int i = 0; i < k; i++) {
                clusterWeight[i] = 0.0;
                meanSums.add(means.get(i).nullVector());
                covarianceMatrix[i] = Matrix.zeroMatrix(dimensionality);
            }
            // weights and means
//            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
//            {
//                Integer id = iter.next();
//                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
//                
//                for(int i = 0; i < k; i++)
//                {
//                    sumOfClusterProbabilities[i] += clusterProbabilities.get(i);
//                    V summand = database.get(id).multiplicate(clusterProbabilities.get(i));
//                    V currentMeanSum = meanSums.get(i).plus(summand);
//                    meanSums.set(i, currentMeanSum);
//                }
//            }
//            
//            for(int i = 0; i < k; i++)
//            {
//                clusterWeight[i] = sumOfClusterProbabilities[i] / n;
//                V newMean = meanSums.get(i).multiplicate(1 / sumOfClusterProbabilities[i]);
//                means.set(i, newMean);
//            }
            Integer[][] hardClustering = hardClustering(database);
            for (int i = 0; i < k; i++) {
                for (Integer id : hardClustering[i]) {
                    double clusterProbability = ((List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id)).get(i);
                    sumOfClusterProbabilities[i] += clusterProbability;
                    V summand = database.get(id).multiplicate(clusterProbability);
                    V currentMeanSum = meanSums.get(i).plus(summand);
                    meanSums.set(i, currentMeanSum);
                }
                clusterWeight[i] = hardClustering[i].length == 0 ? 0 : sumOfClusterProbabilities[i] / hardClustering[i].length;
                V newMean = meanSums.get(i).multiplicate(1 / sumOfClusterProbabilities[i]);
                means.set(i, newMean);
            }
            // covariance matrices
//            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
//            {
//                Integer id = iter.next();
//                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
//                V instance = database.get(id);
//                for(int i = 0; i < k; i++)
//                {
//                    V difference = instance.plus(means.get(i).negativeVector());
//                    Matrix newCovMatr = covarianceMatrix[i].plus(difference.getColumnVector().times(difference.getRowVector()).times(clusterProbabilities.get(i)));
//                    covarianceMatrix[i] = newCovMatr;
//                }
//            }

            for (int i = 0; i < k; i++) {
                for (Integer id : hardClustering[i]) {
                    V instance = database.get(id);
                    //List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                    V difference = instance.plus(means.get(i).negativeVector());
                    Matrix newCovMatr = covarianceMatrix[i].plus(difference.getColumnVector().times(difference.getRowVector()));//.times(clusterProbabilities.get(i)));
                    covarianceMatrix[i] = newCovMatr;
                }
            }
            // eigensystems and standard deviations
            for (int i = 0; i < k; i++) {
                covarianceMatrix[i] = covarianceMatrix[i].times(1 / sumOfClusterProbabilities[i]).cheatToAvoidSingularity(SINGULARITY_CHEAT);
                eigensystems[i] = covarianceMatrix[i].eig().getV();
                // standard deviation for the points from database again weighted accordingly to their cluster probability?
                double variance = 0;
//                for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
//                {
//                    Integer id = iter.next();
//                    List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
//                    double distance = SubspaceEM.distance(database.get(id),means.get(i),eigensystems[i].times(selectionStrong));
//                    variance += distance * distance;// * clusterProbabilities.get(i);
//                }
                for (Integer id : hardClustering[i]) {
                    double distance = SubspaceEM.distance(database.get(id), means.get(i), eigensystems[i].times(selectionStrong));
                    variance += distance * distance;
                }
                standardDeviation[i] = hardClustering[i].length == 0 ? 1 : Math.sqrt(variance / hardClustering[i].length);
                if (debug) {
                    if (standardDeviation[i] == 0) {
                        debugFine(i + ": " + standardDeviation[i]);
                    }
                }
                normDistributionFactor[i] = 1.0 / (standardDeviation[i] * Math.sqrt(2 * Math.PI));
            }
            // reassign probabilities
            assignProbabilities(database, normDistributionFactor, standardDeviation, clusterWeight, means, eigensystems, selectionStrong);

            // new expectation
            emNew = expectationOfMixture(database);
            if (debug && emNew <= em) {
                debugFine("expectation value decreasing: old=" + em + " new=" + emNew + " difference=" + (em - emNew));
            }

        }
        while (Math.abs(em - emNew) > delta);

        // provide a hard clustering
        if (isVerbose()) {
            verbose("\nassigning clusters");
        }
        result = new EMClusters<V>(hardClustering(database), database);
        result.associate(SimpleClassLabel.class);
        // provide models within the result
        for (int i = 0; i < k; i++) {
            SimpleClassLabel label = new SimpleClassLabel();
            label.init(result.canonicalClusterLabel(i));
            Matrix transposedWeakEigenvectors = eigensystems[i].times(selectionWeak).transpose();
            Matrix vTimesMean = transposedWeakEigenvectors.times(means.get(i).getColumnVector());
            double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
            double[][] we = transposedWeakEigenvectors.getArray();
            double[] b = vTimesMean.getColumn(0).getRowPackedCopy();
            System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());
            LinearEquationSystem lq = new LinearEquationSystem(a, b);
            lq.solveByTotalPivotSearch();
            CorrelationAnalysisSolution<V> solution = new CorrelationAnalysisSolution<V>(lq, database, eigensystems[i].times(selectionStrong), eigensystems[i].times(selectionWeak), eigensystems[i].times(selectionWeak).times(eigensystems[i].transpose()), means.get(i).getColumnVector());
            result.appendModel(label, solution);
        }
        // TODO: instead of hard clustering: overlapping subspace clusters assigned with dist < 3*sigma

    }

    /**
     * todo arthur comment
     */
    protected Integer[][] hardClustering(Database<V> database) {
        List<List<Integer>> hardClusters = new ArrayList<List<Integer>>(k);
        for (int i = 0; i < k; i++) {
            hardClusters.add(new LinkedList<Integer>());
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
            int maxIndex = 0;
            double currentMax = 0.0;
            for (int i = 0; i < k; i++) {
                if (clusterProbabilities.get(i) > currentMax) {
                    maxIndex = i;
                    currentMax = clusterProbabilities.get(i);
                }
            }
            hardClusters.get(maxIndex).add(id);
        }
        Integer[][] resultClusters = new Integer[k][];
        for (int i = 0; i < k; i++) {
            resultClusters[i] = hardClusters.get(i).toArray(new Integer[hardClusters.get(i).size()]);
        }
        return resultClusters;
    }

    /**
     * The expectation value of the current mixture of distributions.
     * <p/>
     * Computed as the sum of the logarithms of the prior probability of each instance.
     *
     * @param database the database where the prior probability of each instance is associated
     * @return the expectation value of the current mixture of distributions
     */
    protected double expectationOfMixture(Database<V> database) {
        double sum = 0.0;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            double priorProbX = database.getAssociation(AssociationID.PROBABILITY_X, id);
            double logP = Math.log(priorProbX);
            sum += logP;
            if (debug && false) {
                debugFine("\nid=" + id + "\nP(x)=" + priorProbX + "\nlogP=" + logP + "\nsum=" + sum);
            }
        }
        return sum;
    }

    /**
     * todo arthur comment
     */
    protected void assignProbabilities(Database<V> database, double[] normDistributionFactor, double[] standardDeviation, double[] clusterWeight, List<V> means, Matrix[] eigensystems, Matrix selectionStrong) {
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            V x = database.get(id);
            List<Double> probabilities = new ArrayList<Double>(k);
            for (int i = 0; i < k; i++) {
                double distance = SubspaceEM.distance(x, means.get(i), eigensystems[i].times(selectionStrong));
                probabilities.add(normDistributionFactor[i] * Math.exp(-0.5 * distance * distance / (standardDeviation[i] * standardDeviation[i])));
            }
            database.associate(AssociationID.PROBABILITY_X_GIVEN_CLUSTER_I, id, probabilities);
            double priorProbability = 0.0;
            for (int i = 0; i < k; i++) {
                priorProbability += probabilities.get(i) * clusterWeight[i];
            }
            database.associate(AssociationID.PROBABILITY_X, id, priorProbability);
            List<Double> clusterProbabilities = new ArrayList<Double>(k);
            for (int i = 0; i < k; i++) {
                clusterProbabilities.add(probabilities.get(i) / priorProbability * clusterWeight[i]);
            }
            database.associate(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id, clusterProbabilities);
        }
    }

    //todo arthur comment
    protected static <V extends RealVector<V, ?>> double distance(V p, V mean, Matrix strongEigenvectors) {
        Matrix p_minus_a = p.getColumnVector().minus(mean.getColumnVector());
        Matrix proj = p_minus_a.projection(strongEigenvectors);
        return p_minus_a.minus(proj).euclideanNorm(0);
    }

    /**
     * Creates {@link #k k} random points distributed uniformly within the
     * attribute ranges of the given database.
     *
     * @param database the database must contain enough points in order to
     *                 ascertain the range of attribute values. Less than two points
     *                 would make no sense. The content of the database is not touched
     *                 otherwise.
     * @return a list of {@link #k k} random points distributed uniformly within
     *         the attribute ranges of the given database
     */
    protected List<V> initialMeans(Database<V> database) {
        Random random = new Random();
        if (database.size() > 0) {
            // needs normalization to ensure the randomly generated means
            // are in the same range as the vectors in the database
            // XXX perhaps this can be done more conveniently?
            V randomBase = database.get(database.iterator().next());
            AttributeWiseRealVectorNormalization<V> normalization = new AttributeWiseRealVectorNormalization<V>();
            List<V> list = new ArrayList<V>(database.size());
            for (Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();) {
                list.add(database.get(dbIter.next()));
            }
            try {
                normalization.normalize(list);
            }
            catch (NonNumericFeaturesException e) {
                warning(e.getMessage());
            }
            List<V> means = new ArrayList<V>(k);
            if (isVerbose()) {
                verbose("initializing random vectors");
            }
            for (int i = 0; i < k; i++) {
                V randomVector = randomBase.randomInstance(random);
                try {
                    means.add(normalization.restore(randomVector));
                }
                catch (NonNumericFeaturesException e) {
                    warning(e.getMessage());
                    means.add(randomVector);
                }
            }
            return means;
        }
        else {
            return new ArrayList<V>(0);
        }
    }

    // todo arthur comment
    protected Matrix[] initialEigensystems(int dimensionality) {
        Random random = new Random();
        Matrix[] eigensystems = new Matrix[k];
        for (int i = 0; i < k; i++) {
            double[][] vec = new double[dimensionality][1];
            {
                double sum = 0;
                do {

                    for (int d = 0; d < dimensionality; d++) {
                        vec[d][0] = random.nextDouble() * 2 - 1;
                        sum += vec[d][0];
                    }
                }
                while (sum == 0);
            }
            Matrix eig = new Matrix(vec);
            eig = eig.appendColumns(eig.completeToOrthonormalBasis());
            eigensystems[i] = eig;

        }
        return eigensystems;
    }

    // todo arthur comment
    private void gnuplot(String title, Database<V> db, List<V> means, Matrix[] eigensystems) {
        if (means.size() != eigensystems.length) {
            throw new IllegalArgumentException("number of means: " + means.size() + " -- number of eigensystems: " + eigensystems.length);
        }
        if (isVerbose()) {
            verbose("plotting " + title);
        }
        StringBuilder script = new StringBuilder();

        for (int i = 0; i < means.size(); i++) {
            script.append("set arrow from ");
            script.append(means.get(i).getValue(1));
            script.append(",");
            script.append(means.get(i).getValue(2));
            script.append(" to ");
            script.append(eigensystems[i].get(0, 0) + (Double) means.get(i).getValue(1));
            script.append(",");
            script.append(eigensystems[i].get(0, 1) + (Double) means.get(i).getValue(2));
            script.append("\n");
        }
        script.append("plot \"-\" title \"").append(title).append("\"\n");
        for (Iterator<Integer> iter = db.iterator(); iter.hasNext();) {
            script.append(db.get(iter.next()).toString());
            script.append("\n");
        }
        script.append("end\n");
        script.append("pause -1\n");

        try {
            File scriptFile = File.createTempFile(title, ".gnuscript");
            PrintStream scriptFileStream = new PrintStream(scriptFile);
            scriptFileStream.print(script.toString());
            scriptFileStream.flush();
            scriptFileStream.close();
//
//            Runtime runtime = Runtime.getRuntime();
//            Process proc = runtime.exec("gnuplot "+scriptFile.getAbsolutePath());
//            proc.wait();
//            if(isVerbose())
//            {
//                verbose("Process terminated: "+proc.exitValue());
//            }
//            scriptFile.deleteOnExit();
        }
        catch (IOException e) {
            exception(e.getMessage(), e);
        }
//        catch(InterruptedException e)
//        {
//            exception(e.getMessage(), e);
//        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // k and delta
        k = getParameterValue(K_PARAM);
        delta = getParameterValue(DELTA_PARAM);

        return remainingParameters;
    }

}
