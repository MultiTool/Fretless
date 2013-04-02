/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tunepad;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;

import java.awt.Color;
import java.util.*;
import java.util.ArrayList;

import java.io.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
/* ************************************************************************************************************************ */

public class Invisible_Playable extends TunePadLogic.Dummy_Playable {
  public ArrayList<TunePadLogic.Playable_Drawable> Children;
  /* ************************************************************************************************************************ */
  public Invisible_Playable() {
  }
  double Radius = 8;
  double Diameter = Radius * 2.0;
  /* ************************************************************************************************************************ */
  @Override
  public Boolean Hit_Test_Stack(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
    /* Invisible_Playable  */
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.Children.size(); cnt++) {
      TunePadLogic.Playable Child = this.Children.get(cnt);
      if (Child.Hit_Test_Stack(mydc, Xloc, Yloc, Child_Depth, Stack)) {
        found = true;
        break;
      }
    }
    if (found) {/* child hit preempts hitting me */
      Stack.Set(Depth, this);
    } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
      Stack.Init(Depth + 1);
      Stack.Set(Depth, this);
      //this.Diameter = 2.0 * (this.Radius = 10.0);
      found = true;
    }
    return found;
  }
  /* ************************************************************************************************************************ */
  @Override
  public Boolean Hit_Test_Container(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
    /* Invisible_Playable  */
    if (this == Stack.Exclude) {
      return false;
    }
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.Children.size(); cnt++) {
      TunePadLogic.Playable Child = this.Children.get(cnt);
      if (Child.Hit_Test_Container(mydc, Xloc, Yloc, Child_Depth, Stack)) {
        found = true;
        break;
      }
    }
    if (found) {/* child hit preempts hitting me */
      Stack.Set(Depth, this);
    } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
      Stack.Init(Depth + 1);
      Stack.Set(Depth, this);
      found = true;
    }
    return found;
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Set_Octave(double Fresh_Octave) {
    Boolean snargle = true;
  }
  @Override
  public void Set_Frequency(double Fresh_Frequency) {
    Boolean snargle = true;
  }
  /* ************************************************************************************************************************ */
  double M_Start_Time = 0;
  @Override
  public/* virtual */ double Start_Time_G() {
    return M_Start_Time;
  }
  @Override
  public void Start_Time_S(double val) {
    M_Start_Time = val;
  }
  /* ************************************************************************************************************************ */
  @Override
  public double End_Time_G() {// getset
    return Double.MIN_VALUE;
  }
  /* ************************************************************************************************************************ */
  @Override
  public double Duration_G() {
    return Double.MIN_VALUE;
  }
  @Override
  public void Duration_S(double value) {
  }
  @Override
  public double Get_Pitch() {
    return Double.MIN_VALUE;// snargle
  }
  @Override
  public double Get_Max_Amplitude() {
    return Double.MIN_VALUE;// snargle
  }
  /* ************************************************************************************************************************ */
  @Override
  public double Loudness_G(double percent) {
    return Double.MIN_VALUE;
  }
  @Override
  public double Loudness_S(double percent, double value) {
    return Double.MIN_VALUE;
  }
  @Override
  public void Render_Audio(TunePadLogic.Render_Context rc, TunePadLogic.Wave_Carrier Wave) {
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Render_Audio_Start(TunePadLogic.Render_Context rc) {// stateful rendering
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Render_Audio_To(double Hasta, TunePadLogic.Wave_Carrier Wave) {
  }
  // Drawable interface
    /* ************************************************************************************************************************ */
  @Override
  public ArrayList<TunePadLogic.Drawable> Get_My_Children() {// Drawable
    return null;
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Draw_Me(TunePadLogic.Drawing_Context dc) {// Drawable
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    AntiAlias(mydc.gr);
    mydc.gr.setColor(Color.black);
    mydc.gr.fillOval((int) (pnt.x) - (int) Radius, (int) (pnt.y) - (int) Radius, (int) Diameter, (int) Diameter);
    for (int cnt = 0; cnt < this.Children.size(); cnt++) {
      TunePadLogic.Playable_Drawable child = this.Children.get(cnt);
      child.Draw_Me(mydc);
    }
  }
  /* ************************************************************************************************************************ */
  public void AntiAlias(Graphics2D g2d) {//http://www.exampledepot.com/egs/java.awt/AntiAlias.html?l=rel

    // projects\TunePad\dist>tunepad.jar -Dsun.java2d.opengl=true

    // Determine if antialiasing is enabled
    {
      RenderingHints rhints = g2d.getRenderingHints();
      Boolean antialiasOn = rhints.containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
    }
    // Enable antialiasing for shapes
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Disable antialiasing for shapes
    //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    // Draw shapes...; see Drawing Simple Shapes

    // Enable antialiasing for text
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Draw text...; see Drawing Simple Text

    // Disable antialiasing for text
    //g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
  }
  /* ************************************************************************************************************************ */
  public Invisible_Playable Xerox_Me_Typed() {
    Invisible_Playable child = null;
    try {
      child = (Invisible_Playable) this.clone();
      child.Children.clear();
      for (int cnt = 0; cnt < this.Children.size(); cnt++) {
        child.Children.add(this.Children.get(cnt).Xerox_Me());
      }
    } catch (CloneNotSupportedException ex) {
      Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
    }
    return child;
  }
  /* ************************************************************************************************************************ */
  public TunePadLogic.Playable_Drawable Xerox_Me() {
    return Xerox_Me_Typed();
  }
}
