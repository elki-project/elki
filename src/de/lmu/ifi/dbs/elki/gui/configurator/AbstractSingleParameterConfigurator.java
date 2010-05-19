package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

public abstract class AbstractSingleParameterConfigurator<T extends Parameter<?, ?>> extends AbstractParameterConfigurator<T> {
  final JLabel label;

  public AbstractSingleParameterConfigurator(T param, JComponent parent) {
    super(param, parent);
    // Label
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    label = new JLabel(param.getName());
    label.setAlignmentX(0.0f);
    label.setToolTipText(param.getShortDescription());
    parent.add(label, constraints);
    // subclasses will add a second component to the row!
  }
}
