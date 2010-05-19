package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

public class FlagParameterConfigurator extends AbstractParameterConfigurator<Flag> implements ActionListener {
  final JCheckBox value;

  public FlagParameterConfigurator(Flag param, JComponent parent) {
    super(param, parent);

    // Input field
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 2;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    value = new JCheckBox(param.getName());
    if(param.isDefined() && !param.tookDefaultValue()) {
      value.setSelected(param.getValue());
    }
    value.setToolTipText(param.getShortDescription());
    parent.add(value, constraints);
    finishGridRow();
    
    value.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == value) {
      fireValueChanged();
    } else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: "+e.getSource());
    }
  }
  
  @Override
  public Object getUserInput() {
    return value.isSelected() ? true : null;
  }
}