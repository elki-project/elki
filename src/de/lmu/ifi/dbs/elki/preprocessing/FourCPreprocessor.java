package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 *
 * @author Arthur Zimek
 * @param <D> Distance type
 * @param <V> Vector type
 */
public class FourCPreprocessor<D extends Distance<D>, V extends RealVector<V, ?>> extends ProjectedDBSCANPreprocessor<D, V> {
    /**
     * Flag for marking parameter delta as an absolute value.
     */
    private final Flag ABSOLUTE_PARAM = new Flag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);

    /**
     * Option string for parameter delta.
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA,
        new GreaterEqualConstraint(0), DEFAULT_DELTA);

    /**
     * The default value for delta.
     */
    public static final double DEFAULT_DELTA = LimitEigenPairFilter.DEFAULT_DELTA;

    /**
     * Threshold for strong eigenpairs, can be absolute or relative.
     */
    private double delta;

    /**
     * Indicates whether delta is an absolute or a relative value.
     */
    private boolean absolute;

    /**
     * The Filtered PCA Runner
     */
    private PCAFilteredRunner<V> pca = new PCAFilteredRunner<V>();

    /**
     * Constructor
     */
    public FourCPreprocessor() {
        super();

        // Parameter delta
        // parameter constraint are only valid if delta is a relative value!
        // Thus they are
        // dependent on the absolute flag, that is they are global constraints!
        addOption(DELTA_PARAM);

        // flag absolute
        addOption(ABSOLUTE_PARAM);

        final ArrayList<ParameterConstraint<Number>> deltaCons = new ArrayList<ParameterConstraint<Number>>();
        // TODO: this constraint is already set in the parameter itself, since it also applies to the relative case, right? -- erich
        //deltaCons.add(new GreaterEqualConstraint(0));
        deltaCons.add(new LessEqualConstraint(1));

        GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Number, Double>(DELTA_PARAM, deltaCons, ABSOLUTE_PARAM, false);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    /**
     * This method implements the type of variance analysis to be computed for a
     * given point. <p/> Example1: for 4C, this method should implement a PCA
     * for the given point. Example2: for PreDeCon, this method should implement
     * a simple axis-parallel variance analysis.
     *
     * @param id        the given point
     * @param neighbors the neighbors as query results of the given point
     * @param database  the database for which the preprocessing is performed
     */
    @Override
    protected void runVarianceAnalysis(Integer id, List<DistanceResultPair<D>> neighbors, Database<V> database) {
        List<Integer> ids = new ArrayList<Integer>(neighbors.size());
        for (DistanceResultPair<D> neighbor : neighbors) {
            ids.add(neighbor.getSecond());
        }
        PCAFilteredResult pcares = pca.processIds(ids, database);

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append(id).append(" ").append(database.getAssociation(AssociationID.LABEL, id));
            msg.append("\ncorrDim ").append(pcares.getCorrelationDimension());
            logger.debugFine(msg.toString());
        }
        database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, pcares.getCorrelationDimension());
        database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pcares.similarityMatrix());
    }

    /**
     * Sets the values for the parameters alpha, pca and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // absolute
        absolute = ABSOLUTE_PARAM.isSet();

        // delta

        /* TODO
           * Daran denken: ich kann auch abfragen, ob der default wert gesetzt
           * wurde!! somit kann ich also auf den flag 'absolute' reagieren...
           * Trotzdem ist die abfrage irgendwie seltsam...
           */
        delta = DELTA_PARAM.getValue();
        if (absolute && DELTA_PARAM.tookDefaultValue()) {
            throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_PARAM.getName() + " is set, " + "but no value for "
                + DELTA_PARAM.getName() + " is specified.");
        }
//		if (optionHandler.isSet(DELTA_P)) {
//			delta = (Double) optionHandler.getOptionValue(DELTA_P);
//			try {
//				if (!absolute && delta < 0 || delta > 1)
//					throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D);
//			} catch (NumberFormatException e) {
//				throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D, e);
//			}
//		} else if (!absolute) {
//			delta = LimitEigenPairFilter.DEFAULT_DELTA;
//		} else {
//			throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_F + " is set, " + "but no value for " + DELTA_P + " is specified.");
//		}

        // save parameters for pca
        List<String> tmpPCAParameters = new ArrayList<String>();
        // eigen pair filter
        OptionUtil.addParameter(tmpPCAParameters, PCAFilteredRunner.PCA_EIGENPAIR_FILTER, LimitEigenPairFilter.class.getName());
        // abs
        if (absolute) {
            OptionUtil.addFlag(tmpPCAParameters, LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);
        }
        // delta
        OptionUtil.addParameter(tmpPCAParameters, LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, Double.toString(delta));

        // big value
        OptionUtil.addParameter(tmpPCAParameters, PCAFilteredRunner.BIG_ID, "50");

        // small value
        OptionUtil.addParameter(tmpPCAParameters, PCAFilteredRunner.SMALL_ID, "1");

        String[] pcaParameters = tmpPCAParameters.toArray(new String[tmpPCAParameters.size()]);
        pca.setParameters(pcaParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #pca}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(pca.getAttributeSettings());
        return attributeSettings;
    }

    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(FourCPreprocessor.class.getName());
        description.append(" computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n");
        description.append("The PCA is based on epsilon range queries.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }

}