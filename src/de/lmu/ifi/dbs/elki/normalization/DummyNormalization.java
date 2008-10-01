package de.lmu.ifi.dbs.elki.normalization;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

import java.util.List;

/**
 * Dummy normalization that does nothing. This class is used at normalization of multi-represented objects
 * if one representation needs no normalization.
 *
 * @author Elke Achtert
 */
public class DummyNormalization<O extends DatabaseObject> extends AbstractParameterizable implements Normalization<O> {

    /**
     * @return the specified objectAndAssociationsList
     */
    public List<ObjectAndAssociations<O>> normalizeObjects(List<ObjectAndAssociations<O>> objectAndAssociationsList) throws NonNumericFeaturesException {
        return objectAndAssociationsList;
    }

    /**
     * @return the specified featureVectors
     */
    public List<O> normalize(List<O> featureVectors) throws NonNumericFeaturesException {
        return featureVectors;
    }

    /**
     * @return the specified featureVectors
     */
    public List<O> restore(List<O> featureVectors) throws NonNumericFeaturesException {
        return featureVectors;
    }

    /**
     * @return the specified featureVector
     */
    public O restore(O featureVector) throws NonNumericFeaturesException {
        return featureVector;
    }

    /**
     * @return the specified linear equation system
     */
    public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) throws NonNumericFeaturesException {
        return linearEquationSystem;
    }

    public String toString(String pre) {
        return pre + toString();
    }

    @Override
    public String parameterDescription() {
        return "Dummy normalization that does nothing. This class is used at normalization of multi-represented " +
            "objects if one representation needs no normalization.";
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
