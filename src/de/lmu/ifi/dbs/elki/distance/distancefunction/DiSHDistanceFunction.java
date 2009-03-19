package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Distance function used in the DiSH algorithm.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class DiSHDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>>
    extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<V, P> {

    /**
     * Computes the correlation distance between the two specified vectors
     * according to the specified preference vectors.
     *
     * @param v1  first RealVector
     * @param v2  second RealVector
     * @param pv1 the first preference vector
     * @param pv2 the second preference vector
     * @return the correlation distance between the two specified vectors
     */
    @Override
    public PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2) {
        BitSet commonPreferenceVector = (BitSet) pv1.clone();
        commonPreferenceVector.and(pv2);
        int dim = v1.getDimensionality();

        // number of zero values in commonPreferenceVector
        Integer subspaceDim = dim - commonPreferenceVector.cardinality();

        // special case: v1 and v2 are in parallel subspaces
        if (commonPreferenceVector.equals(pv1) || commonPreferenceVector.equals(pv2)) {
            double d = weightedDistance(v1, v2, commonPreferenceVector);
            if (d > 2 * getEpsilon()) {
                subspaceDim++;
                if (this.debug) {
                    StringBuffer msg = new StringBuffer();
                    msg.append("\n");
                    msg.append("\nd ").append(d);
                    msg.append("\nv1 ").append(getDatabase().getAssociation(AssociationID.LABEL, v1.getID()));
                    msg.append("\nv2 ").append(getDatabase().getAssociation(AssociationID.LABEL, v2.getID()));
                    msg.append("\nsubspaceDim ").append(subspaceDim);
                    msg.append("\ncommon pv ").append(FormatUtil.format(dim, commonPreferenceVector));
                    debugFine(msg.toString());
                }
            }
        }

        // flip commonPreferenceVector for distance computation in common subspace
        BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
        inverseCommonPreferenceVector.flip(0, dim);

        return new PreferenceVectorBasedCorrelationDistance(
            getDatabase().dimensionality(),
            subspaceDim,
            weightedDistance(v1, v2, inverseCommonPreferenceVector),
            commonPreferenceVector);
    }

    /**
     * @return the name of the default preprocessor,
     * which is {@link DiSHPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return DiSHPreprocessor.class.getName();
    }
}
