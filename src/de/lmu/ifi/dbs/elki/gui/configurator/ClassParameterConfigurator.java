package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

public class ClassParameterConfigurator extends AbstractSingleParameterConfigurator<ClassParameter<?>> implements ActionListener, ChangeListener {
  final JComboBox value;

  final ConfiguratorPanel child;

  public ClassParameterConfigurator(ClassParameter<?> cp, JComponent parent) {
    super(cp, parent);
    // Input field
    {
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      value = new JComboBox();
      value.setToolTipText(param.getShortDescription());
      colorize(value);
      parent.add(value, constraints);
    }

    if(!param.tookDefaultValue() && param.isDefined() && param.getGivenValue() != null) {
      value.addItem(param.getValueAsString());
      value.setSelectedIndex(0);
    }

    // For parameters with a default value, offer using the default
    // For optional parameters, offer not specifying them.
    if(cp.hasDefaultValue()) {
      value.addItem(DynamicParameters.STRING_USE_DEFAULT);
    }
    else if(cp.isOptional()) {
      value.addItem(DynamicParameters.STRING_OPTIONAL);
    }
    // Offer the shorthand version of class names.
    String prefix = cp.getRestrictionClass().getPackage().getName() + ".";
    for(Class<?> impl : cp.getKnownImplementations()) {
      String name = impl.getName();
      if(name.startsWith(prefix)) {
        value.addItem(name.substring(prefix.length()));
      }
      else {
        value.addItem(name);
      }
    }
    // Child options
    {
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      constraints.insets = new Insets(0, 10, 0, 0);
      child = new ConfiguratorPanel();
      child.addChangeListener(this);
      parent.add(child, constraints);
    }
    value.addActionListener(this);
  }

  @Override
  public void addParameter(Object owner, Parameter<?, ?> param) {
    // FIXME: only set the border once!
    child.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
    child.addParameter(owner, param);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == value) {
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: " + e.getSource());
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() == child) {
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("stateChanged triggered by unknown source: " + e.getSource());
    }
  }

  @Override
  public Object getUserInput() {
    String val = (String) value.getSelectedItem();
    if (val == DynamicParameters.STRING_USE_DEFAULT) {
      return null;
    }
    if (val == DynamicParameters.STRING_OPTIONAL) {
      return null;
    }
    return val;
  }

  @Override
  public void appendParameters(ListParameterization params) {
    super.appendParameters(params);
    child.appendParameters(params);
  }
}