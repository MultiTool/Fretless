package fretless;

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

/**

 @author MultiTool
 */

/* ********************************************************************************************************************************************************* */
class Wave {
  /* **************************************************************************** */
  public final static class Render_Context {
  }
  /* **************************************************************************** */
  public final static class Drawing_Context {
  }
  /* **************************************************************************** */
  public final static class Result {
    public double[] buffer;
  }
  /* **************************************************************************** */
  public interface ICursor//Slicer, playerhead, Cursor, generator? 
  {
    /* **************************************************************************** */
    void GetNextChunk(double t1, Result buf);
  }
  /* **************************************************************************** */
  public interface IPlayable {
    public ArrayList<TunePadLogic.ITransformer> Get_Parents();
    /* **************************************************************************** */
    ICursor Launch_Cursor(Render_Context rc); // from start, t0 not supported
      /* **************************************************************************** */
    ICursor Launch_Cursor(Render_Context rc, double t0);
  }
  /* **************************************************************************** */
  public interface IDrawable {
    /* **************************************************************************** */
    void Draw_Me(Drawing_Context dc);
  }
  // #region real use
    /* **************************************************************************** */
  public static class Playable implements IPlayable {
    public String MyName;
    public ArrayList<TunePadLogic.ITransformer> Parents;
    public Playable() {
      Parents = new ArrayList<>();
    }
    @Override
    public ArrayList<TunePadLogic.ITransformer> Get_Parents() {
      return Parents;
    }
    /* **************************************************************************** */
    @Override
    public ICursor Launch_Cursor(Render_Context rc) { /* from start, t0 not supported */ return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    @Override
    public ICursor Launch_Cursor(Render_Context rc, double t0) {
      return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    public static class Cursor implements ICursor // MyCursor
    {
      Playable MyPlayable;
      Render_Context MyRC;
      public double currentT; // and whatever state info
      public Cursor(Playable Playable0, Render_Context MyRC0) {
        MyPlayable = Playable0;
        MyRC = MyRC0;
      }
      /* **************************************************************************** */
      @Override
      public void GetNextChunk(double t1, Result buf) {// this?
      }
    }
  }
  //#endregion real use

  /* **************************************************************************** */
  public static class Group extends Playable {
    @Override
    public ArrayList<TunePadLogic.ITransformer> Get_Parents() {
      return Parents;
    }
    /* **************************************************************************** */
    @Override
    public ICursor Launch_Cursor(Render_Context rc) { /* from start, t0 not supported */ return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    @Override
    public ICursor Launch_Cursor(Render_Context rc, double t0) {
      return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    public static class Cursor implements ICursor // MyCursor
    {
      Group MyPlayable;
      Render_Context MyRC;
      public double currentT; // and whatever state info
      public Cursor(Group Playable0, Render_Context MyRC0) {
        MyPlayable = Playable0;
        MyRC = MyRC0;
      }
      /* **************************************************************************** */
      @Override
      public void GetNextChunk(double t1, Result buf) {// this?
      }
    }
  }
}
