package de.lmu.ifi.dbs.elki.data.synthetic;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.VectorListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalVectorListElementSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

/**
 * Provides automatic generation of arbitrary oriented hyperplanes of arbitrary
 * correlation dimensionalities. <p/>
 *
 * @author Elke Achtert
 */
public class ArbitraryCorrelationGenerator extends AxesParallelCorrelationGenerator {
    /**
     * OptionID for {@link #POINT_PARAM}
     */
    public static final OptionID POINT_ID = OptionID.getOrCreateOptionID(
        "acg.point", "a comma separated list of " +
        "the coordinates of the model point, " +
        "default is the centroid of the defined feature space.");

    /**
     * OptionID for {@link #BASIS_PARAM}
     */
    public static final OptionID BASIS_ID = OptionID.getOrCreateOptionID(
        "acg.basis", "a list of basis vectors of the correlation hyperplane, "
        + "where c denotes the correlation dimensionality and d the dimensionality of the "
        + "feature space. Each basis vector is separated by :, the coordinates within "
        + "the basis vectors are separated by a comma. If no basis is specified, the basis vectors are generated randomly.");

    /**
     * OptionID for {@link #GAUSSIAN_FLAG}
     */
    public static final OptionID GAUSSIAN_ID = OptionID.getOrCreateOptionID(
        "acg.gaussian", "flag to indicate gaussian distribution, default is an equal distribution.");

    /**
     * Parameter point.
     */
    private final DoubleListParameter POINT_PARAM = new DoubleListParameter(POINT_ID, null, true, null);

    /**
     * Parameter basis vectors
     */
    private final VectorListParameter BASIS_PARAM = new VectorListParameter(BASIS_ID, null, true, null);
    
    /**
     * Flag for gaussian distribution.
     */
    private final Flag GAUSSIAN_FLAG = new Flag(GAUSSIAN_ID);
    
    /**
     * The model point.
     */
    private Vector point;

    /**
     * The basis vectors.
     */
    private Matrix basis;

    /**
     * The standard deviation for jitter.
     */
    private double jitter_std;

    /**
     * Indicates, if a gaussian distribution is desired.
     */
    private boolean gaussianDistribution;

    /**
     * Creates a new arbitrary correlation generator that provides automatic
     * generation of arbitrary oriented hyperplanes of arbitrary correlation
     * dimensionalities.
     */
    public ArbitraryCorrelationGenerator() {
        super();
        // parameter point
        addOption(POINT_PARAM);
        
        GlobalParameterConstraint gpc = new GlobalListSizeConstraint(POINT_PARAM, DIM_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);

        // parameter basis vectors
        addOption(BASIS_PARAM);
        
        GlobalParameterConstraint gpc2 = new GlobalListSizeConstraint(BASIS_PARAM, CORRDIM_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc2);
        GlobalParameterConstraint gpc3 = new GlobalVectorListElementSizeConstraint(BASIS_PARAM, DIM_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc3);

        addOption(GAUSSIAN_FLAG);
    }

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        LoggingConfiguration.assertConfigured();

        ArbitraryCorrelationGenerator wrapper = new ArbitraryCorrelationGenerator();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggingUtil.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
        }
        catch (Exception e) {
          LoggingUtil.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    // todo comment
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // model point
        if (POINT_PARAM.isSet()) {
            List<Double> pointList = POINT_PARAM.getValue();
            double[] p = new double[dataDim];
            for (int i = 0; i < dataDim; i++) {

                p[i] = pointList.get(i);
            }
            point = new Vector(p);
        }
        else {
            point = centroid(dataDim);
        }

        // basis
        if (BASIS_PARAM.isSet()) {
            List<List<Double>> basis_lists = BASIS_PARAM.getValue();

            double[][] b = new double[dataDim][corrDim];
            for (int c = 0; c < corrDim; c++) {
                List<Double> basisCoordinates = basis_lists.get(c);
                for (int d = 0; d < dataDim; d++) {

                    b[d][c] = basisCoordinates.get(d);
                }
            }
            basis = new Matrix(b);

        }
        else {
            basis = correlationBasis(dataDim, corrDim);
        }

        // jitter std
        jitter_std = 0;
        for (int d = 0; d < dataDim; d++) {
            jitter_std = Math.max(jitter * (max[d] - min[d]), jitter_std);
        }

        // gaussian distribution
        gaussianDistribution = GAUSSIAN_FLAG.isSet();

        return remainingParameters;
    }

    /**
     * Generates a correlation hyperplane and writes it to the specified
     * according output stream writer.
     *
     * @param outStream the output stream to write into
     */
    @Override
    void generateCorrelation(OutputStreamWriter outStream) throws IOException {
        if (logger.isLoggable(LogLevel.FINE)) {
            StringBuffer msg = new StringBuffer();
            msg.append("basis");
            msg.append(basis.toString(Format.NF4));
            msg.append("\npoint");
            msg.append(point.toString(Format.NF4));
            logger.log(LogLevel.FINE, msg.toString());
        }

        if (point.getRowDimensionality() != basis.getRowDimensionality())
            throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

        if (point.getColumnDimensionality() != 1)
            throw new IllegalArgumentException("point.getColumnDimension() != 1!");

        if (!inMinMax(point))
            throw new IllegalArgumentException("point not in min max!");

        if (basis.getColumnDimensionality() == basis.getRowDimensionality()) {
            generateNoise(outStream);
            return;
        }

        // determine the dependency
        Dependency dependency = determineDependency();
        if (isVerbose()) {
            StringBuffer msg = new StringBuffer();
            msg.append(dependency.toString());
            verbose(msg.toString());
        }

        Matrix b = dependency.basisVectors;

        List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(number);
        while (featureVectors.size() != number) {
            Vector featureVector = generateFeatureVector(point, b);

            if (jitter != 0) {
                featureVector = jitter(featureVector, dependency.normalVectors);
            }
            if (inMinMax(featureVector)) {
                featureVectors.add(new DoubleVector(featureVector));
            }
        }

        double std = standardDeviation(featureVectors, point, b);
        if (isVerbose()) {
            verbose("standard deviation " + std);
        }

        output(outStream, featureVectors, dependency.dependency, std);
    }

    /**
     * Determines the linear equation system describing the dependencies of the
     * correlation hyperplane depicted by the model point and the basis.
     *
     * @return the dependencies
     */
    private Dependency determineDependency() {
        // orthonormal basis of subvectorspace U
        Matrix orthonormalBasis_U = basis.orthonormalize();
        Matrix completeVectors = orthonormalBasis_U.completeBasis();
        if (logger.isLoggable(LogLevel.FINE)) {
            StringBuffer msg = new StringBuffer();

            msg.append("point ").append(point.toString(Format.NF4));
            msg.append("\nbasis ").append(basis.toString(Format.NF4));
            msg.append("\northonormal basis ").append(orthonormalBasis_U.toString(Format.NF4));
            msg.append("\ncomplete vectors ").append(completeVectors.toString(Format.NF4));
            logger.log(LogLevel.FINE, msg.toString());
        }

        // orthonormal basis of vectorspace V
        Matrix basis_V = orthonormalBasis_U.appendColumns(completeVectors);
        basis_V = basis_V.orthonormalize();
        if (logger.isLoggable(LogLevel.FINE)) {
          logger.log(LogLevel.FINE, "basis V " + basis_V.toString(Format.NF4));
        }

        // normal vectors of U
        Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimensionality() - 1, basis.getColumnDimensionality(), basis
            .getRowDimensionality()
            - basis.getColumnDimensionality() + basis.getColumnDimensionality() - 1);
        if (logger.isLoggable(LogLevel.FINE)) {
          logger.log(LogLevel.FINE, "normal vector U " + normalVectors_U.toString(Format.NF4));
        }
        Matrix transposedNormalVectors = normalVectors_U.transpose();
        if (logger.isLoggable(LogLevel.FINE)) {
          logger.log(LogLevel.FINE, "tNV " + transposedNormalVectors.toString(Format.NF4));
          logger.log(LogLevel.FINE, "point " + point.toString(Format.NF4));
        }

        // gauss jordan
        Matrix B = transposedNormalVectors.times(point);
        if (logger.isLoggable(LogLevel.FINE)) {
          logger.log(LogLevel.FINE, "B " + B.toString(Format.NF4));
        }
        Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimensionality(), transposedNormalVectors.getColumnDimensionality()
            + B.getColumnDimensionality());
        gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimensionality() - 1, 0,
            transposedNormalVectors.getColumnDimensionality() - 1, transposedNormalVectors);
        gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1, transposedNormalVectors.getColumnDimensionality(), gaussJordan
            .getColumnDimensionality() - 1, B);

        double[][] a = new double[transposedNormalVectors.getRowDimensionality()][transposedNormalVectors.getColumnDimensionality()];
        double[][] we = transposedNormalVectors.getArray();
        double[] b = B.getColumn(0).getRowPackedCopy();
        System.arraycopy(we, 0, a, 0, transposedNormalVectors.getRowDimensionality());

        LinearEquationSystem lq = new LinearEquationSystem(a, b);
        lq.solveByTotalPivotSearch();

        return new Dependency(orthonormalBasis_U, normalVectors_U, lq);
    }

    /**
     * Generates a vector lying on the hyperplane defined by the specified
     * parameters.
     *
     * @param point the model point of the hyperplane
     * @param basis the basis of the hyperplane
     * @return a matrix consisting of the po
     */
    private Vector generateFeatureVector(Vector point, Matrix basis) {
        Vector featureVector = point.copy();

        for (int i = 0; i < basis.getColumnDimensionality(); i++) {
            Vector b_i = basis.getColumnVector(i);

            double lambda_i;
            if (gaussianDistribution) {
                lambda_i = RANDOM.nextGaussian();
            }
            else {
                lambda_i = RANDOM.nextDouble();
                if (RANDOM.nextBoolean()) {
                    lambda_i *= -1;
                }
            }

            featureVector = featureVector.plus(b_i.times(lambda_i));
        }
        return featureVector;
    }

    /**
     * Adds a jitter to each dimension of the specified feature vector.
     *
     * @param featureVector the feature vector
     * @param normalVectors the normal vectors
     * @return the new (jittered) feature vector
     */
    private Vector jitter(Vector featureVector, Matrix normalVectors) {
        if (gaussianDistribution) {
            for (int i = 0; i < normalVectors.getColumnDimensionality(); i++) {
                Vector n_i = normalVectors.getColumnVector(i);
                n_i.normalizeColumns();
                double distance = RANDOM.nextGaussian() * jitter_std;
                featureVector = n_i.times(distance).plus(featureVector);
            }
        }
        else {
            double maxDist = Math.sqrt(dataDim);
            for (int i = 0; i < normalVectors.getColumnDimensionality(); i++) {
                Vector n_i = normalVectors.getColumnVector(i);
                n_i.normalizeColumns();
                double distance = RANDOM.nextDouble() * jitter * maxDist;
                featureVector = n_i.times(distance).plus(featureVector);

                // double v = featureVector.get(d);
                // if (RANDOM.nextBoolean())
                // featureVector.set(d, v + v * RANDOM.nextDouble() * jitter);
                // else
                // featureVector.set(d, v - v * RANDOM.nextDouble() * jitter);
            }
        }
        return featureVector;
    }

    /**
     * Returns true, if the specified feature vector is in the interval [min,
     * max], false otherwise.
     *
     * @param featureVector the feature vector to be tested
     * @return true, if the specified feature vector is in the interval [min,
     *         max], false otherwise
     */
    private boolean inMinMax(Vector featureVector) {
        for (int i = 0; i < featureVector.getRowDimensionality(); i++) {
            for (int j = 0; j < featureVector.getColumnDimensionality(); j++) {
                double value = featureVector.get(i, j);
                if (value < min[i]) {
                    return false;
                }
                if (value > max[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Writes the specified list of feature vectors to the output.
     *
     * @param outStream      the output stream to write into
     * @param featureVectors the feature vectors to be written
     * @param dependency     the dependency of the feature vectors
     * @param std            the standard deviation of the jitter of the feature vectors
     * @throws IOException if an I/O exception occurs during writing
     */
    private void output(OutputStreamWriter outStream, List<DoubleVector> featureVectors, LinearEquationSystem dependency, double std)
        throws IOException {
        outStream.write("########################################################" + LINE_SEPARATOR);
        outStream.write("### " + MIN_ID.getName() + " [" + FormatUtil.format(min, ",", Format.NF4) + "]" + LINE_SEPARATOR);
        outStream.write("### " + MAX_ID.getName() + " [" + FormatUtil.format(max, ",", Format.NF4) + "]" + LINE_SEPARATOR);
        outStream.write("### " + NUMBER_ID.getName() + " " + number + LINE_SEPARATOR);
        outStream.write("### " + POINT_ID.getName() + " [" + FormatUtil.format(point.getColumnPackedCopy(), Format.NF4) + "]" + LINE_SEPARATOR);
        outStream.write("### " + BASIS_ID.getName() + " ");
        for (int i = 0; i < basis.getColumnDimensionality(); i++) {
            outStream.write("[" + FormatUtil.format(basis.getColumn(i).getColumnPackedCopy(), Format.NF4) + "]");
            if (i < basis.getColumnDimensionality() - 1) {
                outStream.write(",");
            }
        }
        outStream.write(LINE_SEPARATOR);

        if (jitter != 0) {
            outStream.write("### max jitter in each dimension " + FormatUtil.format(jitter, Format.NF4) + "%" + LINE_SEPARATOR);
            outStream.write("### Randomized standard deviation " + FormatUtil.format(jitter_std, Format.NF4) + LINE_SEPARATOR);
            outStream.write("### Real       standard deviation " + FormatUtil.format(std, Format.NF4) + LINE_SEPARATOR);
            outStream.write("###" + LINE_SEPARATOR);
        }

        if (dependency != null) {
            outStream.write("### " + LINE_SEPARATOR);
            outStream.write("### dependency ");
            outStream.write(dependency.equationsToString("### ", Format.NF4.getMaximumFractionDigits()));
        }
        outStream.write("########################################################" + LINE_SEPARATOR);

        for (DoubleVector featureVector : featureVectors) {
            if (label == null) {
                outStream.write(featureVector + LINE_SEPARATOR);
            }
            else {
                outStream.write(featureVector.toString());
                outStream.write(" " + label + LINE_SEPARATOR);
            }
        }
    }

    /**
     * Returns the standard deviation of the distance of the feature vectors to
     * the hyperplane defined by the specified point and basis.
     *
     * @param featureVectors the feature vectors
     * @param point          the model point of the hyperplane
     * @param basis          the basis of the hyperplane
     * @return the standard deviation of the distance
     */
    private double standardDeviation(List<DoubleVector> featureVectors, Vector point, Matrix basis) {
        double std_2 = 0;
        for (DoubleVector doubleVector : featureVectors) {
            double distance = distance(doubleVector.getColumnVector(), point, basis);
            std_2 += distance * distance;
        }
        return Math.sqrt(std_2 / featureVectors.size());
    }

    /**
     * Returns the distance of the specified feature vector to the hyperplane
     * defined by the specified point and basis.
     *
     * @param featureVector the feature vector
     * @param point         the model point of the hyperplane
     * @param basis         the basis of the hyperplane
     * @return the distance of the specified feature vector to the hyperplane
     */
    private double distance(Vector featureVector, Vector point, Matrix basis) {
        Matrix p_minus_a = featureVector.minus(point);
        Matrix proj = p_minus_a.projection(basis);
        return p_minus_a.minus(proj).euclideanNorm(0);
    }

    /**
     * Generates noise and writes it to the specified outStream.
     *
     * @param outStream the output stream to write to
     * @throws java.io.IOException if an I/O exception occurs during writing
     */
    private void generateNoise(OutputStreamWriter outStream) throws IOException {
        List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(number);
        int dim = min.length;
        for (int i = 0; i < number; i++) {
            double[] values = new double[dim];
            for (int d = 0; d < dim; d++) {
                values[d] = RANDOM.nextDouble() * (max[d] - min[d]) + min[d];
            }
            featureVectors.add(new DoubleVector(values));
        }
        output(outStream, featureVectors, null, 0);
    }

    /**
     * Returns the centroid of the feature space.
     *
     * @param dim the dimensionality of the feature space.
     * @return the centroid of the feature space
     */
    private Vector centroid(int dim) {
        double[] p = new double[dim];
        for (int i = 0; i < p.length; i++) {
            p[i] = (max[i] - min[i]) / 2;
        }
        return new Vector(p);
    }

    /**
     * Returns a basis for for a hyperplane of the specified correlation
     * dimension
     *
     * @param dim     the dimensionality of the feature space
     * @param corrDim the correlation dimensionality
     * @return a basis for a hyperplane of the specified correlation dimension
     */
    private Matrix correlationBasis(int dim, int corrDim) {
        double[][] b = new double[dim][corrDim];
        for (int i = 0; i < b.length; i++) {
            if (i < corrDim) {
                b[i][i] = 1;
            }
            else {
                for (int j = 0; j < corrDim; j++) {
                    b[i][j] = RANDOM.nextDouble() * (max[i] - min[i]) + min[i];
                }
            }
        }
        return new Matrix(b);
    }

    /**
     * Encapsulates dependencies.
     */
    private static class Dependency {
        /**
         * The basis vectors.
         */
        Matrix basisVectors;

        /**
         * The normal vectors.
         */
        Matrix normalVectors;

        /**
         * The linear equation system.
         */
        LinearEquationSystem dependency;

        /**
         * Provided a new dependency object.
         *
         * @param basisVectors         the basis vectors
         * @param normalvectors        the normal vectors
         * @param linearEquationSystem the linear equation system
         */
        public Dependency(Matrix basisVectors, Matrix normalvectors, LinearEquationSystem linearEquationSystem) {
            this.basisVectors = basisVectors;
            this.normalVectors = normalvectors;
            this.dependency = linearEquationSystem;
        }

        /**
         * Returns a string representation of the object.
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return
                // "basisVectors : " + basisVectors.toString(NF) +
                // "normalVectors: " + normalVectors.toString(NF) +
                "dependency: " + dependency.equationsToString(Format.NF4.getMaximumFractionDigits());
        }
    }
}