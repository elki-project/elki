package de.lmu.ifi.dbs.elki.gui.configurator;

import javax.swing.event.ChangeListener;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

public interface ParameterConfigurator {
  public void addParameter(Object owner, Parameter<?, ?> param);

  public void addChangeListener(ChangeListener listener);

  public void removeChangeListener(ChangeListener listener);

  public void appendParameters(ListParameterization params);
}