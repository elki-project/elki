package de.lmu.ifi.dbs.elki.algorithm.result;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A multivariate data model.
 *
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class MultivariateModel<V extends RealVector<V, ?>> extends AbstractResult<V> {
    /**
     * The dimensionality of the correlation.
     */
    private int correlationDimensionality;

    /**
     * The dimensionality of the database.
     */
    private int dbdimensionality;

    /**
     * The scaled eigenvectors of the cluster
     */
    private final Matrix projectionMatrix;

    /**
     * The un-projection matrix
     */
    private final Matrix unprojectionMatrix;

    /**
     * Normalization factor
     */
    private final double normalizationFactor;

    /**
     * Standard deviations
     */
    private final double[] stddevs;

    /**
     * The centroid if the objects belonging to the hyperplane induced by the
     * correlation.
     */
    private final Vector centroid;

    /**
     * Provides a new CorrelationAnalysisSolution holding the specified matrix and
     * number format.
     *
     * @param solution           the linear equation system describing the solution
     *                           equations
     * @param db                 the database containing the objects
     * @param strongEigenvectors the strong eigenvectors of the hyperplane induced
     *                           by the correlation
     * @param weakEigenvectors   the weak eigenvectors of the hyperplane induced by
     *                           the correlation
     * @param similarityMatrix   the similarity matrix of the underlying distance
     *                           computations
     * @param centroid           the centroid if the objects belonging to the hyperplane
     *                           induced by the correlation
     * @param nf                 the number format for output accuracy
     */
    public MultivariateModel(Database<V> db, Collection<Integer> ids, Vector centroid, PCAFilteredResult pca) {
        super(db);

        this.correlationDimensionality = pca.getCorrelationDimension();
        // this.similarityMatrix = pca.similarityMatrix();
        // this.eigenvectors = pca.getEigenvectors();
        this.centroid = centroid;
        this.dbdimensionality = centroid.getDimensionality();

        this.projectionMatrix = pca.getEigenvectors();
        this.unprojectionMatrix = projectionMatrix.transpose();

        // determine standard deviations
        // double[] stddevs = new double[dbdimensionality];
        stddevs = pca.getEigenvalues();
        for (int i = 0; i < dbdimensionality; i++)
            stddevs[i] = Math.sqrt(stddevs[i]);

        if (false) {
            // for (int i = 0; i < dbdimensionality; i++) stddevs[i] = 0.0;

            for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
                Integer id = it.next();
                Vector centered = db.get(id).getColumnVector().minus(centroid);
                Matrix projected = unprojectionMatrix.times(centered);
                for (int i = 0; i < dbdimensionality; i++)
                    stddevs[i] += projected.get(i, 0) * projected.get(i, 0);
            }
            for (int i = 0; i < dbdimensionality; i++) {
                // TODO: handle a bias for the variance estimation?
                stddevs[i] = stddevs[i] / (ids.size() - 1);
                stddevs[i] = Math.sqrt(stddevs[i]);
                // Avoid a variance of 0, to avoid div-by-zero.
                // TODO: make this value configurable?
                if (stddevs[i] < Double.MIN_VALUE)
                    stddevs[i] = Double.MIN_VALUE; // 1.0?
                // System.out.println(stddevs[i] + "<->" +
                // Math.sqrt(pca.getEigenvalues()[i]));
                // System.out.println(stddevs[i] / Math.sqrt(pca.getEigenvalues()[i]));
            }
        }
        for (int i = 0; i < dbdimensionality; i++)
            for (int j = 0; j < dbdimensionality; j++) {
                // Note: cols on forward, rows on backward transformation!
                projectionMatrix.set(j, i, projectionMatrix.get(j, i) * stddevs[i]);
                unprojectionMatrix.set(j, i, unprojectionMatrix.get(j, i) / stddevs[j]);
            }
        double normalize = 1.0;
        for (int i = 0; i < dbdimensionality; i++)
            normalize *= stddevs[i];
        // avoid potential division by 0 in corner case (all points the same)
        if (normalize < Double.MIN_VALUE)
            normalize = 1.0;
        normalizationFactor = normalize;
    }

    @Override
    public void output(File out, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        PrintStream outStream;
        try {
            outStream = new PrintStream(new FileOutputStream(out));
        }
        catch (Exception e) {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        output(outStream, normalization, settings);
    }

    /**
     * Writes the clustering result to the given stream.
     *
     * @param outStream     the stream to write to
     * @param normalization Normalization to restore original values according to,
     *                      if this action is supported - may remain null.
     * @param settings      the settings to be written into the header
     * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException
     *          if any
     *          feature vector is not compatible with values initialized during
     *          normalization
     */
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        writeHeader(outStream, settings, null);

        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println("### mean = " + centroid);
        outStream.println("################################################################################");
        outStream.flush();
    }

    /**
     * Return the full dimensionality.
     *
     * @return the full dimensionality
     */
    public int getFullDimensionality() {
        return dbdimensionality;
    }

    /**
     * Return the correlation dimensionality.
     *
     * @return the correlation dimensionality
     */
    public int getCorrelationDimensionality() {
        return correlationDimensionality;
    }

    /**
     * Returns the distance of RealVector p from the hyperplane underlying this
     * solution.
     *
     * @param p a vector in the space underlying this solution
     * @return the distance of p from the hyperplane underlying this solution
     */
    public double distance(V p) {
        return distance(p.getColumnVector());
    }

    /**
     * Returns the distance of Matrix p from the model solution.
     *
     * @param p a vector in the space underlying this solution
     * @return the distance of p from the model
     */
    private double distance(Matrix p) {
        return unprojectionMatrix.times(p.minus(centroid)).euclideanNorm(0);
    }

    /**
     * Returns the probability of RealVector p to belong to the given model.
     *
     * @param p a vector in the space underlying this solution
     * @return the probability of p belonging to this model.
     */
    public double probability(V p) {
        return probability(p.getColumnVector(), false);
    }

    /**
     * Returns the probability of RealVector p to belong to the given model.
     *
     * @param p a vector in the space underlying this solution
     * @return the probability of p belonging to this model.
     */
    public double boostedProbability(V p) {
        return probability(p.getColumnVector(), true);
    }

    /**
     * Returns the probability of a Matrix p to belong to this model.
     *
     * @param p a vector in the space underlying this solution
     * @return the probability belonging to this model
     */
    private double probability(Matrix p, boolean boost) {
        Matrix proj = unprojectionMatrix.times(p.minus(centroid));
        double probability = 1.0;
        int start = 0;
        if (boost && (correlationDimensionality < dbdimensionality))
            start = correlationDimensionality;
        for (int i = start; i < dbdimensionality; i++)
            probability = probability * ErrorFunctions.erf(Math.abs(proj.get(i, 0)) / Math.sqrt(2));
        // probability = probability * ErrorFunctions.erfc(proj.get(i,0) /
        // (standardDeviations[i] * Math.sqrt(2)));
        return 1.0 - probability;
    }

    /**
     * Returns the centroid of this model.
     *
     * @return the centroid of this model
     */
    public Vector getCentroid() {
        return centroid;
    }

    /**
     * Returns a copy of the projection matrix
     *
     * @return the projection matrix
     */
    public Matrix getProjectionMatrix() {
        return projectionMatrix.copy();
    }

    /**
     * Returns a copy of the unprojection matrix
     *
     * @return the unprojection matrix
     */
    public Matrix getUnprojectionMatrix() {
        return unprojectionMatrix.copy();
    }

    /**
     * Returns a normalized copy of the projection matrix
     *
     * @return the projection matrix
     */
    public Matrix getNormalizedProjectionMatrix() {
        return projectionMatrix.times(normalizationFactor);
    }

    /**
     * Returns a normalized copy of the unprojection matrix
     *
     * @return the unprojection matrix
     */
    public Matrix getNormalizedUnprojectionMatrix() {
        return unprojectionMatrix.times(1.0 / normalizationFactor);
    }

    /**
     * Read the normalization factor
     *
     * @return normalization factor
     */
    public double getNormalizationFactor() {
        return normalizationFactor;
    }

    /**
     * Returns a normalized copy of the eigenvalues-roots
     *
     * @return the unprojection matrix
     */
    public double[] getNormalizedRoots() {
        double[] normdevs = new double[dbdimensionality];
        for (int i = 0; i < dbdimensionality; i++)
            normdevs[i] = stddevs[i] / normalizationFactor;
        return normdevs;
    }

    /**
     * Returns a copy of the eigenvalues-roots
     *
     * @return the unprojection matrix
     */
    public double[] getRoots() {
        double[] normdevs = new double[dbdimensionality];
        for (int i = 0; i < dbdimensionality; i++)
            normdevs[i] = stddevs[i];
        return normdevs;
    }
}