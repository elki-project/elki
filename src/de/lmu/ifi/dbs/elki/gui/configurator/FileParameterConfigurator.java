package de.lmu.ifi.dbs.elki.gui.configurator;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
  @Override
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
  public String getUserInput() {
    return textfield.getText();
  }
}