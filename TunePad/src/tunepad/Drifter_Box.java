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
public class Drifter_Box implements TunePadLogic.Playable_Drawable {/* Drifter_Box exists to provide a local offset origin to its contents. */

  public TunePadLogic.Playable_Drawable Content;
  double octave, frequency;
  public String MyName;
  /* ************************************************************************************************************************ */
  public void Set_Octave(double Fresh_Octave) {
    this.octave = Fresh_Octave;
    this.frequency = TunePadLogic.Octave_To_Frequency(Fresh_Octave);
  }
  public void Set_Frequency(double Fresh_Frequency) {
    this.frequency = Fresh_Frequency;
    this.octave = TunePadLogic.Frequency_To_Octave(Fresh_Frequency);
  }
  /* ************************************************************************************************************************ */
  double M_Start_Time = 0;
  @Override
  public double Start_Time_G() {
    return M_Start_Time;
  }
  @Override
  public void Start_Time_S(double val) {
    M_Start_Time = val;
  }
  /* ************************************************************************************************************************ */
  double End_Time_Val;
  @Override
  public double End_Time_G() {
    return End_Time_Val;
  }
  /* ************************************************************************************************************************ */
  public void End_Time_S(double value) {
    End_Time_Val = value;
  }
  /* ************************************************************************************************************************ */
  @Override
  public double Duration_G() {
    return this.Content.Duration_G();
  }
  @Override
  public void Duration_S(double value) {
    this.Content.Duration_S(value);
  }
  @Override
  public double Get_Pitch() {
    return octave;
  }
  public double Get_Max_Amplitude() {
    Boolean snargle = true;
    return 1.0;
  }
  public double Loudness_G(double percent) {
    return 1.0;
  }
  public double Loudness_S(double percent, double value) {
    return 1.0;
  }
  public void Render_Audio(TunePadLogic.Render_Context rc, TunePadLogic.Wave_Carrier Wave) {
    this.Content.Render_Audio(rc, Wave);
  }
  public void Render_Audio_Start(TunePadLogic.Render_Context rc) {
    this.Content.Render_Audio_Start(rc);
  }
  public void Render_Audio_To(double Hasta, TunePadLogic.Wave_Carrier Wave) {
    this.Content.Render_Audio_To(Hasta, Wave);
  }
  public Boolean Hit_Test_Stack(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
    /* Drifter_Box  */
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    Boolean found = this.Content.Hit_Test_Stack(mydc, Xloc, Yloc, Depth + 1, Stack);
    if (found) {
      Stack.Set(Depth, this);
    }
    return found;
  }
  /* ************************************************************************************************************************ */
  public Boolean Hit_Test_Container(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
    /* Drifter_Box  */
    if (this == Stack.Exclude) {/* look for any contact with the container hits of any children I might own. */
      return false;
    }
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Boolean found = this.Content.Hit_Test_Container(mydc, Xloc, Yloc, Depth + 1, Stack);
    if (found) {
      Stack.Set(Depth, this);
    }
    return found;
  }
  /* ************************************************************************************************************************ */
  public ArrayList< TunePadLogic.Drawable> Get_My_Children() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  /* ************************************************************************************************************************ */
  public void Draw_Me(TunePadLogic.Drawing_Context dc) {
    /* Drifter_Box  */
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    dc.gr.setColor(Color.green);
    Content.Draw_Me(mydc);
  }
  /* ************************************************************************************************************************ */
  public Drifter_Box Xerox_Me_Typed() {
    Drifter_Box child = null;
    try {
      child = (Drifter_Box) this.clone();
      child.Content = this.Content.Xerox_Me();
    } catch (CloneNotSupportedException ex) {
      Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
    }
    return child;
  }
  /* ************************************************************************************************************************ */
  public TunePadLogic.Playable_Drawable Xerox_Me() {
    return Xerox_Me_Typed();
  }
  /* ************************************************************************************************************************ */
  public String Name_G() {
    return MyName;
  }
  /* ************************************************************************************************************************ */
  public String Name_S(String value) {
    return MyName = value;
  }
}
