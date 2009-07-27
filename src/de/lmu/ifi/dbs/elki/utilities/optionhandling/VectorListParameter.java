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
   * @param optionID
   */
  public VectorListParameter(OptionID optionID) {
    super(optionID);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraint
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraint
   * @param optional
   * @param defaultValue
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint, boolean optional, List<List<Double>> defaultValue) {
    super(optionID, constraint, optional, defaultValue);
  }

  @Override
  public void setValue(String value) throws ParameterException {
    super.setValue(value);
    if(isValid(value)) {
      String[] vectors = VECTOR_SPLIT.split(value);
      ArrayList<List<Double>> vecs = new ArrayList<List<Double>>();

      for(String vector : vectors) {
        String[] coordinates = SPLIT.split(vector);
        ArrayList<Double> vectorCoord = new ArrayList<Double>();
        for(String coordinate : coordinates) {
          vectorCoord.add(Double.parseDouble(coordinate));
        }
        vecs.add(vectorCoord);
      }
      this.value = vecs;
    }
  }

  /**
   * Returns an array containing the individual vector sizes of this vector list
   * parameter.
   * 
   * @return the individual vector sizes
   */
  public int[] vectorSizes() {

    int[] sizes = new int[getListSize()];

    int i = 0;
    for(List<?> vecs : value) {
      sizes[i] = vecs.size();
      i++;
    }

    return sizes;
  }

  @Override
  public boolean isValid(String value) throws ParameterException {

    String[] vectors = VECTOR_SPLIT.split(value);
    if(vectors.length == 0) {

      throw new UnspecifiedParameterException("Wrong parameter format! Given list of vectors for parameter \"" + getName() + "\" is either empty or has the wrong format!\nParameter value required:\n" + getFullDescription());
    }

    List<List<Double>> vecList = new ArrayList<List<Double>>();
    for(String vector : vectors) {
      String[] coordinates = SPLIT.split(vector);
      ArrayList<Double> list = new ArrayList<Double>();

      for(String coordinate : coordinates) {
        try {
          Double.parseDouble(coordinate);
          list.add(Double.parseDouble(coordinate));
        }
        catch(NumberFormatException e) {
          throw new WrongParameterValueException("Wrong parameter format! Coordinates of vector \"" + vector + "\" are not valid!");
        }
      }
      vecList.add(list);
    }

    // check constraints
    for(ParameterConstraint<List<List<Double>>> con : this.constraints) {
      con.test(vecList);
    }
    return true;
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return 
   *         &quot;&lt;double_11,...,double_1n:...:double_m1,...,double_mn&gt;&quot
   *         ;
   */
  @Override
  public String getSyntax() {
    return "<double_11,...,double_1n:...:double_m1,...,double_mn>";
  }
}
