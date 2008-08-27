package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameter class for a parameter specifying a list of vectors.
 *
 * @author Steffi Wanka
 */
public class VectorListParameter extends ListParameter<List<Double>> {
    /**
     * Constructs a vector list parameter with the given name and description.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @deprecated
     */
    @Deprecated
    public VectorListParameter(String name, String description) {
        super(name, description);
    }

    /**
     * Constructs a vector list parameter with the given name, description, and parameter constraint.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param con         a parameter constraint
     * @deprecated
     */
    @Deprecated
    public VectorListParameter(String name, String description, ParameterConstraint<List<List<Double>>> con) {
        this(name, description);
        addConstraint(con);
        this.constraints.add(con);
    }

    /**
     * Constructs a vector list parameter with the given name, description, and list of parameter constraints.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param cons        a list of parameter constraints
     */
    public VectorListParameter(String name, String description, List<ParameterConstraint<List<List<Double>>>> cons) {
        this(name, description);
        addConstraintList(cons);

    }

    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            String[] vectors = VECTOR_SPLIT.split(value);
            ArrayList<List<Double>> vecs = new ArrayList<List<Double>>();

            for (String vector : vectors) {
                String[] coordinates = SPLIT.split(vector);
                ArrayList<Double> vectorCoord = new ArrayList<Double>();
                for (String coordinate : coordinates) {
                    vectorCoord.add(Double.parseDouble(coordinate));
                }
                vecs.add(vectorCoord);
            }
            this.value = vecs;
        }
    }

    /**
     * Returns an array containing the individual vector sizes of this vector list parameter.
     *
     * @return the individual vector sizes
     */
    public int[] vectorSizes() {

        int[] sizes = new int[getListSize()];

        int i = 0;
        for (List<?> vecs : value) {
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

        List<List<Double>> vecList = new ArrayList<List<Double>>();
        for (String vector : vectors) {
            String[] coordinates = SPLIT.split(vector);
            ArrayList<Double> list = new ArrayList<Double>();

            for (String coordinate : coordinates) {
                try {
                    Double.parseDouble(coordinate);
                    list.add(Double.parseDouble(coordinate));
                }
                catch (NumberFormatException e) {
                    throw new WrongParameterValueException(
                        "Wrong parameter format! Coordinates of vector \"" + vector
                            + "\" are not valid!");
                }
            }
            vecList.add(list);
        }

        // check constraints
        for (ParameterConstraint<List<List<Double>>> con : this.constraints) {
            con.test(vecList);
        }
        return true;
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;double_11,...,double_1n:...:double_m1,...,double_mn&gt;&quot;
     */
    protected String getParameterType() {
        return "<double_11,...,double_1n:...:double_m1,...,double_mn>";
    }
}
