package de.lmu.ifi.dbs.elki.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

// todo steffi comment all
public class KDDTaskFrame extends JFrame implements PropertyChangeListener {

    /**
     *
     */
    private static final long serialVersionUID = 7530155806655934877L;

    public static final boolean CENTERED_LOCATION = true;

    public static final int WIDTH = 600;

    public static final int HEIGHT = 380;

    private KDDTask<? extends DatabaseObject> task;

    private String[] dbConnection;

    private String[] algorithm;

    private String[] completeParameters;

    private SelectionPanel dbConnectionPanel;

    private SelectionPanel algorithmPanel;

    private JButton runButton;

    private JTextPane parameterField;

    private JScrollPane scroller;

    private JPanel runPanel;

    private JButton saveButton;

    private JTextField nameField;

    // borders
    private Border loweredEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

//    private Border raisedEtched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

//    private Border loweredBevel = BorderFactory.createBevelBorder(BevelBorder.LOWERED);

//    private Border raisedBevel = BorderFactory.createBevelBorder(BevelBorder.RAISED);

    public KDDTaskFrame() {
        setTitle("KDD Task");

        JPanel mainPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        // task name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        // gbc.ipadx = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(taskNamePanel(), gbc);

        // database connection and algorithm panel
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);

        mainPanel.add(createDBAlgPanel(), gbc);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(mainPanel);
        setMinimumSize(new Dimension(WIDTH, HEIGHT));

        pack();
        dbConnectionPanel.select.requestFocusInWindow();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // task name panel
    private JComponent taskNamePanel() {

        JPanel taskName = new JPanel();
        taskName.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // label
        gbc.gridwidth = 2;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(0, 10, 0, 15);
        taskName.add(new JLabel("Task Name"), gbc);

        // textField
        gbc.gridwidth = 3;
        gbc.gridx = 2;
        // gbc.anchor = GridBagConstraints.FIRST_LINE_END;
        gbc.insets = new Insets(0, 10, 0, 0);
        nameField = new JTextField();
        // default name ist date_time
        nameField.setColumns(15);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.S");
        nameField.setText(formatter.format(cal.getTime()));
        nameField.setCaretPosition(0);

        taskName.add(nameField, gbc);

        taskName.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray), BorderFactory.createEmptyBorder(20, 10, 20, 20)));

        return taskName;
    }

    private String[] getPropertyFileInfo(Class<?> type) {

        // check if we got a property file
        if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

            return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(type));
        }
        return new String[]{};

    }

    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals(OptionID.DATABASE_CONNECTION.getName())) {
            if (evt.getSource() instanceof SelectionPanel) {
                dbConnection = ((SelectionPanel) evt.getSource()).parameterValuesToArray();
                if (dbConnection == null) dbConnection = new String[0];
            }
            else {
                KDDDialog.showMessage(this, "Error while fetching parameters for KDD TaskFrame, exit. Got event from: " + evt.getSource().toString());
            }
//			dbConnection = ((String) evt.getNewValue()).split(" ");
            algorithmPanel.setEnabled(true);

        }
        else if (evt.getPropertyName().equals(OptionID.ALGORITHM.getName())) {
            algorithm = ((String) evt.getNewValue()).split(" ");
            if (algorithm == null) algorithm = new String[0];
            completeParameters = new String[dbConnection.length + algorithm.length];
            System.arraycopy(algorithm, 0, completeParameters, 0, algorithm.length);
            System.arraycopy(dbConnection, 0, completeParameters, algorithm.length, dbConnection.length);
            updateParameterField();
        }

    }

    @SuppressWarnings("unchecked")
    private void runKDDTask(String[] args) {
        task = new KDDTask();
        try {
            task.setParameters(args);
            task.run();
        }
        catch (ParameterException e) {
            KDDDialog.showParameterMessage(this, e.getMessage(), e);
        }
        catch (Exception e) {
            KDDDialog.showErrorMessage(this, e.getMessage(), e);
        }
    }

    private JComponent createDBAlgPanel() {
        JPanel base = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // database connection panel
        dbConnectionPanel = new SelectionPanel("Select a database connection", "Database Connection", FileBasedDatabaseConnection.class.getName(), DatabaseConnection.class);
        dbConnectionPanel.select.setEnabled(true);
        dbConnectionPanel.addPropertyChangeListener(this);
        dbConnectionPanel.propName = OptionID.DATABASE_CONNECTION.getName();

        // algorithm panel
        algorithmPanel = new SelectionPanel("Select an algorithm", "Algorithm", "", Algorithm.class);
        algorithmPanel.addPropertyChangeListener(this);
        algorithmPanel.propName = OptionID.ALGORITHM.getName();
        algorithmPanel.setEnabled(false);

        gbc.insets = new Insets(0, 10, 10, 10);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridwidth = 2;
        base.add(dbConnectionPanel, gbc);
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 10, 10, 10);
        gbc.anchor = GridBagConstraints.LINE_END;
        base.add(algorithmPanel, gbc);

        // text field displaying the complete parameters selected
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 10, 0);
        base.add(runPanel(), gbc);

        // run button
        runButton = new JButton("Run Task");
        runButton.setEnabled(false);
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runKDDTask(completeParameters);
            }
        });
        gbc.gridy = 2;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.LINE_END;
        base.add(runButton, gbc);

        saveButton = new JButton("Save Settings");
        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // KDDDialog.showSaveSettingsDialog(KDDTaskFrame.this);
                KDDFileChooser chooser = new KDDFileChooser(System.getProperty("java.class.path"));
                if (chooser.showSaveDialog(KDDTaskFrame.this) == JFileChooser.APPROVE_OPTION) {
                    saveSettings(chooser.getFileName(), chooser.getFileType());
                }

//				JFileChooser cheesy = new JFileChooser(System.getProperty("java.class.path"));
//				if (cheesy.showSaveDialog(KDDTaskFrame.this) == JFileChooser.APPROVE_OPTION) {
//					saveSettings(cheesy.getSelectedFile().getPath());
//				}
            }
        });
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        base.add(saveButton, gbc);

        base.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        return base;
    }

//    private JPanel createKDDTaskParameterPanel() {
//
//        JPanel parameterPanel = new JPanel(new GridBagLayout());
//
//        return parameterPanel;
//    }

    private JComponent runPanel() {

        runPanel = new JPanel();
        parameterField = new JTextPane() {

            /**
           * 
           */
          private static final long serialVersionUID = -8391444656542286134L;

            public Dimension getPreferredScrollableViewportSize() {
                Dimension dim = getPreferredSize();
                dim.width = 500;

                return dim;
            }

            public void setSize(Dimension d) {

                if (d.width < getParent().getSize().width) {
                    d.width = getParent().getSize().width;

                }

                super.setSize(d);
            }

            public boolean getScrollableTracksViewportWidth() {

                return false;
            }
        };

        this.parameterField.setEditable(false);
        // set the right color
        this.parameterField.setBackground(Color.white);
        scroller = new JScrollPane(parameterField);
        this.scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        runPanel.add(scroller);

        Dimension dim = runPanel.getPreferredSize();
        dim.height = 50;
        runPanel.setPreferredSize(dim);

        return runPanel;
    }

    private void updateParameterField() {
        parameterField.setText(printArray(completeParameters));
        parameterField.setCaretPosition(0);
        runPanel.revalidate();

        runButton.setEnabled(true);
        saveButton.setEnabled(true);
    }

    private String printArray(String[] args) {
        StringBuilder bob = new StringBuilder();
        int i = 0;
        for (String s : args) {
            bob.append(s);
            if (i != args.length - 1) {
                bob.append(" ");
            }
        }

        return bob.toString();
    }

    private void saveSettings(String path, String type) {

        if (type.equals(KDDFileChooser.TEXT_FORMAT)) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                // task name
                writer.write(nameField.getText());
                writer.write("\n");
                writer.write(printArray(completeParameters));
                writer.flush();
                writer.close();
            }
            catch (IOException e) {
                KDDDialog.showErrorMessage(KDDTaskFrame.this, "Could not save settings to file '" + path + "', reason: " + e.getMessage(), e);
            }
        }
        else if (type.equals(KDDFileChooser.XML_FORMAT)) {
            //TODO
        }
        else {
            //TODO
        }


    }

    private class SelectionPanel extends JPanel implements PopUpTreeListener,
        EditObjectChangeListener {

        /**
         *
         */
        private static final long serialVersionUID = 7161605147411986066L;

        /**
         * the popup menu of this panel
         */
        private PopUpTree menu;

        /**
         * the select button
         */
        private JButton select;

        /**
         * the name label
         */
        private JLabel nameLabel;

        /**
         * the text field
         */
        private JTextField textField;

        /**
         * the panel to customize the edit object
         */
        private CustomizerPanel editObjectCustomizer;

        private String propName;

        public SelectionPanel(String buttonToolTip, String labelToolTip, String defaultClassName, Class<?> parentClass) {

            menu = new PopUpTree(getPropertyFileInfo(parentClass), defaultClassName);
            // don't forget to register as listener
            menu.addPopUpTreeListener(this);

            editObjectCustomizer = new CustomizerPanel(KDDTaskFrame.this, parentClass);
            editObjectCustomizer.addEditObjectChangeListener(this);

            layoutPanel(buttonToolTip, labelToolTip);
        }

        private void layoutPanel(final String buttonToolTip, final String labelToolTip) {

            // gridbaylayout
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // name label
            nameLabel = new JLabel(labelToolTip);
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 10, 10, 10);
            // gbc.anchor = GridBagConstraints.LINE_START;
            add(nameLabel, gbc);

            // select button
            createSelectButton(buttonToolTip);
            gbc.gridx = 2;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.LINE_END;
            add(select, gbc);

            // text field
            createTextField(buttonToolTip);
            gbc.gridy = 2;
            gbc.gridwidth = 4;
            gbc.gridx = 0;
            gbc.insets = new Insets(5, 10, 10, 10);
            gbc.anchor = GridBagConstraints.CENTER;
            add(textField, gbc);

            setBorder(loweredEtched);
            // setBorder(BorderFactory.createCompoundBorder(raisedBevel,
            // loweredBevel));
        }

        private void createSelectButton(String toolTipText) {
            select = new JButton("Select");

            select.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    menu.show(select, nameLabel.getLocation().x, select.getLocation().y);
                }
            });

            select.setEnabled(false);
            select.setToolTipText(toolTipText);
        }

        private void createTextField(final String toolTipText) {

            textField = new JTextField(20) {

                /**
               * 
               */
              private static final long serialVersionUID = 7641527597255200000L;

                public void setText(String text) {
                    if (text != null && !(text.equals(""))) {
                        super.setText(text);
                        this.setToolTipText("Click for details");
                        this.setCaretPosition(0);
                    }
                    else {
                        super.setText("");

                        setToolTipText(toolTipText);
                    }
                }
            };
            textField.setEditable(false);
            textField.setBackground(Color.white);
            textField.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (editObjectCustomizer != null && editObjectCustomizer.getSelectedClass() != null && !textField.getText().equals("")) {
                        editObjectCustomizer.setVisible(true, nameLabel.getLocationOnScreen());
                    }
                }

                public void mousePressed(MouseEvent e) {
                    if (editObjectCustomizer != null && editObjectCustomizer.getSelectedClass() != null && !textField.getText().equals("")) {
                        editObjectCustomizer.setVisible(true, nameLabel.getLocationOnScreen());
                    }
                }
            });
        }

        public void selectedClassChanged(String selectedClass) {
            editObjectCustomizer.setEditObjectClass(selectedClass, nameLabel.getLocationOnScreen());
        }

        public void editObjectChanged(String editObjectName, String parameters) {

            // update textField
            textField.setText(editObjectName);
            textField.setCaretPosition(editObjectName.length());

            // update TaskFrame
//            String[] customizerParameters = this.editObjectCustomizer.parameterValuesToArray();
//			String[]
//			completeParams[0] = "-" + propName;
//			completeParams[1] = editObjectName;
//			System.arraycopy(parameters, 0, completeParams, 2, parameters.length);

            String completeParams = "-" + propName + " " + editObjectName + " " + parameters;
            firePropertyChange(propName, "", completeParams);
        }

        public void setEnabled(boolean e) {
            this.nameLabel.setEnabled(e);
            this.textField.setEnabled(e);
            this.select.setEnabled(e);
        }
        
        public String[] parameterValuesToArray() {
          return editObjectCustomizer.parameterValuesToArray();
        }
    }
}
