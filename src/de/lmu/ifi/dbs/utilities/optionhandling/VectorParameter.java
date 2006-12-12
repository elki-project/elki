package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;

public class VectorParameter extends ListParameter<List> {

	public VectorParameter(String name, String description) {
		super(name, description);

	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	@Override
	public List<List> getValue() throws UnusedParameterException {
    if (value == null)
      throw new UnusedParameterException("Parameter " + name + " is not specified!");

    return value;
  }

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
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

	public boolean isValid(String value) throws ParameterException {

		String[] vectors = VECTOR_SPLIT.split(value);
		if (vectors.length == 0) {

			throw new WrongParameterValueException(
					"Wrong parameter format! Given list of vectors for parameter \""
							+ getName()
							+ "\" is either empty or has the wrong format!\nParameter value required:\n"
							+ getDescription());
		}

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
