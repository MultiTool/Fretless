/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fretless;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

/**

 @author bczinrw
 */
public class Fretless {
  /**
   @param args the command line arguments
   */
//  public static void main(String[] args) {
//    // TODO code application logic here
//  }
  /* *************************************************************************************************** */
  public static void main(String[] args) {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        createAndShowGUI();
      }
    });
  }
  /* *************************************************************************************************** */
  private static void createAndShowGUI() {
    // http://docs.oracle.com/javase/tutorial/uiswing/examples/start/HelloWorldSwingProject/src/start/HelloWorldSwing.java
    //Create and set up the window.
    JFrame frame = new JFrame("Fretless");
    // capture keyboard events 
    frame.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.isControlDown()) {
          char ch = Character.toLowerCase(e.getKeyChar());
          if (ch == 'q') {
            System.out.println("Quit!");
          }
        }
      }
      @Override
      public void keyReleased(KeyEvent e) {
      }
      @Override
      public void keyTyped(KeyEvent e) {
        if (e.isControlDown()) {
          char ch = Character.toLowerCase(e.getKeyChar());
          if (ch == 'q') {
            System.out.println("Quit!");
          }
        }
      }
    });
    
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel MainPanel = new JPanel();
    frame.getContentPane().add(MainPanel);

    //Display the window.
    frame.pack();
    frame.setVisible(true);

    MainPanel.setBackground(Color.CYAN);

    if (true) {// http://zetcode.com/tutorials/javaswingtutorial/basicswingcomponents/
      JPanel panel = MainPanel;// new JPanel();
      panel.setLayout(new BorderLayout(10, 10));

      Drawing_Canvas dc = new Drawing_Canvas();

      dc.setBackground(Color.red);
      dc.setSize(700, 700);
      panel.add(dc, BorderLayout.CENTER);

      //panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

//    KeyEventDispatcher myKeyEventDispatcher = new DefaultFocusManager();
//    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(myKeyEventDispatcher);
    // KeyboarFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(myKeyEventDispatcher);

    frame.setSize(700, 550);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);

  }
}
