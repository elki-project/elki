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
package de.lmu.ifi.dbs.elki.visualization.savedialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * A component (JPanel) which can be displayed in the save dialog to show
 * additional options when saving as JPEG or PNG.
 *
 * @author Simon Mittermüller
 * @since 0.2
 */
public class SaveOptionsPanel extends JPanel {
  private static final String STR_CHOOSE_FORMAT = "Format";

  private static final String STR_IMAGE_WIDTH = "Width";

  private static final String STR_IMAGE_HEIGHT = "Height";

  private static final String STR_LOCK_ASPECT_RATIO = "Ratio lock";

  private static final String STR_JPEG_QUALITY = "Quality";

  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /** Ratio for easier size adjustment */
  double ratio = 16.0 / 10.0;

  protected JSpinner spinnerWidth, spinnerHeight, spinnerQual;

  protected SpinnerNumberModel modelWidth, modelHeight, modelQual;

  protected JCheckBox checkAspectRatio;

  protected JButton resetSizeButton;

  protected JComboBox<String> formatSelector;

  private JLabel labelWidth, labelHeight, labelQual;

  /**
   * Construct a new Save Options Panel.
   *
   * @param fc File chooser to display in
   * @param width Default image width
   * @param height Default image height
   */
  public SaveOptionsPanel(JFileChooser fc, final int width, final int height) {
    this.setLayout(new GridBagLayout());
    GridBagConstraints left = new GridBagConstraints(),
        right = new GridBagConstraints(), both = new GridBagConstraints();
    left.gridx = 0;
    left.anchor = GridBagConstraints.WEST;
    left.ipadx = 2;
    right.gridx = 1;
    right.weightx = 1;
    right.anchor = GridBagConstraints.WEST;
    right.ipadx = 2;
    both.gridx = 0;
    both.gridwidth = 2;
    both.ipadx = 2;

    this.add(new JLabel(STR_CHOOSE_FORMAT), left);
    this.add(formatSelector = new JComboBox<>(SVGSaveDialog.getVisibleFormats()), right);
    formatSelector.setSelectedIndex(0);
    formatSelector.addItemListener((e) -> setFormat((String) e.getItem()));

    // size spinners
    modelWidth = new SpinnerNumberModel(width, 0, 90000, 1);
    this.add(labelWidth = new JLabel(STR_IMAGE_WIDTH), left);
    this.add(spinnerWidth = new JSpinner(modelWidth), right);
    spinnerWidth.addChangeListener((e) -> {
      if(checkAspectRatio.isSelected()) {
        spinnerHeight.setValue((int) Math.round(modelWidth.getNumber().intValue() / ratio));
      }
    });

    modelHeight = new SpinnerNumberModel(height, 0, 90000, 1);
    this.add(labelHeight = new JLabel(STR_IMAGE_HEIGHT), left);
    this.add(spinnerHeight = new JSpinner(modelHeight), right);
    spinnerHeight.addChangeListener((e) -> {
      if(checkAspectRatio.isSelected()) {
        spinnerWidth.setValue((int) Math.round(modelHeight.getNumber().intValue() * ratio));
      }
    });

    // aspect lock
    this.ratio = (double) width / (double) height;
    checkAspectRatio = new JCheckBox(STR_LOCK_ASPECT_RATIO);
    checkAspectRatio.setSelected(true);
    this.add(checkAspectRatio, both);

    modelQual = new SpinnerNumberModel(0.75, 0.1, 1.0, 0.05);
    this.add(labelQual = new JLabel(STR_JPEG_QUALITY), left);
    this.add(spinnerQual = new JSpinner(modelQual), right);

    // setup a listener to react to file name changes
    fc.addPropertyChangeListener((e) -> {
      if(e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
        File file = (File) e.getNewValue();
        if(file != null && file.getName() != null) {
          setFormat(SVGSaveDialog.guessFormat(file.getName()));
        }
      }
    });

    setFormat(SVGSaveDialog.VISIBLE_FORMATS[0]);
  }

  protected void setFormat(String format) {
    String[] formats = SVGSaveDialog.getVisibleFormats();
    int index = -1;
    for(int i = 0; i < formats.length; i++) {
      if(formats[i].equalsIgnoreCase(format)) {
        index = i;
        break;
      }
    }
    if(index != formatSelector.getSelectedIndex() && index >= 0) {
      formatSelector.setSelectedIndex(index);
    }
    boolean jpeg = "jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format);
    boolean hassize = jpeg || "png".equalsIgnoreCase(format);
    labelWidth.setVisible(hassize);
    spinnerWidth.setVisible(hassize);
    labelHeight.setVisible(hassize);
    spinnerHeight.setVisible(hassize);
    checkAspectRatio.setVisible(hassize);
    labelQual.setVisible(jpeg);
    spinnerQual.setVisible(jpeg);
    this.revalidate();
  }

  /**
   * Return the selected file format.
   *
   * @return file format identification
   */
  public String getSelectedFormat() {
    return (String) formatSelector.getSelectedItem();
  }

  /**
   * Get the user selected width.
   *
   * @return Width
   */
  public int getSelectedWidth() {
    return (int) (modelWidth.getValue());
  }

  /**
   * Get the user selected height.
   *
   * @return Height
   */
  public int getSelectedHeight() {
    return (int) (modelHeight.getValue());
  }

  /**
   * Returns the quality value in the quality field.
   *
   * It is ensured that return value is in the range of [0:1]
   *
   * @return Quality value for JPEG.
   */
  public float getJPEGQuality() {
    float qual = modelQual.getNumber().floatValue();
    return (qual > 1.f) ? 1.f : (qual < 0.f) ? 0.f : qual;
  }
}