package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;


/**
 * Interface to define the required methods for command line interaction.
 *
 * @author Arthur Zimek
 */
public interface Parameterizable {
  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description as for a standalone application.
   *
   * @return String a description of the class and the required parameters
   */
  String description();
  
  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description as for an inline application,
   * i.e. the usage of a Parameterizable within another Parameterizable
   * where this Parameterizable is not a parameter itself but is used and expects parameters.
   *
   * @return String a description of the class and the required parameters
   */
  String inlineDescription();

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws ParameterException in case of wrong parameter-setting
   */
  String[] setParameters(String[] args) throws ParameterException;
  
  /**
   * Returns the parameter array as given to the last call
   * of {@link #setParameters(String[]) setParameters(String[])}
   * but without unnecessary entries.
   * The provided array should be suitable to call
   * {@link #setParameters(String[]) setParameters(String[])}
   * with it resulting in the identical parameterization.
   * 
   * @return the parameter array as given to the last call
   * of {@link #setParameters(String[]) setParameters(String[])}
   * but without unnecessary entries
   */
  String[] getParameters();

  /**
   * Returns the setting of the attributes of the parameterizable.
   *
   * @return the setting of the attributes of the parameterizable
   */
  public List<AttributeSettings> getAttributeSettings();
  
  /**
   * Returns an array containing all options of this parameterizable object
   * 
   * @return the options of this parameterizable object
   */
  Option<?>[] getPossibleOptions();
  
  /**
   * Checks if all global parameter constraints are kept
   *
   */
  void checkGlobalParameterConstraints() throws ParameterException;
}
