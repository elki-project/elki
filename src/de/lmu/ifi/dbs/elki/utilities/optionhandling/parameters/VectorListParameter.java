package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a list of vectors.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class VectorListParameter extends ListParameter<List<Double>> {
  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraints
   * @param defaultValue
   */
  public VectorListParameter(OptionID optionID, List<ParameterConstraint<List<List<Double>>>> constraints, List<List<Double>> defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraints
   * @param optional
   */
  public VectorListParameter(OptionID optionID, List<ParameterConstraint<List<List<Double>>>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraints
   */
  // Indiscernible from optionID, defaults
  /*
   * public VectorListParameter(OptionID optionID, List<ParameterConstraint<?
   * super List<List<Double>>>> constraints) { super(optionID, constraints); }
   */

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraint
   * @param defaultValue
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint, List<List<Double>> defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param constraint
   * @param optional
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint, boolean optional) {
    super(optionID, constraint, optional);
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
   * @param defaultValue
   */
  // Indiscernible from optionID, constraints
  /*
   * public VectorListParameter(OptionID optionID, List<List<Double>>
   * defaultValue) { super(optionID, defaultValue); }
   */

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   * @param optional
   */
  public VectorListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID
   */
  public VectorListParameter(OptionID optionID) {
    super(optionID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    StringBuffer buf = new StringBuffer();
    List<List<Double>> val = getValue();
    Iterator<List<Double>> valiter = val.iterator();
    while(valiter.hasNext()) {
      List<Double> vec = valiter.next();
      Iterator<Double> veciter = vec.iterator();
      while(veciter.hasNext()) {
        buf.append(Double.toString(veciter.next()));
        if (veciter.hasNext()) {
          buf.append(LIST_SEP);
        }
      }
      // Append separation character
      if (valiter.hasNext()) {
        buf.append(VECTOR_SEP);
      }
    }
    return buf.toString();
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected List<List<Double>> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        List<?> v = List.class.cast(o);
        for(Object c : v) {
          if(!(c instanceof Double)) {
            throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given list contains objects of different type!");
          }
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list and vectors?
      return (List<List<Double>>) l;
    }
    catch(ClassCastException e) {
      // continue with other attempts.
    }
    if(obj instanceof String) {
      String[] vectors = VECTOR_SPLIT.split((String) obj);
      if(vectors.length == 0) {
        throw new UnspecifiedParameterException("Wrong parameter format! Given list of vectors for parameter \"" + getName() + "\" is empty!");
      }
      ArrayList<List<Double>> vecs = new ArrayList<List<Double>>();

      for(String vector : vectors) {
        String[] coordinates = SPLIT.split(vector);
        ArrayList<Double> vectorCoord = new ArrayList<Double>();
        for(String coordinate : coordinates) {
          try {
            Double.parseDouble(coordinate);
            vectorCoord.add(Double.parseDouble(coordinate));
          }
          catch(NumberFormatException e) {
            throw new WrongParameterValueException("Wrong parameter format! Coordinates of vector \"" + vector + "\" are not valid!");
          }
        }
        vecs.add(vectorCoord);
      }
      return vecs;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Double values!");
  }

  /**
   * Returns an array containing the individual vector sizes of this vector list
   * parameter.
   * 
   * @return the individual vector sizes
   */
  // unused?
  /*
   * public int[] vectorSizes() {
   * 
   * int[] sizes = new int[getListSize()];
   * 
   * int i = 0; for(List<?> vecs : value) { sizes[i] = vecs.size(); i++; }
   * 
   * return sizes; }
   */

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
