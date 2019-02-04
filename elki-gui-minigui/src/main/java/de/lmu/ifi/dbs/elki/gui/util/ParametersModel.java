/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.gui.util;

import javax.swing.table.AbstractTableModel;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * A Swing TableModel that uses a {@link DynamicParameters} object as storage.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class ParametersModel extends AbstractTableModel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(ParametersModel.class);

  /**
   * Parameter storage
   */
  private DynamicParameters parameters;

  /**
   * Column headers in model
   */
  public static final String[] columns = { "Parameter", "Value" };

  /**
   * Constructor
   * 
   * @param parameters Parameter store
   */
  public ParametersModel(DynamicParameters parameters) {
    super();
    this.parameters = parameters;
  }

  @Override
  public int getColumnCount() {
    return columns.length;
  }

  @Override
  public int getRowCount() {
    return parameters.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if(rowIndex < parameters.size()) {
      DynamicParameters.Node node = parameters.getNode(rowIndex);
      if(columnIndex == 0) {
        return node;
      }
      else if(columnIndex == 1) {
        String ret = node.value;
        if(ret == null) {
          ret = "";
        }
        return ret;
      }
      return "";
    }
    else {
      return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    return columns[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if(columnIndex == 0) {
      return Parameter.class;
    }
    return String.class;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return (columnIndex == 1) || (rowIndex > parameters.size());
  }

  @Override
  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    if(value instanceof String) {
      String s = (String) value;
      if(columnIndex == 1) {
        // Unchanged?
        if(s.equals(parameters.getNode(rowIndex).value)) {
          return;
        }
        // Default value:
        if((DynamicParameters.STRING_USE_DEFAULT + s).equals(parameters.getNode(rowIndex).value)) {
          return;
        }
        parameters.getNode(rowIndex).value = s;
        fireTableCellUpdated(rowIndex, columnIndex);
      }
    }
    else {
      LOG.warning("Edited value is not a String!");
    }
  }
}