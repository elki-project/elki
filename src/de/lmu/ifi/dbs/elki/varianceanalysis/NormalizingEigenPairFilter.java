package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * The NormalizingEigenPairFilter normalizes all eigenvectors s.t.
 * <eigenvector, eigenvector> * eigenvalue = 1, where <,> is the standard dot product
 *
 * @author Simon Paradies
 */

public class NormalizingEigenPairFilter extends AbstractParameterizable
		implements EigenPairFilter {

	/**
	 * Provides a new EigenPairFilter that normalizes all eigenvectors s.t.
	 * eigenvalue * <eigenvector, eigenvector> = 1, where <,> is the standard dot product
	 */
	public NormalizingEigenPairFilter() {
		super();
	}

	/**
	 * @see EigenPairFilter#filter(de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs)
	 */
	public FilteredEigenPairs filter(final SortedEigenPairs eigenPairs) {
		final StringBuffer msg = new StringBuffer();
		// init strong and weak eigenpairs
		// all normalized eigenpairs are regarded as strong
		final List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
		final List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();
		for (int i = 0; i < eigenPairs.size(); i++) {
			final EigenPair eigenPair = eigenPairs.getEigenPair(i);
			normalizeEigenPair(eigenPair);
			strongEigenPairs.add(eigenPair);
		}
		if (debug) {
			msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
			msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
			debugFine(msg.toString());
		}

		return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
	}

	/**
	 * Normalizes an eingenpair consisting of eigenvector v and eigenvalue e s.t. <v,v> * e = 1
	 * @param eigenPair the eigenpair to be normalized
	 *
	 */
	private void normalizeEigenPair(final EigenPair eigenPair) {
		final Matrix eigenvector = eigenPair.getEigenvector();
		final double scaling = 1.0 / Math.sqrt(eigenPair.getEigenvalue()) * eigenvector.normF();
		eigenvector.scaleColumn(0, scaling);
	}

	/**
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@Override
	public String[] setParameters(final String[] args) throws ParameterException {
		final String[] remainingParameters = super.setParameters(args);
		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
	 */
	@Override
	public String description() {
		final StringBuffer description = new StringBuffer();
		description.append(PercentageEigenPairFilter.class.getName());
		description.append(" normalizes all eigenpairs, "
				+ " consisting of eigenvalue e and"
				+ " eigenvector v s.t. <v,v> * e = 1,"
				+ " where <,> is the standard dot product.\n");
		description.append(optionHandler.usage("", false));
		return description.toString();
	}

}
