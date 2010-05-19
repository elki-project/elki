package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboPopup;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

public class ClassListParameterConfigurator extends AbstractSingleParameterConfigurator<ClassListParameter<?>> implements ActionListener, ChangeListener {
  final ConfiguratorPanel child;

  /**
   * We need a panel to put our components on.
   */
  final JPanel panel;

  /**
   * Text field to store the name
   */
  final JTextField textfield;

  /**
   * The button to open the file selector
   */
  final JButton button;

  /**
   * The combobox we are abusing to produce the popup
   */
  final JComboBox combo;

  /**
   * The popup menu.
   */
  final SuperPopup popup;

  public ClassListParameterConfigurator(ClassListParameter<?> cp, JComponent parent) {
    super(cp, parent);
    textfield = new JTextField();
    colorize(textfield);
    textfield.setToolTipText(param.getShortDescription());
    if(cp.isDefined() && !cp.tookDefaultValue()) {
      textfield.setText(cp.getValueAsString());
    }

    button = new JButton("+");
    button.setToolTipText(param.getShortDescription());
    button.addActionListener(this);
    // So the first item doesn't get automatically selected
    combo = new JComboBox();
    combo.setEditable(true);
    popup = new SuperPopup(combo);

    // fill dropdown menu
    {
      // Offer the shorthand version of class names.
      String prefix = cp.getRestrictionClass().getPackage().getName() + ".";
      for(Class<?> impl : cp.getKnownImplementations()) {
        String name = impl.getName();
        if(name.startsWith(prefix)) {
          name = name.substring(prefix.length());
        }
        combo.addItem(name);
      }
    }
    combo.addActionListener(this);

    // setup panel
    {
      panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(textfield, BorderLayout.CENTER);
      panel.add(button, BorderLayout.EAST);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      parent.add(panel, constraints);
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

    textfield.addActionListener(this);
  }

  @Override
  public void addParameter(Object owner, Parameter<?, ?> param) {
    // FIXME: only set the border once!
    child.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
    child.addParameter(owner, param);
  }

  /**
   * Callback to show the popup menu
   */
  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == button) {
      popup.show(panel);
    }
    else if(e.getSource() == combo) {
      String newClass = (String) combo.getSelectedItem();
      if(newClass != null && newClass.length() > 0) {
        String val = textfield.getText();
        if(val.length() > 0) {
          val = val + ClassListParameter.LIST_SEP + newClass;
        }
        else {
          val = newClass;
        }
        textfield.setText(val);
        popup.hide();
        fireValueChanged();
      }
    }
    else if(e.getSource() == textfield) {
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: " + e.getSource());
    }
  }

  // FIXME: Refactor - duplicate code.
  class SuperPopup extends BasicComboPopup {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param combo Combo box used for data storage.
     */
    public SuperPopup(JComboBox combo) {
      super(combo);
    }

    /**
     * Show the menu on a particular panel.
     * 
     * This code is mostly copied from {@link BasicComboPopup#getPopupLocation}
     * 
     * @param parent Parent element to show at.
     */
    public void show(JPanel parent) {
      Dimension popupSize = parent.getSize();
      Insets insets = getInsets();

      // reduce the width of the scrollpane by the insets so that the popup
      // is the same width as the combo box.
      popupSize.setSize(popupSize.width - (insets.right + insets.left), getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
      Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height, popupSize.width, popupSize.height);
      Dimension scrollSize = popupBounds.getSize();

      scroller.setMaximumSize(scrollSize);
      scroller.setPreferredSize(scrollSize);
      scroller.setMinimumSize(scrollSize);

      list.revalidate();

      super.show(parent, 0, parent.getBounds().height);
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() == child) {
      fireValueChanged();
    } else {
      LoggingUtil.warning("stateChanged triggered by unknown source: "+e.getSource());
    }
  }
  
  @Override
  public Object getUserInput() {
    return textfield.getText();
  }

  @Override
  public void appendParameters(ListParameterization params) {
    super.appendParameters(params);
    child.appendParameters(params);
  }
}