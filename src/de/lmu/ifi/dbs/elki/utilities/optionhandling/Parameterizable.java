package de.lmu.ifi.dbs.elki.utilities.optionhandling;


/**
 * Interface to define the required methods for command line interaction.
 *
 * @author Arthur Zimek
 */
public interface Parameterizable {
  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  String shortDescription();
  
  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return a list containing the unused parameters
   * @throws ParameterException in case of wrong parameter-setting
   */
  /* List<String> setParameters(List<String> args) throws ParameterException; */
  
  //boolean setParameters(Parameterization config) throws ParameterException;
  
  /**
   * Returns the parameter array as given to the last call
   * of {@link #setParameters(List) setParameters(List)}
   * but without unnecessary entries.
   * The provided array should be suitable to call
   * {@link #setParameters(List) setParameters(List)}
   * with it resulting in the identical parameterization.
   * 
   * @return the parameter array as given to the last call
   * of {@link #setParameters(List) setParameters(List)}
   * but without unnecessary entries
   */
  //List<String> getParameters();

  /**
   * Fill the given collection with a list of available options.
   *  
   * @return array of parameterizable and options
   */
  //public ArrayList<Pair<Parameterizable, Option<?>>> collectOptions();  
  
  /**
   * Get this objects options.
   */
  //public List<Option<?>> getOptions();
}
