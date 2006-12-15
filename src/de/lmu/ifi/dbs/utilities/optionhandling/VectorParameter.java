package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameter class for a parameter specifying a vector.
 * 
 * @author Steffi Wanka
 *
 */
public class VectorParameter extends ListParameter<List> {

	/**
	 * Constructs a vector parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public VectorParameter(String name, String description) {
		super(name, description);
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
	 */
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {

			String[] vectors = VECTOR_SPLIT.split(value);

			ArrayList<List> vecs = new ArrayList<List>();

			for (int c = 0; c < vectors.length; c++) {
				String[] coordinates = SPLIT.split(vectors[c]);

				ArrayList<Double> vectorCoord = new ArrayList<Double>();
				for (int d = 0; d < coordinates.length; d++) {

					vectorCoord.add(Double.parseDouble(coordinates[d]));
				}
				vecs.add(vectorCoord);
			}
			this.value = vecs;
		}
	}

	public int[] vectorSizes() {

		int[] sizes = new int[getListSize()];

		int i = 0;
		for (List vecs : value) {
			sizes[i] = vecs.size();
			i++;
		}

		return sizes;
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {

		String[] vectors = VECTOR_SPLIT.split(value);
		if (vectors.length == 0) {

			throw new WrongParameterValueException(
					"Wrong parameter format! Given list of vectors for parameter \""
							+ getName()
							+ "\" is either empty or has the wrong format!\nParameter value required:\n"
							+ getDescription());
		}

		//TODO dies besser als vector constraint! Kann ja sein, dass die vectoren 
		// unterschiedliche laengen haben duerfen!
		int firstLength = -1;
		for (int c = 0; c < vectors.length; c++) {
			String[] coordinates = SPLIT.split(vectors[c]);
			if (c == 0) {
				firstLength = coordinates.length;
			} else if (coordinates.length != firstLength) {
				throw new WrongParameterValueException("Given vectors for parameter \"" + getName()
						+ "\" have different dimensions!");
			}

			for (int d = 0; d < coordinates.length; d++) {
				try {
					Double.parseDouble(coordinates[d]);
				} catch (NumberFormatException e) {
					throw new WrongParameterValueException(
							"Wrong parameter format! Coordinates of vector \"" + vectors[c]
									+ "\" are not valid!");
				}
			}
		}
		return true;
	}

}
