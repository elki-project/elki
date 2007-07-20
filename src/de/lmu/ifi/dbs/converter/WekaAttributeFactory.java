package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * A factory to create WekaAttributes
 * out of Strings.
 *
 * @author Arthur Zimek
 */
public final class WekaAttributeFactory// implements Parameterizable
{
  /**
   * The classLabel class for nominal attributes.
   */
  private Class<HierarchicalClassLabel> classLabelClass = HierarchicalClassLabel.class;

  /**
   * Creates a {@link WekaNumericAttribute WekaNumericAttribute}
   * if the given value can be parsed as double.
   * Otherwise a {@link WekaNominalAttribute WekaNominalAttribute}
   * is created.
   *
   * @param value the value of the new attribute.
   * @return a new weka attribute for the given value
   */
  public WekaAttribute getAttribute(String value) {
    try {
      Double dvalue = Double.parseDouble(value);
      return  new WekaNumericAttribute(dvalue);
    }
    catch (NumberFormatException e) {
      ClassLabel<HierarchicalClassLabel> classLabel = null;
      try {
        classLabel = Util.instantiate(ClassLabel.class, classLabelClass.getName());
      }
      catch (UnableToComplyException e1) {
        throw new RuntimeException("This should never happen!", e);
      }
      classLabel.init(value);
      return  new WekaNominalAttribute(classLabel);
    }
  }

  /**
   * Returns a {@link WekaStringAttribute WekaStringAttribute}
   * if the parameter string is set to true.
   * Otherwise the result is the same as from
   * {@link #getAttribute(String) getAttribute(value)}.
   *
   * @param value  the value of the new attribute
   * @param string if true, the new attribute will be a {@link WekaStringAttribute WekaStringAttribute},
   *               otherwise it will be the result of
   *               {@link #getAttribute(String) getAttribute(value)}
   * @return a {@link WekaStringAttribute WekaStringAttribute}
   *         if the parameter string is set to true.
   *         Otherwise the result is the same as from
   *         {@link #getAttribute(String) getAttribute(value)}
   */
  public WekaAttribute getAttribute(String value, boolean string) {
    if (string) {
      return new WekaStringAttribute(value);
    }
    else {
      return getAttribute(value);
    }
  }


}
