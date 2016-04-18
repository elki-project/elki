package de.lmu.ifi.dbs.elki.visualization.savedialog;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A component (JPanel) which can be displayed in the save dialog to show
 * additional options when saving as JPEG or PNG.
 * 
 * @author Simon Mittermüller
 * @since 0.2
 */
public class SaveOptionsPanel extends JPanel {
  // TODO: externalize strings
  private static final String STR_IMAGE_SIZE = "Size options:";

  private static final String STR_JPEG_QUALITY = "Quality:";

  private static final String STR_IMAGE_HEIGHT = "Height:";

  private static final String STR_IMAGE_WIDTH = "Width:";

  private static final String STR_CHOOSE_FORMAT = "Choose format:";

  private static final String STR_RESET_IMAGE_SIZE = "Reset image size";

  private static final String STR_LOCK_ASPECT_RATIO = "Lock aspect ratio";

  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /** The fileChooser on which this panel is installed. */
  private JFileChooser fc;

  /**
   * The width of the exported image (if exported to JPEG or PNG). Default is
   * 1024.
   */
  protected int width = 1024;

  /**
   * The height of the exported image (if exported to JPEG or PNG). Default is
   * 768.
   */
  protected int height = 768;

  /** Ratio for easier size adjustment */
  double ratio = 16.0 / 10.0;

  /** Main panel */
  private JPanel mainPanel;

  /** Shows quality info when saving as JPEG. */
  private JPanel qualPanel;

  /** If saving as JPEG/PNG show width/height infos here. */
  private JPanel sizePanel;

  protected JSpinner spinnerWidth;

  protected JSpinner spinnerHeight;

  protected JSpinner spinnerQual;

  protected SpinnerNumberModel modelWidth;

  protected SpinnerNumberModel modelHeight;

  protected SpinnerNumberModel modelQuality;

  protected JCheckBox aspectRatioLock;

  protected JButton resetSizeButton;

  protected JComboBox<String> formatSelector;

  // Not particularly useful for most - hide it for now.
  private final boolean hasResetButton = false;

  /**
   * Construct a new Save Options Panel.
   * 
   * @param fc File chooser to display in
   * @param w Default image width
   * @param h Default image height
   */
  public SaveOptionsPanel(JFileChooser fc, int w, int h) {
    this.width = w;
    this.height = h;
    this.ratio = (double) w / (double) h;
    this.fc = fc;

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

    sizePanel = new JPanel();
    sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.Y_AXIS));
    sizePanel.setBorder(BorderFactory.createTitledBorder(STR_IMAGE_SIZE));

    // *** Format panel
    mainPanel.add(new JLabel(STR_CHOOSE_FORMAT));

    formatSelector = new JComboBox<>(SVGSaveDialog.getVisibleFormats());
    formatSelector.setSelectedIndex(0);
    formatSelector.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getItem() != null) {
          String format = (String) formatSelector.getSelectedItem();
          setFormat(format);
        }
      }
    });
    mainPanel.add(formatSelector);

    // *** Size panel
    JPanel widthPanel = new JPanel();
    JPanel heightPanel = new JPanel();
    widthPanel.add(new JLabel(STR_IMAGE_WIDTH));
    heightPanel.add(new JLabel(STR_IMAGE_HEIGHT));

    // size models
    modelWidth = new SpinnerNumberModel(width, 0, 100000, 1);
    modelHeight = new SpinnerNumberModel(height, 0, 100000, 1);

    // size spinners
    spinnerWidth = new JSpinner(modelWidth);
    spinnerWidth.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if(aspectRatioLock.isSelected()) {
          int val = modelWidth.getNumber().intValue();
          spinnerHeight.setValue(new Integer((int) Math.round(val / ratio)));
        }
      }
    });
    widthPanel.add(spinnerWidth);

    spinnerHeight = new JSpinner(modelHeight);
    spinnerHeight.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if(aspectRatioLock.isSelected()) {
          int val = modelHeight.getNumber().intValue();
          spinnerWidth.setValue(new Integer((int) Math.round(val * ratio)));
        }
      }
    });
    heightPanel.add(spinnerHeight);

    // add subpanels
    sizePanel.add(widthPanel);
    sizePanel.add(heightPanel);

    // aspect lock
    aspectRatioLock = new JCheckBox(STR_LOCK_ASPECT_RATIO);
    aspectRatioLock.setSelected(true);
    // aspectRatioLock.addActionListener(x);
    sizePanel.add(aspectRatioLock);

    // reset size button
    if(hasResetButton) {
      resetSizeButton = new JButton(STR_RESET_IMAGE_SIZE);
      resetSizeButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          modelWidth.setValue(Integer.valueOf(width));
          modelHeight.setValue(Integer.valueOf(height));
          aspectRatioLock.setSelected(true);
        }
      });
      sizePanel.add(resetSizeButton);
    }

    mainPanel.add(sizePanel);

    // Quality settings panel
    qualPanel = new JPanel();
    // quality settings will not be visible by default (JPEG only)
    qualPanel.setVisible(false);
    qualPanel.setLayout(new BoxLayout(qualPanel, BoxLayout.Y_AXIS));
    qualPanel.setBorder(BorderFactory.createTitledBorder(STR_JPEG_QUALITY));
    modelQuality = new SpinnerNumberModel(0.7, 0.1, 1.0, 0.1);
    spinnerQual = new JSpinner(modelQuality);
    // spinnerQual.addChangeListener(x);
    qualPanel.add(spinnerQual);

    mainPanel.add(qualPanel);

    add(mainPanel);

    // setup a listener to react to file name changes
    this.fc.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if(e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
          File file = (File) e.getNewValue();
          if(file != null && file.getName() != null) {
            String format = SVGSaveDialog.guessFormat(file.getName());
            if(format != null) {
              setFormat(format);
            }
          }
        }
      }
    });
  }

  protected void setFormat(String format) {
    String[] formats = SVGSaveDialog.getVisibleFormats();
    int index = -1;
    for(int i = 0; i < formats.length; i++) {
      if(formats[i].equals(format)) {
        index = i;
        break;
      }
    }
    if(index != formatSelector.getSelectedIndex() && index >= 0) {
      formatSelector.setSelectedIndex(index);
    }
    if("jpeg".equals(format) || "jpg".equals(format)) {
      sizePanel.setVisible(true);
      qualPanel.setVisible(true);
    }
    else if("png".equals(format)) {
      sizePanel.setVisible(true);
      qualPanel.setVisible(false);
    }
    else if("pdf".equals(format)) {
      sizePanel.setVisible(false);
      qualPanel.setVisible(false);
      mainPanel.validate();
    }
    else if("ps".equals(format)) {
      sizePanel.setVisible(false);
      qualPanel.setVisible(false);
      mainPanel.validate();
    }
    else if("eps".equals(format)) {
      sizePanel.setVisible(false);
      qualPanel.setVisible(false);
      mainPanel.validate();
    }
    else if("svg".equals(format)) {
      sizePanel.setVisible(false);
      qualPanel.setVisible(false);
      mainPanel.validate();
    }
    else {
      // TODO: what to do on unknown formats?
      // LoggingUtil.warning("Unrecognized file extension seen: " + format);
    }
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
   * Returns the quality value in the quality field.
   * 
   * It is ensured that return value is in the range of [0:1]
   * 
   * @return Quality value for JPEG.
   */
  public double getJPEGQuality() {
    double qual =modelQuality.getNumber().doubleValue();
    if(qual > 1.0) {
      qual = 1.0;
    }
    if(qual < 0) {
      qual = 0.0;
    }
    return qual;
  }
}