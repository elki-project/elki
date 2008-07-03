package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessGlobalConstraint;

import java.util.Collection;

/**
 * LocalPCA is a super calss for PCA-algorithms considering only a local
 * neighborhood. LocalPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert
 */
public abstract class LocalPCA<V extends RealVector<V, ?>> extends AbstractPCA {

    /**
     * OptionID for {@link #BIG_PARAM}
     */
    public static final OptionID LOCAL_PCA_BIG = OptionID.getOrCreateOptionID(
        "localpca.big",
        "A constant big value to reset high eigenvalues."
    );

    /**
     * OptionID for {@link #SMALL_PARAM}
     */
    public static final OptionID LOCAL_PCA_SMALL = OptionID.getOrCreateOptionID(
        "localpca.small",
        "A constant small value to reset low eigenvalues."
    );

    /**
     * Parameter to specify a constant big value to reset high eigenvalues,
     * must be a double greater than 0.
     * <p>Default value: {@code 1.0} </p>
     * <p>Key: {@code -localpca.big} </p>
     */
    private final DoubleParameter BIG_PARAM = new DoubleParameter(
        LOCAL_PCA_BIG,
        new GreaterConstraint(0),
        1.0);

    /**
     * Parameter to specify a constant small value to reset low eigenvalues,
     * must be a double greater than 0.
     * <p>Default value: {@code 0.0} </p>
     * <p>Key: {@code -localpca.small} </p>
     */
    private final DoubleParameter SMALL_PARAM = new DoubleParameter(
        LOCAL_PCA_SMALL,
        new GreaterConstraint(0),
        0.0);

    /**
     * Holds the big value.
     */
    private double big;

    /**
     * Holds the small value.
     */
    private double small;

    /**
     * The correlation dimension (i.e. the number of strong eigenvectors) of the
     * object to which this PCA belongs to.
     */
    private int correlationDimension = 0;

    /**
     * The selection matrix of the weak eigenvectors.
     */
    private Matrix e_hat;

    /**
     * The selection matrix of the strong eigenvectors.
     */
    private Matrix e_czech;

    /**
     * The similarity matrix.
     */
    private Matrix m_hat;

    /**
     * The dissimilarity matrix.
     */
    private Matrix m_czech;

    /**
     * The diagonal matrix of adapted strong eigenvalues: eigenvectors *
     * e_czech.
     */
    private Matrix adapatedStrongEigenvectors;

    /**
     * Adds parameter for big and small value to parameter map.
     */
    public LocalPCA() {
        super();

        // parameter big value
        addOption(BIG_PARAM);

        // parameter small value
        addOption(SMALL_PARAM);

        // global constraintsmall <--> big
        optionHandler.setGlobalParameterConstraint(new LessGlobalConstraint<Double>(SMALL_PARAM, BIG_PARAM));
    }

    /**
     * Performs a LocalPCA for the object with the specified ids stored in the
     * given database.
     *
     * @param ids      the ids of the objects for which the PCA should be performed
     * @param database the database containing the objects
     */
    public final void run(Collection<Integer> ids, Database<V> database) {
        // logging
        StringBuffer msg = new StringBuffer();
        if (this.debug) {
            V o = database.get(ids.iterator().next());
            String label = database.getAssociation(AssociationID.LABEL, o.getID());
            msg.append("\nobject ").append(o).append(" ").append(label);
        }

        // sorted eigenpairs, eigenvectors, eigenvalues
        Matrix pcaMatrix = pcaMatrix(database, ids);
        determineEigenPairs(pcaMatrix);

        // correlationDimension = #strong EV
        correlationDimension = getStrongEigenvalues().length;
        int dim = getEigenvalues().length;

        // selection Matrix for weak and strong EVs
        e_hat = new Matrix(dim, dim);
        e_czech = new Matrix(dim, dim);
        for (int d = 0; d < dim; d++) {
            if (d < correlationDimension) {
                e_czech.set(d, d, big);
                e_hat.set(d, d, small);
            }
            else {
                e_czech.set(d, d, small);
                e_hat.set(d, d, big);
            }
        }

        Matrix V = getEigenvectors();
        adapatedStrongEigenvectors = V.times(e_czech).times(Matrix.identity(dim, correlationDimension));

        m_hat = V.times(e_hat).times(V.transpose());
        m_czech = V.times(e_czech).times(V.transpose());

        if (this.debug) {
            msg.append("\n ids =");
            for (Integer id : ids) {
                msg.append(database.getAssociation(AssociationID.LABEL, id)).append(", ");
            }

            msg.append("\n  E = ");
            msg.append(Util.format(getEigenvalues(), ",", 6));

            msg.append("\n  V = ");
            msg.append(V);

            msg.append("\n  E_hat = ");
            msg.append(e_hat);

            msg.append("\n  E_czech = ");
            msg.append(e_czech);

            msg.append("\n  corrDim = ");
            msg.append(correlationDimension);

            debugFine(msg.toString() + "\n");
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // big value
        big = getParameterValue(BIG_PARAM);

        // small value
        small = getParameterValue(SMALL_PARAM);

        return remainingParameters;
    }

    /**
     * Returns the correlation dimension (i.e. the number of strong
     * eigenvectors) of the object to which this PCA belongs to.
     *
     * @return the correlation dimension
     */
    public int getCorrelationDimension() {
        return correlationDimension;
    }

    /**
     * Returns a copy of the selection matrix of the weak eigenvectors (E_hat)
     * of the object to which this PCA belongs to.
     *
     * @return the selection matrix of the weak eigenvectors E_hat
     */
    public Matrix selectionMatrixOfWeakEigenvectors() {
        return e_hat.copy();
    }

    /**
     * Returns a copy of the selection matrix of the strong eigenvectors
     * (E_czech) of this LocalPCA.
     *
     * @return the selection matrix of the weak eigenvectors E_czech
     */
    public Matrix selectionMatrixOfStrongEigenvectors() {
        return e_czech.copy();
    }

    /**
     * Returns a copy of the similarity matrix (M_hat) of this LocalPCA.
     *
     * @return the similarity matrix M_hat
     */
    public Matrix similarityMatrix() {
        return m_hat.copy();
    }

    /**
     * Returns a copy of the dissimilarity matrix (M_czech) of this LocalPCA.
     *
     * @return the dissimilarity matrix M_hat
     */
    public Matrix dissimilarityMatrix() {
        return m_czech.copy();
    }

    /**
     * Returns a copy of the adapted strong eigenvectors.
     *
     * @return the adapted strong eigenvectors
     */
    public Matrix adapatedStrongEigenvectors() {
        return adapatedStrongEigenvectors.copy();
    }

    /**
     * Determines and returns the matrix that is used for performaing the pca.
     *
     * @param database the database holding the objects
     * @param ids      the list of the object ids for which the matrix should be
     *                 determined
     * @return he matrix that is used for performaing a pca
     */
    protected abstract Matrix pcaMatrix(Database<V> database, Collection<Integer> ids);
}
