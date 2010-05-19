package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.elki.gui.icons.StockIcon;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

public class FileParameterConfigurator extends AbstractSingleParameterConfigurator<FileParameter> implements ActionListener {
  /**
   * The panel to store the components
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
   * The actual file chooser
   */
  final JFileChooser fc = new JFileChooser();

  public FileParameterConfigurator(FileParameter fp, JComponent parent) {
    super(fp, parent);
    // create components
    textfield = new JTextField();
    textfield.setToolTipText(param.getShortDescription());
    textfield.addActionListener(this);
    button = new JButton(StockIcon.getStockIcon(StockIcon.DOCUMENT_OPEN));
    button.setToolTipText(param.getShortDescription());
    button.addActionListener(this);
    // fill with value
    File f = null;
    if(fp.isDefined()) {
      f = fp.getValue();
    }
    if(f != null) {
      String fn = f.getPath();
      textfield.setText(fn);
      fc.setSelectedFile(f);
    }
    else {
      textfield.setText("");
      fc.setSelectedFile(null);
    }

    // make a panel
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    panel = new JPanel(new BorderLayout());
    panel.add(textfield, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);
    
    parent.add(panel, constraints);
    finishGridRow();
  }

  /**
   * Button callback to show the file selector
   */
  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == button) {
      int returnVal = fc.showOpenDialog(button);

      if(returnVal == JFileChooser.APPROVE_OPTION) {
        textfield.setText(fc.getSelectedFile().getPath());
        fireValueChanged();
      }
      else {
        // Do nothing on cancel.
      }
    }
    else if(e.getSource() == textfield) {
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: " + e.getSource());
    }
  }

  @Override
  public Object getUserInput() {
    return textfield.getText();
  }
}