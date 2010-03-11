package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

/**
 * Interface to define the required methods for command line interaction.
 * 
 * <b>Important note:</b>
 * 
 * Although this cannot be specified in a Java interface, any class implementing
 * this interface <em>must</em> also have a constructor that takes a single
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization}
 * as option, which is used to set the class parameters.
 * 
 * @author Arthur Zimek
 */
public interface Parameterizable {
  // Empty marker interface - the OldDescription requirements cannot be specified
  // in Java!
}