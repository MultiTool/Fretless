package fretless;

import static fretless.TunePadLogic.Frequency_To_Octave;
import static fretless.TunePadLogic.Octave_To_Frequency;
import static fretless.TunePadLogic.Slop;
import fretless.TunePadLogic.Transformer;
import static fretless.TunePadLogic.TwoPi;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 @author MultiTool
 */

/* ********************************************************************************************************************************************************* */
public class Wave {
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
  public static class CursorBase//Slicer, playerhead, Cursor, generator? 
  {
    Transformer MyTransformer;// the transform that owns my playable
    Playable MyPlayable;
    Render_Context MyRC;
    CursorBase MyParent;
    public double currentT; // and whatever state info
    /* **************************************************************************** */
    void LoadParentCursor(CursorBase Parent) {// virtual
      //MyPlayable = Playable0;
      MyParent = Parent;
      MyRC = Parent.MyRC;
    }
    /* **************************************************************************** */
    void GetNextChunk(double t1, Result buf) {
    }
  }
  /* **************************************************************************** */
  public interface IPlayable {
    public ArrayList<TunePadLogic.Transformer> Get_Parents();
        /* ************************************************************************************************************************ */
    double Duration_G();
    /* **************************************************************************** */
    double Get_Max_Amplitude();
    /* **************************************************************************** */
    CursorBase Launch_Cursor(Render_Context rc); // from start, t0 not supported
      /* **************************************************************************** */
    CursorBase Launch_Cursor(Render_Context rc, double t0);
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
    double Duration_Val;
    public ArrayList<TunePadLogic.Transformer> Parents;
    public Playable() {
      Parents = new ArrayList<>();
    }
    @Override
    public ArrayList<TunePadLogic.Transformer> Get_Parents() {
      return Parents;
    }
    /* ************************************************************************************************************************ */
    @Override
    public double Duration_G() {
      return this.Duration_Val;
    }
    @Override
    public double Get_Max_Amplitude() {
      return 1.0;
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc) { /* from start, t0 not supported */ return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc, double t0) {
      return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    public class Cursor extends CursorBase // MyCursor
    {
      /* **************************************************************************** */
      public Cursor(Playable Playable0, Render_Context MyRC0) {
        this.MyPlayable = Playable.this;//MyPlayable = Playable0;
        MyRC = MyRC0;
      }
      /* **************************************************************************** */
      @Override
      public void LoadParentCursor(CursorBase Parent) {
        super.LoadParentCursor(Parent);
        this.MyPlayable = Playable.this;
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
    public ArrayList<TunePadLogic.Transformer> Get_Parents() {
      return Parents;
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc) { /* from start, t0 not supported */ return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc, double t0) {
      return new Cursor(this, rc);
    }
    /* **************************************************************************** */
    public class Cursor extends CursorBase // MyCursor
    {
      public double currentT; // and whatever state info
      public Cursor(Group Playable0, Render_Context MyRC0) {
        this.MyPlayable = Group.this;//MyPlayable = Playable0;
        MyRC = MyRC0;
      }
      /* **************************************************************************** */
      @Override
      public void LoadParentCursor(CursorBase Parent) {
        super.LoadParentCursor(Parent);
        // here we want to generate and attach render context
        // so take parent cursor's rc and generate a new global xform from that and local xform
      }
      /* **************************************************************************** */
      @Override
      public void GetNextChunk(double t1, Result buf) {// this?
      }
    }
  }
  /* ************************************************************************************************************************ */
  public static class Note extends Playable {
    public double octave = 10.0, frequency = 0.0;// 440;
    double slope = 0.0, ybase = 0.0;
    double Radius = 5;
    double Diameter = Radius * 2.0;
    public Note() {
      octave = 0.0;
      frequency = 0.0;
    }
    ////#region Playable Members
    /* ************************************************************************************************************************ */
    @Override
    public double Duration_G() {
      return this.Duration_Val;
    }
    @Override
    public double Get_Max_Amplitude() {
      return 1.0;
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc) { /* from start, t0 not supported */
      return new Note.Cursor(this, rc);
    }
    /* **************************************************************************** */
    @Override
    public CursorBase Launch_Cursor(Render_Context rc, double t0) {
      return new Note.Cursor(this, rc);
    }
    /* ************************************************************************************************************************ */
    public class Cursor extends CursorBase // MyCursor
    {
      Note MyNote;
      TunePadLogic.Render_Context MyRc;
      double My_Absolute_Start_Pitch;
      double My_Absolute_Start_Time;
      double Local_Clip_Absolute_Start_Time;
      long Absolute_Sample_Start_Dex;
      /* **************************************************************************** */
      public Cursor(Note Playable0, Render_Context MyRC0) {
        this.MyNote = Note.this;
        MyRC = MyRC0;
      }
      /* **************************************************************************** */
      @Override
      public void LoadParentCursor(CursorBase Parent) {
        super.LoadParentCursor(Parent);
      }
      /* **************************************************************************** */
      @Override
      public void GetNextChunk(double t1, Result buf) {// this?
      }
      /* ************************************************************************************************************************ */
      public void Render_Audio(TunePadLogic.Render_Context rc, TunePadLogic.Wave_Carrier Wave) {
        long samplecnt = 0;
        double Time;
        TunePadLogic.RefDouble amp = new TunePadLogic.RefDouble();
        double Note_Time_Offset;

        double Absolute_Pitch = rc.Absolute_YTranspose + this.MyTransformer.Octave;

        double Note_Absolute_Start_Time = rc.Absolute_Time + MyTransformer.Start_Time_G();
        double Local_Clip_Absolute_Start_Time = Math.max(Note_Absolute_Start_Time, rc.Clip_Time_Start);
        double Local_Clip_Absolute_End_Time = Math.min(Local_Clip_Absolute_Start_Time + MyNote.Duration_G(), rc.Clip_Time_End);
        if (Local_Clip_Absolute_Start_Time >= Local_Clip_Absolute_End_Time) {
          Wave.WaveForm = new double[0];
          Wave.Duration = 0;
          return;
        }/* zero-length */
        Wave.Start_Time = Local_Clip_Absolute_Start_Time;

        long Absolute_Sample_Start = (long) Math.ceil(Local_Clip_Absolute_Start_Time * (double) rc.Sample_Rate);// index of first sample of this note.
        long Absolute_Sample_End = (long) Math.floor(Local_Clip_Absolute_End_Time * (double) rc.Sample_Rate);// index of last sample of this note.
        Wave.Start_Index = Absolute_Sample_Start;

        long Num_Samples = Absolute_Sample_End - Absolute_Sample_Start;
        Wave.WaveForm = new double[(int) (Num_Samples + Slop)];

        double Absolute_Frequency = Octave_To_Frequency(Absolute_Pitch);

        /* Note_Time_Offset is the difference between the beginning of this note and the place we start rendering, defined by clip start time. */
        Note_Time_Offset = (Absolute_Sample_Start * rc.Sample_Interval) - Note_Absolute_Start_Time;// this time is right.  first it is aligned to T0 (origin of the universe), then cropped to local note coords
        for (samplecnt = 0; samplecnt < Num_Samples; samplecnt++) {
          Time = Note_Time_Offset + (rc.Sample_Interval * (double) samplecnt);
          this.Play_Me_Local_Time(Time, Absolute_Frequency, amp);
          Wave.WaveForm[(int) samplecnt] = amp.num;
        }
      }
      /* ************************************************************************************************************************ */
      public void Render_Audio_Start(TunePadLogic.Render_Context rc) {// stateful rendering
        MyRc = rc;
        My_Absolute_Start_Pitch = MyRc.Absolute_YTranspose + this.MyTransformer.Octave;
        My_Absolute_Start_Time = MyRc.Absolute_Time + this.MyTransformer.Start_Time_G();
        Local_Clip_Absolute_Start_Time = Math.max(My_Absolute_Start_Time, MyRc.Clip_Time_Start);
        Absolute_Sample_Start_Dex = (long) Math.ceil(Local_Clip_Absolute_Start_Time * (double) MyRc.Sample_Rate);// index of first sample of this note.
      }
      /* ************************************************************************************************************************ */
      public void Render_Audio_To(double Hasta, TunePadLogic.Wave_Carrier Wave) {
        long samplecnt = 0;
        double Time;
        TunePadLogic.RefDouble amp = new TunePadLogic.RefDouble();
        double Note_Time_Offset;
        double Local_Clip_Absolute_End_Time = Math.min(Local_Clip_Absolute_Start_Time + this.MyNote.Duration_G(), MyRc.Clip_Time_End);
        if (Local_Clip_Absolute_Start_Time >= Local_Clip_Absolute_End_Time) {
          Wave.WaveForm = new double[0];
          Wave.Duration = 0;
          return;
        }/* zero-length */
        Wave.Start_Time = Local_Clip_Absolute_Start_Time;

        long Absolute_Sample_End = (long) Math.floor(Local_Clip_Absolute_End_Time * (double) MyRc.Sample_Rate);// index of last sample of this note.
        Wave.Start_Index = Absolute_Sample_Start_Dex;

        long Num_Samples = Absolute_Sample_End - Absolute_Sample_Start_Dex;
        Wave.WaveForm = new double[(int) (Num_Samples + Slop)];

        double Absolute_Frequency = Octave_To_Frequency(My_Absolute_Start_Pitch);

        /* Note_Time_Offset is the difference between the beginning of this note and the place we start rendering, defined by clip start time. */
        Note_Time_Offset = (Absolute_Sample_Start_Dex * MyRc.Sample_Interval) - My_Absolute_Start_Time;// this time is right.  first it is aligned to T0 (origin of the universe), then cropped to local note coords
        for (samplecnt = 0; samplecnt < Num_Samples; samplecnt++) {
          Time = Note_Time_Offset + (MyRc.Sample_Interval * (double) samplecnt);
          this.Play_Me_Local_Time(Time, Absolute_Frequency, amp);
          Wave.WaveForm[(int) samplecnt] = amp.num;
        }
      }
    }// class Cursor
    /* ************************************************************************************************************************ */
    private double Play_Me_Local_Time(double time0, double Base_Frequency, TunePadLogic.RefDouble amp) {// assumes we are passed time in the note's own local coordinates.
      double percenttime = time0 / this.Duration_G();
      double Loudness = ((1.0 - percenttime) * Loudness_G(0.0)) + (percenttime * Loudness_G(1.0));
      amp.num = Math.sin(time0 * (Base_Frequency) * TwoPi) * Loudness;
      //double cycles = Frequency_Integral_Bent_Octave(slope, ybase, time0);
      return amp.num;
    }
    /* #endregion */
    /* ************************************************************************************************************************ */
    public void Change_Time(double Time) {
      this.Start_Time_S(Time);
    }
    /* Drawable interface */
    /* ************************************************************************************************************************ */
    @Override
    public ArrayList<TunePadLogic.Drawable> Get_My_Children() {/* Drawable */
      return null;
    }
    /* ************************************************************************************************************************ */
    @Override
    public TunePadLogic.Playable_Drawable Xerox_Me() {
      TunePadLogic.Note child = null;
      try {
        child = (TunePadLogic.Note) this.clone();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
    }
    /* ************************************************************************************************************************ */
    @Override
    public String Name_G() {
      return MyName;
    }
    /* ************************************************************************************************************************ */
    @Override
    public String Name_S(String value) {
      return MyName = value;
    }
  };// class Note
}
