package de.lmu.ifi.dbs.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// todo steffi comment all
public class KDDStartFrame extends JFrame {

    private JPanel base;

    private GridBagConstraints constraints;

    public KDDStartFrame() {
        setTitle("KDD Workbench");
        base = new JPanel(new GridBagLayout());
        getContentPane().add(base);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 5, 10, 5);

        // new task
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
//		constraints.anchor = GridBagConstraints.LINE_START;
        base.add(createNewTask(), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
//		constraints.fill = GridBagConstraints.HORIZONTAL;
        base.add(new JPanel(), constraints);

        // load task
        constraints.gridwidth = 1;
        constraints.gridy = 1;
        constraints.gridx = 0;
        base.add(createLoadButton(), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        base.add(new JPanel(), constraints);

        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        base.add(new JPanel(), constraints);

//		 if the window is closed exit the program
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(new Dimension(300, 200));

        setVisible(true);

    }

    private JComponent createNewTask() {
        JPanel newTask = new JPanel();


        JButton newButton = new JButton("New Task");
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
//				KDDGui workbench = new KDDGui();
                JFrame frame = new KDDTaskFrame();
            }
        });

        return newButton;
//		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
//		newTask.add(newButton);
//		return newTask;
    }


    private JComponent createLoadButton() {
        JPanel panel = new JPanel();

        JButton load = new JButton("Load Task");
        load.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

            }
        });
        panel.add(load);
        return panel;
    }

    public Dimension getMinimumSize() {
        return new Dimension(500, 200);
    }


    public static void main(String[] args) {
        new KDDStartFrame();
    }
}
