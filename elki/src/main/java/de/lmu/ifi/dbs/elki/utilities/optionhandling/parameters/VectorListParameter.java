package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a list of vectors.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class VectorListParameter extends ListParameter<VectorListParameter, List<Vector>> {
  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   * @param defaultValue Default value
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<Vector>> constraint, List<Vector> defaultValue) {
    super(optionID, defaultValue);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   * @param optional Optional flag
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<Vector>> constraint, boolean optional) {
    super(optionID, optional);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<Vector>> constraint) {
    super(optionID);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param defaultValue Default value
   */
  // Indiscernible from optionID, constraints
  /*
   * public VectorListParameter(OptionID optionID, List<Vector> defaultValue) {
   * super(optionID, defaultValue); }
   */

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param optional Optional flag
   */
  public VectorListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   */
  public VectorListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<Vector> val = getValue();
    Iterator<Vector> valiter = val.iterator();
    while(valiter.hasNext()) {
      buf.append(FormatUtil.format(valiter.next().getArrayRef(), LIST_SEP));
      // Append separation character
      if(valiter.hasNext()) {
        buf.append(VECTOR_SEP);
      }
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Vector> parseValue(Object obj) throws ParameterException {
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
      return (List<Vector>) l;
    }
    catch(ClassCastException e) {
      // continue with other attempts.
    }
    if(obj instanceof String) {
      String[] vectors = VECTOR_SPLIT.split((String) obj);
      if(vectors.length == 0) {
        throw new WrongParameterValueException("Wrong parameter format! Given list of vectors for parameter \"" + getName() + "\" is empty!");
      }
      ArrayList<Vector> vecs = new ArrayList<>();

      DoubleArray vectorCoord = new DoubleArray();
      for(String vector : vectors) {
        vectorCoord.clear();
        String[] coordinates = SPLIT.split(vector);
        for(String coordinate : coordinates) {
          try {
            vectorCoord.add(FormatUtil.parseDouble(coordinate));
          }
          catch(NumberFormatException e) {
            throw new WrongParameterValueException("Wrong parameter format! Coordinates of vector \"" + vector + "\" are not valid!");
          }
        }
        vecs.add(new Vector(vectorCoord.toArray()));
      }
      return vecs;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of double values!");
  }

  @Override
  public int size() {
    return getValue().size();
  }

  /**
   * Returns a string representation of the parameter's type.
   *
   * @return &quot;&lt;double_11,...,double_1n:...:double_m1,...,double_mn&gt;&
   *         quot ;
   */
  @Override
  public String getSyntax() {
    return "<double_11,...,double_1n:...:double_m1,...,double_mn>";
  }
}
