package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The LimitEigenPairFilter marks all eigenpairs having an (absolute) eigenvalue
 * below the specified threshold (relative or absolute) as weak eigenpairs, the
 * others are marked as strong eigenpairs.
 * 
 * @author Elke Achtert 
 */

public class LimitEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {

	/**
	 * Flag for marking parameter delta as an absolute value.
	 */
	public static final String ABSOLUTE_F = "abs";

	/**
	 * Description for flag abs.
	 */
	public static final String ABSOLUTE_D = "flag to mark delta as an absolute value.";

	/**
	 * The default value for delta.
	 */
	public static final double DEFAULT_DELTA = 0.01;

	/**
	 * Option string for parameter delta.
	 */
	public static final String DELTA_P = "delta";

	/**
	 * Description for parameter delta.
	 */
	public static final String DELTA_D = "a double specifying the threshold for "
			+ "strong Eigenvalues. If not otherwise specified, delta " + "is a relative value w.r.t. the (absolute) highest "
			+ "Eigenvalues and has to be a double between 0 and 1 " + "(default is delta = " + DEFAULT_DELTA + "). "
			+ "To mark delta as an absolute value, use " + "the option -" + ABSOLUTE_F + ".";

	/**
	 * Threshold for strong eigenpairs, can be absolute or relative.
	 */
	private double delta;

	/**
	 * Indicates wether delta is an absolute or a relative value.
	 */
	private boolean absolute;

	/**
	 * Provides a new EigenPairFilter that marks all eigenpairs having an
	 * (absolute) eigenvalue below the specified threshold (relative or
	 * absolute) as weak eigenpairs, the others are marked as strong eigenpairs.
	 */
	public LimitEigenPairFilter() {
		super();

		DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D);
		delta.setDefaultValue(DEFAULT_DELTA);
		optionHandler.put(delta);
		// delta must be >= 0 and <= 1 if it's a relative value
		// Since relative or absolute is dependent on the absolute flag this is a
		// global constraint!
		List<ParameterConstraint> cons = new Vector<ParameterConstraint>();
		ParameterConstraint aboveNull = new GreaterEqualConstraint(0);
		cons.add(aboveNull);
		ParameterConstraint underOne = new LessEqualConstraint(1);
		cons.add(underOne);

		Flag abs = new Flag(ABSOLUTE_F, ABSOLUTE_D);
		optionHandler.put(abs);

		GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint(delta, cons, abs, false);
		optionHandler.setGlobalParameterConstraint(gpc);
	}

	/**
	 * @see EigenPairFilter#filter(de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs)
	 */
	public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
		StringBuffer msg = new StringBuffer();
		if (this.debug) {
			msg.append("\ndelta = ").append(delta);
		}

		// determine limit
		double limit;
		if (absolute) {
			limit = delta;
		} else {
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < eigenPairs.size(); i++) {
				EigenPair eigenPair = eigenPairs.getEigenPair(i);
				double eigenValue = Math.abs(eigenPair.getEigenvalue());
				if (max < eigenValue) {
					max = eigenValue;
				}
			}
			limit = max * delta;
		}
		if (this.debug) {
			msg.append("\nlimit = ").append(limit);
		}

		// init strong and weak eigenpairs
		List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
		List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

		// determine strong and weak eigenpairs
		for (int i = 0; i < eigenPairs.size(); i++) {
			EigenPair eigenPair = eigenPairs.getEigenPair(i);
			double eigenValue = Math.abs(eigenPair.getEigenvalue());
			if (eigenValue >= limit) {
				strongEigenPairs.add(eigenPair);
			} else {
				weakEigenPairs.add(eigenPair);
			}
		}
		if (this.debug) {
			msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
			msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
			debugFine(msg.toString());
		}

		return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
	}

	/**
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// absolute
		absolute = optionHandler.isSet(ABSOLUTE_F);

		// delta
		delta = (Double) optionHandler.getOptionValue(DELTA_P);
		if (absolute && ((Parameter) optionHandler.getOption(DELTA_P)).tookDefaultValue()) {
			throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_F + " is set, " + "but no value for "
					+ DELTA_P + " is specified.");
		}

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
	 */
	public String description() {
		StringBuffer description = new StringBuffer();
		description.append(PercentageEigenPairFilter.class.getName());
		description.append(" filters all eigenpairs, " + " which are lower than a given value.\n");
		description.append(optionHandler.usage("", false));
		return description.toString();
	}

}
