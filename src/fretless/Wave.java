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
  /* ************************************************************************************************************************ */
  public static class Transformer {// implements ITransformer{
    /*
     possible transforms are:
     xy transpose: start time, octave
     time (X) scale: duration factor - NOT duration, but percentage scale.
     initial loudness (W) scale: loudness factor - NOT loudness, but coefficient. (how to display this if coef is >1? 
     variable loudness scale could only work if we have loudness envelopes that trickle down.
     
     can we also use shear transforms to bend whole bunches of children?  Start_Octave, End_Octave. 
    
     a note would be a series of control xyz(pitch,time,loud) points that a continuous note morphs to.
     that is an array of control points. shear is generated. 
     so: note.addpoint(xyz), note.setpoint(dex?,xyz); list is automatically sorted by Time.
     in the beginning, this cannot be an envelope, so it is always a leaf node. if it WERE an envelope, it must pass through transformer. a tensor? 
     how does this fit with click and drag? 
     playable origins can be cut/copied and relocated.
     inner stuff like vol envelopes and pitch changes, only dragged around. no topo change. 
     so is initial pitch T=0, Y=0 always? or are they offset from 0,0?  ideally offset. 
     transform keeps location relative to parent, note parts keep location relative to transform.
    
     basic non-bendy note is always 0,0 to transform. playables have a hit test, but all of that logic is internal. 
    
     bendy note can have an offset origin just for editing, but that creates conflict between drag-all and drag-first cpoint.
     so bendy notes always draw a 00 origin point. 
     don't the xformers draw their own click/drag point? yes. 
     you can drag an xform, but you if you cut/copy it, you only cut/copy the note under it. xforms are unique relationships.
     for now every xform has the same clickable shape. 
    
     */
    public double Octave = 0.0;
    public double Start_Time = 0.0;
    public double Time_Scale = 1.0;
    public double Loudness_Scale = 1.0;
    public Playable MyParent = null;// container, group?
    public Playable MyPlayable = null;
    public double Octave_G() {// getset
      return Octave;
    }
    public void Octave_S(double Fresh_Octave) {
      Octave = Fresh_Octave;
    }
    public double Start_Time_G() {
      return Start_Time;
    }
    public void Start_Time_S(double val) {// getset
      Start_Time = val;
    }
    public double Time_Scale_G() {// getset
      return Time_Scale;
    }
    public void Time_Scale_S(double value) {// getset
      Time_Scale = value;
    }
    public double Loudness_Scale_G() {
      return Loudness_Scale;
    }
    public double Loudness_Scale_S(double value) {
      return Loudness_Scale = value;
    }
    public void Add_Transpose(double Time_Offset, double Pitch_Offset) {
      this.Start_Time += Time_Offset;
      this.Octave += Pitch_Offset;
    }
    public void Add_Transpose(Wave.Transformer Child_Frame) {
      this.Start_Time += Child_Frame.Start_Time_G();
      this.Octave += Child_Frame.Octave_G();
    }
    public Boolean Hit_Test_Stack(Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
      return false;
    }// gets the stack from me to the grandchild you hit.  ideally you'd load it on the way back out, but that'd load in reverse yes?
    public Boolean Hit_Test_Container(Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
      return false;
    }
  };
  /* **************************************************************************** */
  public final static class Render_Context {
    // will either own or inherit from xformer
    //public double Absolute_Time, Absolute_YTranspose;
    public Transformer Absolute_XForm;
    public double Clip_Time_Start, Clip_Time_End;// absolute coords
    public int Sample_Rate;
    public double Sample_Interval;
    public long Sample_Clip_Start, Sample_Clip_End;// start and ending sample indexes, based at index 0 == beginning of universe.  are they clip limits or parent coordinates?
    public Render_Context() {
      Absolute_XForm = new Transformer();
      //Wave = new Wave_Carrier();
      Sample_Rate = 44100;
      Sample_Interval = 1.0 / (double) Sample_Rate;
    }
    public Render_Context(Wave.Render_Context ParentRC) {// pass the torch of context coordinates
      this();
      this.Absolute_XForm.Octave = ParentRC.Absolute_XForm.Octave;
      this.Absolute_XForm.Start_Time = ParentRC.Absolute_XForm.Start_Time;
      this.Sample_Rate = ParentRC.Sample_Rate;
      this.Sample_Interval = ParentRC.Sample_Interval;
      this.Clip_Time_Start = ParentRC.Clip_Time_Start;
      this.Clip_Time_End = ParentRC.Clip_Time_End;
      this.Sample_Clip_Start = ParentRC.Sample_Clip_Start;
      this.Sample_Clip_End = ParentRC.Sample_Clip_End;
    }
    public Render_Context(Wave.Render_Context Parent, Wave.Transformer Child_Frame) {// pass the torch of context coordinates
      this(Parent);
      this.Add_Transpose(Child_Frame);
    }
    public void Add_Transpose(double Time_Offset, double Pitch_Offset) {
      Absolute_XForm.Add_Transpose(Time_Offset, Pitch_Offset);
    }
    public void Add_Transpose(Wave.Transformer Child_Frame) {
      Absolute_XForm.Add_Transpose(Child_Frame);
    }
  }
  /* **************************************************************************** */
  public final static class Drawing_Context {
    public Transformer Absolute_XForm;
    public Drawing_Context() {
      Absolute_XForm = new Transformer();
    }
    public Drawing_Context(Wave.Drawing_Context ParentDC) {// pass the torch of context coordinates
      this();
      this.Absolute_XForm.Octave = ParentDC.Absolute_XForm.Octave;
      this.Absolute_XForm.Start_Time = ParentDC.Absolute_XForm.Start_Time;
    }
    public Drawing_Context(Wave.Drawing_Context ParentDC, Wave.Transformer XForm) {// pass the torch of context coordinates
      this(ParentDC);
      this.Add_Transpose(XForm);
    }
    public void Add_Transpose(double Time_Offset, double Pitch_Offset) {
      this.Absolute_XForm.Add_Transpose(Time_Offset, Pitch_Offset);
    }
    public void Add_Transpose(Wave.Transformer Child_Frame) {
      this.Absolute_XForm.Add_Transpose(Child_Frame);
    }
    public Point2D.Double To_Screen(double xraw, double yraw) {
      Point2D.Double loc = new Point2D.Double();
      loc.setLocation((xraw * Absolute_XForm.Time_Scale) + Absolute_XForm.Start_Time, (yraw * Scale_Y) + Trans_Y);
      //loc.setLocation((xraw * Scale_X) + Absolute_X, (yraw * Scale_Y) + Absolute_Y);
      return loc;
    }
    public Point2D.Double From_Screen(double xscreen, double yscreen) {
      Point2D.Double loc = new Point2D.Double();
      loc.setLocation(((xscreen - Absolute_XForm.Time_Scale) / Absolute_XForm.Start_Time), ((yscreen - Trans_Y) / Scale_Y));
      return loc;
    }
  }
  /* **************************************************************************** */
  public final static class Result {
    public double[] buffer;
  }
  /* **************************************************************************** */
  public final static class Control_Point {// 
    public double Octave, Time, Loudness;
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
    void GetNextChunk(double TNext, TunePadLogic.Wave_Carrier buf) {
    }
  }
  /* **************************************************************************** */
  public interface IPlayable {
    public ArrayList<Transformer> Get_Parents();
    /* ************************************************************************************************************************ */
//    void Start_Time_S(double val);
//    double Start_Time_G();
//    void Loudness_S(double DeltaT, double val);
//    double Loudness_G(double DeltaT);
    double Duration_G();
    /* **************************************************************************** */
    double Get_Max_Amplitude();
    /* **************************************************************************** */
    CursorBase Launch_Cursor(Render_Context rc); // from start, t0 not supported
    /* **************************************************************************** */
    CursorBase Launch_Cursor(Render_Context rc, double t0);
    /* **************************************************************************** */
    Playable Xerox_Me();
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
    //double Start_Time_Val, Loudness_Val;
    public ArrayList<Transformer> Parents;
    public Playable() {
      Parents = new ArrayList<>();
    }
    @Override
    public ArrayList<Transformer> Get_Parents() {
      return Parents;
    }
    /* ************************************************************************************************************************ */
//    @Override
//    public void Start_Time_S(double val) {
//      Start_Time_Val = val;
//    }
//    @Override
//    public double Start_Time_G() {
//      return Start_Time_Val;
//    }
//    @Override
//    public void Loudness_S(double DeltaT, double val) {
//      Loudness_Val = val;
//    }
//    @Override
//    public double Loudness_G(double DeltaT) {
//      return Loudness_Val;
//    }
    @Override
    public double Duration_G() {
      return this.Duration_Val;
    }
    @Override
    public double Get_Max_Amplitude() {
      return 1.0;
    }
    /* ************************************************************************************************************************ */
    @Override
    public Playable Xerox_Me() {
      Playable child = null;
      try {
        child = (Playable) this.clone();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
    }
    // virtual
    public Boolean Hit_Test_Stack(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
      return false;
    }
    // virtual
    public Boolean Hit_Test_Container(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
      return false;
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
      public void GetNextChunk(double TNext, TunePadLogic.Wave_Carrier buf) {// this?
      }
    }
  }
  //#endregion real use

  /* **************************************************************************** */
  public static class Group extends Playable {
    public ArrayList<Transformer> Children;
    public Group() {
      Children = new ArrayList<>();
    }
    /* ************************************************************************************************************************ */
    public ArrayList<Transformer> Get_My_Children() {/* Drawable */
      return Children;
    }
    /* ************************************************************************************************************************ */
    public void Add_Child(Playable child, double Time, double Pitch) {
      Transformer trans = new Transformer();
      trans.Start_Time_S(Time);
      trans.Octave_S(Pitch);
      trans.MyParent = this;
      trans.MyPlayable = child;
      this.Children.add(trans);
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
      public void GetNextChunk(double TNext, TunePadLogic.Wave_Carrier buf) {// this?
      }
    }
  }
  /* ************************************************************************************************************************ */
  public static class Note extends Playable {
    public double octave = 10.0, frequency = 0.0;// 440;
    double slope = 0.0, ybase = 0.0;
    public ArrayList<Control_Point> CPoints;
    double Radius = 5;
    double Diameter = Radius * 2.0;
    public Note() {
      octave = 0.0;
      frequency = 0.0;
      CPoints = new ArrayList<>();
      Control_Point cp0 = new Control_Point();
      {
        cp0.Octave = 0;
        cp0.Time = 0;
        cp0.Loudness = 1.0;
      }
      Control_Point cp1 = new Control_Point();
      {
        cp1.Octave = 0;
        cp1.Time = 1.0;
        cp1.Loudness = 0.0;
      }
      CPoints.add(cp0);
      CPoints.add(cp1);
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
      /* ************************************************************************************************************************ */
      public void Render_Audio_Start(TunePadLogic.Render_Context rc) {// stateful rendering
        MyRc = rc;
        My_Absolute_Start_Pitch = MyRc.Absolute_YTranspose + this.MyTransformer.Octave;
        My_Absolute_Start_Time = MyRc.Absolute_Time + this.MyTransformer.Start_Time_G();
        Local_Clip_Absolute_Start_Time = Math.max(My_Absolute_Start_Time, MyRc.Clip_Time_Start);
        Absolute_Sample_Start_Dex = (long) Math.ceil(Local_Clip_Absolute_Start_Time * (double) MyRc.Sample_Rate);// index of first sample of this note.
      }
      /* **************************************************************************** */
      @Override
      public void GetNextChunk(double TNext, TunePadLogic.Wave_Carrier buf) {
        Render_Audio_To(TNext, buf);
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
          this.MyNote.Play_Me_Local_Time(Time, Absolute_Frequency, amp);
          Wave.WaveForm[(int) samplecnt] = amp.num;
        }
      }
    }// class Cursor
    /* ************************************************************************************************************************ */
    private double Play_Me_Local_Time(double time0, double Base_Frequency, TunePadLogic.RefDouble amp) {// assumes we are passed time in the note's own local coordinates.
      double percenttime = time0 / this.Duration_G();
      double Loudness = ((1.0 - percenttime) * this.CPoints.get(0).Loudness) + (percenttime * this.CPoints.get(1).Loudness);
      amp.num = Math.sin(time0 * (Base_Frequency) * TwoPi) * Loudness;
      return amp.num;
    }
    /* #endregion */
    /* Drawable interface */
    /* ************************************************************************************************************************ */
    @Override
    public Playable Xerox_Me() {
      Playable child = null;
      try {
        child = (Note) this.clone();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
    }
  };// class Note
  /* ************************************************************************************************************************ */
  public static class Hit_Stack {
    public int Stack_Depth;
    Transformer[] Path;
    public void Init(int Depth) {
      Stack_Depth = Depth;
      Path = new Transformer[Depth];
    }
    public void Set(int Depth, Transformer Note) {
      Path[Depth] = Note;
    }
    public void Render_Audio(TunePadLogic.Wave_Carrier wave) {
      if (Stack_Depth <= 0) {
        wave.WaveForm = new double[1];
        return;
      }
      Wave.Render_Context rc = new Wave.Render_Context();
      rc.Clip_Time_End = Double.MAX_VALUE;
      int Last_Depth = Stack_Depth - 1;
      Create_Audio_Transform(rc);
      Transformer leaf = this.Path[Last_Depth];
      CursorBase cb = leaf.MyPlayable.Launch_Cursor(rc, rc.Clip_Time_Start);
      cb.GetNextChunk(rc.Clip_Time_End, wave);
    }
    public Wave.Render_Context Create_Audio_Transform(Wave.Render_Context rc) {
      int Last_Depth = Stack_Depth - 1;
      for (int depth = 0; depth < Last_Depth; depth++) {
        Transformer pb = this.Path[depth];
        rc.Add_Transpose(pb);
      }
      return rc;
    }
    public Wave.Drawing_Context Create_Drawing_Transform(Wave.Drawing_Context dc) {
      int Last_Depth = Stack_Depth - 0;
      for (int depth = 0; depth < Last_Depth; depth++) {
        Transformer pb = this.Path[depth];
        dc.Add_Transpose(pb);
      }
      return dc;
    }
    public Transformer End() {
      int Last_Depth = Stack_Depth - 1;
      return this.Path[Last_Depth];
    }
    public void Clear() {
      Stack_Depth = 0;
      Path = null;
    }
    public TunePadLogic.Playable Container;
    public Transformer Find_Up(Transformer Target) {
      Transformer retval = null;
      int Last_Depth = Stack_Depth - 1;
      for (int depth = Last_Depth; depth >= 0; depth--) {
        Transformer Nivel = this.Path[depth];
        if (Nivel == Target) {
          retval = Nivel;
          break;
        }
      }
      return retval;
    }
  }
}
