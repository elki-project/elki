package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

// FIXME: update on focus loss?
// FIXME: restrictions for number input?
public class TextParameterConfigurator extends AbstractSingleParameterConfigurator<Parameter<?, ?>> implements ActionListener {
  final JTextField value;

  public TextParameterConfigurator(Parameter<?, ?> param, JComponent parent) {
    super(param, parent);

    // Input field
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    value = new JTextField();
    if(param.isDefined() && !param.tookDefaultValue()) {
      value.setText(param.getValueAsString());
    }
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
    return value.getText();
  }
}
