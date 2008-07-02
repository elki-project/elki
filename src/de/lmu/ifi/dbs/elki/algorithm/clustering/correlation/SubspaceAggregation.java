package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 * todo parameter
 */
public class SubspaceAggregation<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V> {
    /**
     * Small value to increment diagonally of a matrix
     * in order to avoid singularity befor building the inverse.
     */
    private static final double SINGULARITY_CHEAT = 1E-9;

    /**
     * Parameter k.
     */
    public static final String K_P = "k";

    /**
     * Description for parameter k.
     */
    public static final String K_D = "k - the number of clusters to find (positive integer)";

    /**
     * Parameter for k.
     * Constraint greater 0.
     */
    private final IntParameter K_PARAM = new IntParameter(K_P, K_D, new GreaterConstraint(0));

    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;


    /**
     * Stores the result.
     */
    private Clusters<V> result;

    /**
     *
     */
    public SubspaceAggregation() {
        super();
        debug = true;
        addOption(K_PARAM);
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
        return new Description("SubspaceAggregation", "SubspaceAggregation", "", "");
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
     */
    @Override
    public void runInTime(Database<V> database) throws IllegalStateException {
        if (database.size() == 0) {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
        List<V> newMeans = initialMeans(database);
        int dimensionality = newMeans.get(0).getDimensionality();
        Matrix[] newEigensystems = initialEigensystems(dimensionality);
        //Matrix selectionWeak = Matrix.zeroMatrix(dimensionality);
        //selectionWeak.set(dimensionality-1, dimensionality-1, 1);
        Matrix selectionStrong = Matrix.unitMatrix(dimensionality);
        selectionStrong.set(dimensionality - 1, dimensionality - 1, 0);

        List<V> means = new ArrayList<V>(k);
        Matrix[] eigensystems = new Matrix[k];
        List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
        for (int i = 0; i < k; i++) {
            means.add(null);
            clusters.add(new ArrayList<Integer>());
        }
        int it = 0;

        do {
            it++;
            for (int i = 0; i < k; i++) {
                means.set(i, newMeans.get(i));
                eigensystems[i] = newEigensystems[i];
                clusters.get(i).clear();
            }

            gnuplot("Iteration_" + it + "_", database, means, eigensystems);
            if (isVerbose()) {
                verbose("iteration " + it);
            }
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
                Integer id = iter.next();
                V currentObject = database.get(id);
                int index = 0;
                double minimalDistance = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double distance = projectionDistance(currentObject, means.get(i), eigensystems[i].times(selectionStrong));
                    if (distance < minimalDistance) {
                        minimalDistance = distance;
                        index = i;
                    }
                }
                clusters.get(index).add(id);
            }
            for (int i = 0; i < k; i++) {
                List<Integer> cluster = clusters.get(i);
                if (cluster.size() == 0) {
                    newMeans.remove(i);
                    clusters.remove(i);
                    Matrix[] shrinkedEigensystems = new Matrix[k - 1];
                    System.arraycopy(newEigensystems, 0, shrinkedEigensystems, 0, i);
                    System.arraycopy(eigensystems, i + 1, shrinkedEigensystems, i, eigensystems.length - i);
                    newEigensystems = shrinkedEigensystems;
                    k--;
                    i--;
                    if (isVerbose()) {
                        verbose("reduced number of clusters, new: " + k);
                    }
                }
                else {
                    newMeans.set(i, Util.centroid(database, cluster));
                    Matrix covarianceMatrix = Matrix.zeroMatrix(dimensionality);
                    for (Integer id : cluster) {
                        V instance = database.get(id);
                        V difference = instance.plus(newMeans.get(i).negativeVector());
                        covarianceMatrix = covarianceMatrix.plus(difference.getColumnVector().times(difference.getRowVector()));
                    }
                    covarianceMatrix = covarianceMatrix.times(1.0 / cluster.size());
                    newEigensystems[i] = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).eig().getV();
                }
            }
        }
        while (!means.equals(newMeans) || !Arrays.equals(eigensystems, newEigensystems));

        Integer[][] resultClusters = new Integer[clusters.size()][];
        for (int i = 0; i < clusters.size(); i++) {
            List<Integer> cluster = clusters.get(i);
            resultClusters[i] = cluster.toArray(new Integer[cluster.size()]);
        }
        result = new Clusters<V>(resultClusters, database);
    }


    protected static <V extends RealVector<V, ?>> double projectionDistance(V p, V mean, Matrix strongEigenvectors) {
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

        k = getParameterValue(K_PARAM);

        return remainingParameters;
    }

}
