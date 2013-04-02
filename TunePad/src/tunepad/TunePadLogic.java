
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

/**
 *
 * @author JCAtkeson
 */

/* ********************************************************************************************************************************************************* */
public class TunePadLogic {
  public static String audiopath = "";
  public static double xscale = 300.0;
  public static double yscale = 50.0;
  /* ********************************************************************************************************************************************************* */
  public Root_Playable rootplay;
  public TunePadLogic() {
    this.Compose_Vine();
  }
  /* ********************************************************************************************************************************************************* */
  public static class WaveInt {
    public long numsamples;
    public int[] wave;
  };
  public static class WaveDouble {
    public long numsamples;
    public double[] wave;
    public void Init(int SizeInit) {
      wave = new double[SizeInit];
    }
  };
  public static class RefLong {
    public long num;
  };
  public static class RefInt {
    public long num;
  };
  public static class RefDouble {
    public double num;
  };
  /* ********************************************************************************************************************************************************* */
  public static double TwoPi = Math.PI * 2.0;
  /* ********************************************************************************************************************************************************* */
  static int Slop = 0;// 0;// 1000;
  public static void Delete(Object obj) {// for eventually porting to C++
  }
  /* ********************************************************************************************************************************************************* */
  public static class Wave_Carrier {
    public double Start_Time, Duration;
    public long Start_Index;// absolute sample index from beginning of universe, Time 0.
    public double[] WaveForm = null;
    public void Clear() {
      WaveForm = null;
    }
  }
  /* ********************************************************************************************************************************************************* */
  public static class Render_Context {// here we might put absolute coordinates as passed down from the root.
    public double TimeShift, YTranspose;
    public double Absolute_Time, Absolute_YTranspose;
    public double Clip_Time_Start, Clip_Time_End;// absolute coords
    public int Sample_Rate;
    public double Sample_Interval;
    public long Sample_Clip_Start, Sample_Clip_End;// start and ending sample indexes, based at index 0 == beginning of universe.  are they clip limits or parent coordinates?
    //public Wave_Carrier Wave;
    public Render_Context() {
      //Wave = new Wave_Carrier();
      Sample_Rate = 44100;
      Sample_Interval = 1.0 / (double) Sample_Rate;
    }
    public Render_Context(Render_Context Parent) {// pass the torch of context coordinates
      this();
      this.Absolute_Time = Parent.Absolute_Time;
      this.Absolute_YTranspose = Parent.Absolute_YTranspose;
      this.Sample_Rate = Parent.Sample_Rate;
      this.Sample_Interval = Parent.Sample_Interval;
      this.Clip_Time_Start = Parent.Clip_Time_Start;
      this.Clip_Time_End = Parent.Clip_Time_End;
      this.Sample_Clip_Start = Parent.Sample_Clip_Start;
      this.Sample_Clip_End = Parent.Sample_Clip_End;
    }
    public Render_Context(Render_Context Parent, Playable Child_Frame) {// pass the torch of context coordinates
      this(Parent);
      this.Add_Transpose(Child_Frame);
    }
    public void Add_Transpose(double Time_Offset, double Pitch_Offset) {
      this.Absolute_Time += Time_Offset;
      this.Absolute_YTranspose += Pitch_Offset;
    }
    public void Add_Transpose(Playable Child_Frame) {
      this.Absolute_Time += Child_Frame.Start_Time_G();
      this.Absolute_YTranspose += Child_Frame.Get_Pitch();
    }
  }
  /* ************************************************************************************************************************ */
  public static class Drawing_Context {
    Drawing_Context Parent;
    public Graphics2D gr;
    public double Absolute_X, Absolute_Y;
    public double Trans_X, Trans_Y;
    public double Scale_X, Scale_Y;
    public double Clip_Start, Clip_End;
    public Boolean Highlight;
    public Drawing_Context(Graphics2D Fresh_Graphics) {
      this.Parent = null;
      this.gr = Fresh_Graphics;
      this.Absolute_X = 0.0;
      this.Absolute_Y = 0.0;
      Scale_X = xscale;
      Scale_Y = -yscale;
      Trans_X = 0.0;
      Trans_Y = 0.0;
      Clip_Start = 0.0;
      Clip_End = Double.MAX_VALUE;
      Highlight = false;
    }
    public Drawing_Context(Drawing_Context Clone_Donor) {
      this.Parent = Clone_Donor;
      this.gr = Clone_Donor.gr;
      this.Absolute_X = Clone_Donor.Absolute_X;
      this.Absolute_Y = Clone_Donor.Absolute_Y;
      Scale_X = Clone_Donor.Scale_X;
      Scale_Y = Clone_Donor.Scale_Y;
      Trans_X = Clone_Donor.Trans_X;
      Trans_Y = Clone_Donor.Trans_Y;
      Clip_Start = Clone_Donor.Clip_Start;
      Clip_End = Clone_Donor.Clip_End;
      Highlight = Clone_Donor.Highlight;
    }
    public Drawing_Context(Drawing_Context Fresh_Parent, Playable_Drawable Fresh_Note) {
      this.Parent = Fresh_Parent;
      this.gr = Fresh_Parent.gr;
      this.Absolute_X = Fresh_Parent.Absolute_X + Fresh_Note.Start_Time_G();
      this.Absolute_Y = Fresh_Parent.Absolute_Y + Fresh_Note.Get_Pitch();
      Scale_X = Fresh_Parent.Scale_X;
      Scale_Y = Fresh_Parent.Scale_Y;
      Trans_X = Fresh_Parent.Trans_X;
      Trans_Y = Fresh_Parent.Trans_Y;
      Clip_Start = Fresh_Parent.Clip_Start;
      Clip_End = Fresh_Parent.Clip_End;
      Highlight = Fresh_Parent.Highlight;
    }
    public void Add_Transpose(Playable_Drawable Child_Frame) {
      this.Absolute_X += Child_Frame.Start_Time_G();
      this.Absolute_Y += Child_Frame.Get_Pitch();

      //Trans_X = Absolute_X; Trans_Y = Absolute_Y;
      /*
      Scale_X = Child_Frame.Scale_X;
      Scale_Y = Fresh_Parent.Scale_Y;
      Trans_X = Fresh_Parent.Trans_X;
      Trans_Y = Fresh_Parent.Trans_Y;
      Clip_Start = Fresh_Parent.Clip_Start;
      Clip_End = Fresh_Parent.Clip_End;
      Highlight = Fresh_Parent.Highlight;
       */
    }
    public Point2D.Double To_Screen(double xraw, double yraw) {
      Point2D.Double loc = new Point2D.Double();
      loc.setLocation((xraw * Scale_X) + Trans_X, (yraw * Scale_Y) + Trans_Y);
      //loc.setLocation((xraw * Scale_X) + Absolute_X, (yraw * Scale_Y) + Absolute_Y);
      return loc;
    }
    public Point2D.Double From_Screen(double xscreen, double yscreen) {
      Point2D.Double loc = new Point2D.Double();
      loc.setLocation(((xscreen - Trans_X) / Scale_X), ((yscreen - Trans_Y) / Scale_Y));
      //loc.setLocation(((xscreen - Absolute_X) / Scale_X), ((yscreen - Absolute_Y) / Scale_Y));
      return loc;
    }
  }
  /* ************************************************************************************************************************ */
  public interface Drawable {
    // Drawable Hit_Test(double Xloc, double Yloc);// gets my child that is hit here.
    ArrayList<Drawable> Get_My_Children();//  ? do we need this?  maps the playable tree to a drawable tree.
    void Draw_Me(Drawing_Context dc);// may have to pass some parent context too.
  }
  /* ************************************************************************************************************************ */
  public class Loudness_Control_Point {
    public double timepercent, level;
  }
  public class Loudness_Control_List {
    ArrayList<Loudness_Control_Point> cpoints;
    public Loudness_Control_List() {
      cpoints = new ArrayList<Loudness_Control_Point>();
    }
    public Loudness_Control_Point Create(double timepercent, double level) {
      Loudness_Control_Point cpoint = new Loudness_Control_Point();
      this.cpoints.add(cpoint);// need to treesearch and insert this
      return cpoint;
    }
    public void Remove(Loudness_Control_Point cpoint) {// or delete
      this.cpoints.remove(cpoint);// need to treesearch and remove this
    }
    public Loudness_Control_Point Hit_Test(double timepercent) {
      Loudness_Control_Point cpoint = null;// need to treesearch and return this
      return cpoint;
    }
    /*
    vocptype volcp.create(time, level)
    volcp.remove(vocptype cp) // or delete
    vocptype volcp.Hit_Test(time)
    volcp.move(vocptype cp, newtime)
     */
  }
  /* ************************************************************************************************************************ */
  public static class Hit_Stack {
    public int Stack_Depth;
    Playable_Drawable[] Path;
    public void Init(int Depth) {
      Stack_Depth = Depth;
      Path = new Playable_Drawable[Depth];
    }
    public void Set(int Depth, Playable_Drawable Note) {
      Path[Depth] = Note;
    }
    public void Render_Audio(Wave_Carrier wave) {
      if (Stack_Depth <= 0) {
        wave.WaveForm = new double[1];
        return;
      }
      Render_Context rc = new Render_Context();
      rc.Clip_Time_End = Double.MAX_VALUE;
      int Last_Depth = Stack_Depth - 1;
      Create_Audio_Transform(rc);
      Playable leaf = this.Path[Last_Depth];
      leaf.Render_Audio(rc, wave);
    }
    public Render_Context Create_Audio_Transform(Render_Context rc) {
      int Last_Depth = Stack_Depth - 1;
      for (int depth = 0; depth < Last_Depth; depth++) {
        Playable pb = this.Path[depth];
        rc.Add_Transpose(pb);
      }
      return rc;
    }
    public Drawing_Context Create_Drawing_Transform(Drawing_Context dc) {
      int Last_Depth = Stack_Depth - 0;
      for (int depth = 0; depth < Last_Depth; depth++) {
        Playable_Drawable pb = this.Path[depth];
        dc.Add_Transpose(pb);
      }
      return dc;
    }
    public Playable_Drawable End() {
      int Last_Depth = Stack_Depth - 1;
      return this.Path[Last_Depth];
    }
    public void Clear() {
      Stack_Depth = 0;
      Path = null;
    }
    public Playable Container;
    /*
    this is where we might attach exactly which line we hit IF we hit a container.
     * does this mean that hitting a line means we hit the container?  then the whole vine may move if we drag one line.
     * could just add a flag that says 'no lines' or yes lines, to try out later.
     * but, if we use this to find a drag destination, it might return true if we hit a non-container (leaf)
     *
     * static public Boolean Hit_Test_Line(Point2D.Double pnt, Line2D.Double lin, Point2D.Double hitpnt)
     *
     *
     */
  }
  /* ************************************************************************************************************************ */
  public class Target_Container_Stack extends Hit_Stack {
    public Playable Exclude;
    public DropBox DropBox_Found;
    /*
    nothing here!  
    everything is a container except leaves right?
    so either we have a test 
    
    leaves can be siblings to containers.  we just want to ignore all leaves.  can have a test for that.  IsContainer()?
    
    oh right, containers return any of their children I hit.  generic.
    
    point being, what matters if I am a possible destination for the drifter.  not just a container, but an open one.
    
     */
  }
  /* ************************************************************************************************************************ */
  public interface Playable {
    void Set_Octave(double Fresh_Octave);
    void Set_Frequency(double Fresh_Frequency);
    double Start_Time_G();
    void Start_Time_S(double val);// getset
    double End_Time_G();// getset
    double Duration_G();
    void Duration_S(double value);// getset
    double Get_Pitch();
    /*
     * we will need
     * double Get_Start_Pitch();
     * Set_Start_Pitch(double value);
     *
     * and at least for bendy notes:
     * double Get_End_Pitch();
     * Set_End_Pitch(double value);
     *
     */
    double Get_Max_Amplitude();
    double Loudness_G(double percent);
    double Loudness_S(double percent, double value);
    void Render_Audio(Render_Context rc, Wave_Carrier Wave);// stateless rendering (requires calculus)
    void Render_Audio_Start(Render_Context rc);// stateful rendering
    void Render_Audio_To(double Hasta, Wave_Carrier Wave);
    Boolean Hit_Test_Stack(Drawing_Context dc, double Xloc, double Yloc, int Depth, Hit_Stack Stack);// gets the stack from me to the grandchild you hit.  ideally you'd load it on the way back out, but that'd load in reverse yes?
    Boolean Hit_Test_Container(Drawing_Context dc, double Xloc, double Yloc, int Depth, Target_Container_Stack Stack);
    String Name_G();
    String Name_S(String value);
  };
  /* ************************************************************************************************************************ */
  public interface Playable_Drawable extends Playable, Drawable, Cloneable {
    //Draw_Me(Drawing_Context dc) {// Drawable
    Playable_Drawable Xerox_Me();
  }
  /* ************************************************************************************************************************ */
  public interface DropBox extends Playable_Drawable {/* Anything that can receive an object in drag and drop. */

    void Container_Insert(Playable_Drawable NewChild, double Time, double Pitch);
  }
  /* ************************************************************************************************************************ */
  public static class Dummy_Playable implements Playable_Drawable {
    String MyName;
    // //#region Playable Members
    public Boolean Hit_Test_Stack(Drawing_Context dc, double Xloc, double Yloc, int Depth, Hit_Stack Stack) {
      /* Dummy_Playable  */
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
      Depth++;
      return false;
    }
    /* ************************************************************************************************************************ */
    public Boolean Hit_Test_Container(Drawing_Context dc, double Xloc, double Yloc, int Depth, Target_Container_Stack Stack) {
      return false;/* Dummy_Playable is a leaf, so always returns false. */
    }
    public void Set_Octave(double Fresh_Octave) {
      Boolean snargle = true;
    }
    public void Set_Frequency(double Fresh_Frequency) {
      Boolean snargle = true;
    }
    double Start_Time_Val;
    public/* virtual */ double Start_Time_G() {
      return Start_Time_Val;
    }
    public/* virtual */ void Start_Time_S(double value) {
      Start_Time_Val = value;
    }
    public double End_Time_G() {
      return Start_Time_Val + this.Duration_G();
    }
    /* ************************************************************************************************************************ */
    double Duration_Val;
    @Override
    public double Duration_G() {
      return Duration_Val;
    }
    @Override
    public void Duration_S(double value) {
      Duration_Val = value;
    }
    public double Get_Pitch() {
      return 1.0;
    }
    public/* virtual */ double Get_Max_Amplitude() {
      return 1.0;
    }
    double Loudness_Value_0, Loudness_Value_1;
    public double Loudness_G(double percent) {
      if (percent < 0.5) {
        return Loudness_Value_0;
      } else {
        return Loudness_Value_1;
      }
    }
    public double Loudness_S(double percent, double value) {
      if (percent < 0.5) {
        Loudness_Value_0 = value;
      } else {
        Loudness_Value_1 = value;
      }
      return value;
    }
    public/* virtual */ void Render_Audio(Render_Context rc, Wave_Carrier Wave) {
      Boolean snargle = true;
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_Start(Render_Context rc) {
    }// stateful rendering
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_To(double Hasta, Wave_Carrier Wave) {
    }
    ////#endregion
    // Drawable interface
    /* ************************************************************************************************************************ */
    public ArrayList<Drawable> Get_My_Children() {// Drawable
      return null;
    }
    public void Draw_Me(Drawing_Context dc) {// Drawable Dummy_Playable
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
      mydc.gr.fillOval((int) (pnt.x) - 5, (int) (pnt.y) - 5, 10, 10);
    }
    /* ************************************************************************************************************************ */
    public Playable_Drawable Xerox_Me() {
      Dummy_Playable child = null;
      try {
        child = (Dummy_Playable) this.clone();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
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
  /* ************************************************************************************************************************ */
  public class Root_Playable extends Dummy_Playable {
    public Playable_Drawable Child;
    /* ************************************************************************************************************************ */
    public Root_Playable() {
    }
    public Root_Playable(Playable_Drawable Fresh_Child) {
      Child = Fresh_Child;
    }
    /* ************************************************************************************************************************ */
    public void Sing_To_File(String FilePath, int Sample_Rate, double Start_Time, double End_Time) {
      WaveDouble outbuf = new WaveDouble();
      Sing_To_Array(Sample_Rate, Start_Time, End_Time, outbuf);
      for (int scnt = 0; scnt < outbuf.numsamples; scnt++) {
        outbuf.wave[scnt] *= 0.75;// arbitrary scaling to match C# code
      }
      StdAudio.save(FilePath, outbuf.wave);
    }
    /* ************************************************************************************************************************ */
    public void Sing_To_Array(int Sample_Rate, double Start_Time, double End_Time, WaveDouble outbuf) {// just for testing
      double amp = 0.0;
      double Test_Duration = End_Time - Start_Time;
      int Num_Samples = (int) (((double) Sample_Rate) * Child.End_Time_G());
      Wave_Carrier Wave = new Wave_Carrier();

      Render_Context rc = new Render_Context();
      rc.Clip_Time_Start = Start_Time;
      rc.Clip_Time_End = End_Time;
      //long start = System.nanoTime();
      long start = System.currentTimeMillis();

      Child.Render_Audio(rc, Wave);
      //long end = System.nanoTime();
      long end = System.currentTimeMillis();

      Num_Samples = Wave.WaveForm.length;
      outbuf.numsamples = Num_Samples;
      outbuf.Init(Num_Samples);
      for (int cnt = 0; cnt < Wave.WaveForm.length; cnt++) {
        amp = Wave.WaveForm[cnt];
        outbuf.wave[cnt] = amp;
      }
      double Milliseconds = end - start;
      double metric = 1000.0 * Child.Duration_G() / Milliseconds;
      System.out.println("Milliseconds:" + Milliseconds + "  Metric:" + metric);
      try {
        java.io.FileWriter outFile = new java.io.FileWriter("metric.txt");
        PrintWriter out = new PrintWriter(outFile);
        out.println("Milliseconds:" + Milliseconds + "  Metric:" + metric);
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    double Radius = 8;
    double Diameter = Radius * 2.0;
    /* ************************************************************************************************************************ */
    ////#region Playable Members
    @Override
    public Boolean Hit_Test_Stack(Drawing_Context dc, double Xloc, double Yloc, int Depth, Hit_Stack Stack) {
      /* Root_Playable  */
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

      if (Child.Hit_Test_Stack(mydc, Xloc, Yloc, Depth + 1, Stack)) {
        Stack.Set(Depth, this);
        return true;
      } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
        Stack.Init(Depth + 1);
        Stack.Set(Depth, this);
        return true;
      } else {
        return false;
      }
    }
    /* ************************************************************************************************************************ */
    @Override
    public Boolean Hit_Test_Container(Drawing_Context dc, double Xloc, double Yloc, int Depth, Target_Container_Stack Stack) {
      /* Root_Playable  */
      if (this == Stack.Exclude) {
        return false;
      }
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

      if (Child.Hit_Test_Container(mydc, Xloc, Yloc, Depth + 1, Stack)) {
        Stack.Set(Depth, this);
        return true;
      } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
        Stack.Init(Depth + 1);
        Stack.Set(Depth, this);
        return true;
      } else {
        return false;
      }
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
    double M_Start_Time = 0.0;
    @Override
    public/* virtual */ double Start_Time_G() {
      return M_Start_Time;
    }
    @Override
    public/* virtual */ void Start_Time_S(double val) {
      M_Start_Time = val;
    }
    /* ************************************************************************************************************************ */
    @Override
    public double End_Time_G() {// getset
      return this.Child.End_Time_G();
    }
    /* ************************************************************************************************************************ */
    @Override
    public double Duration_G() {
      return this.Child.Duration_G();
    }
    @Override
    public void Duration_S(double value) {
    }
    @Override
    public double Get_Pitch() {
      if (false) {
        return 0.0;// snargle
      } else {
        return 1.0;// snargle
      }
    }
    @Override
    public double Get_Max_Amplitude() {
      return 1.0;// snargle
    }
    /* ************************************************************************************************************************ */
    //double Loudness_Value_0, Loudness_Value_1;
    @Override
    public double Loudness_G(double percent) {
      if (percent < 0.5) {
        return Loudness_Value_0;
      } else {
        return Loudness_Value_1;
      }
    }
    @Override
    public double Loudness_S(double percent, double value) {
      if (percent < 0.5) {
        Loudness_Value_0 = value;
      } else {
        Loudness_Value_1 = value;
      }
      return value;
    }
    @Override
    public void Render_Audio(Render_Context rc, Wave_Carrier Wave) {
      //rc = new Render_Context();
      this.Child.Render_Audio(rc, Wave);
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_Start(Render_Context rc) {// stateful rendering
      this.Child.Render_Audio_Start(rc);
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_To(double Hasta, Wave_Carrier Wave) {
      this.Child.Render_Audio_To(Hasta, Wave);
    }
    // Drawable interface
    /* ************************************************************************************************************************ */
    @Override
    public ArrayList<Drawable> Get_My_Children() {// Drawable
      return null;
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Draw_Me(Drawing_Context dc) {// Drawable
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
      AntiAlias(mydc.gr);
      mydc.gr.setColor(Color.black);
      mydc.gr.fillOval((int) (pnt.x) - (int) Radius, (int) (pnt.y) - (int) Radius, (int) Diameter, (int) Diameter);
      this.Child.Draw_Me(mydc);
    }
    public void AntiAlias(Graphics2D g2d) {//http://www.exampledepot.com/egs/java.awt/AntiAlias.html?l=rel

      // D:\Users\JCAtkeson\Documents\projects\TunePad\dist>tunepad.jar -Dsun.java2d.opengl=true

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
  }
  /* ************************************************************************************************************************ */
  public class Note implements Playable_Drawable {
    public double octave = 10.0, frequency = 0.0;// 440;
    double slope = 0.0, ybase = 0.0;
    public String MyName;
    public Note() {
      this.Loudness_S(0.0, 1.0);
      this.Loudness_S(1.0, 0.0);
      octave = 0.0;
      frequency = 0.0;
    }
    ////#region Playable Members
    public void Set_Octave(double Fresh_Octave) {
      this.octave = Fresh_Octave;
      this.frequency = Octave_To_Frequency(Fresh_Octave);
    }
    public void Set_Frequency(double Fresh_Frequency) {
      this.frequency = Fresh_Frequency;
      this.octave = Frequency_To_Octave(Fresh_Frequency);
    }
    /* ************************************************************************************************************************ */
    double M_Start_Time = 0;
    public double Start_Time_G() {
      return M_Start_Time;
    }
    public void Start_Time_S(double val) {
      M_Start_Time = val;
    }
    /* ************************************************************************************************************************ */
    @Override
    public double End_Time_G() {
      return Start_Time_G() + Duration_G();
    }
    /* ************************************************************************************************************************ */
    double Loudness_Value_0, Loudness_Value_1;
    public double Loudness_G(double percent) {
      if (percent < 0.5) {
        return Loudness_Value_0;
      } else {
        return Loudness_Value_1;
      }
    }
    public double Loudness_S(double percent, double value) {
      if (percent < 0.5) {
        Loudness_Value_0 = value;
      } else {
        Loudness_Value_1 = value;
      }
      return value;
    }
    /* ************************************************************************************************************************ */
    double Duration_Val;
    @Override
    public double Duration_G() {
      return this.Duration_Val;
    }
    @Override
    public void Duration_S(double value) {
      this.Duration_Val = value;
    }
    @Override
    public double Get_Pitch() {
      return octave;
    }
    double Radius = 5;
    double Diameter = Radius * 2.0;
    public Boolean Hit_Test_Stack(Drawing_Context dc, double Xloc, double Yloc, int Depth, Hit_Stack Stack) {
      /* Note  */
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

      if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
        Stack.Init(Depth + 1);
        Stack.Set(Depth, this);
        //this.Diameter = 2.0 * (this.Radius = 10.0);
        return true;
      } else {
        return false;
      }
    }
    /* ************************************************************************************************************************ */
    public Boolean Hit_Test_Container(Drawing_Context dc, double Xloc, double Yloc, int Depth, Target_Container_Stack Stack) {
      return false;/* Note is a leaf, so always returns false. */
    }
    /* ************************************************************************************************************************ */
    public void Play_Me(double time0, double Base_Frequency, RefDouble amp) {
      //are;// we going to pass time0 as local note time, or parent song time?  currently it works like parent time.
      // perfect world, time0 will be local note time.  but won't a slicer have to then subtract the note's own start time from the time being passed?
      double reltime = (time0 - this.Start_Time_G());
      {
        double percenttime = reltime / this.Duration_G();
        double Loudness = ((1.0 - percenttime) * Loudness_G(0.0)) + (percenttime * Loudness_G(1.0));
        amp.num = Math.sin(reltime * frequency * TwoPi) * Loudness;
        //double cycles = Frequency_Integral_Bent_Octave(slope, ybase, time0);
      }
    }
    private void Play_Me_Local_Time(Render_Context rc, RefDouble amp) {// assumes we are passed time in the note's own local coordinates.
    }
    private double Play_Me_Local_Time(double time0, double Base_Frequency, RefDouble amp) {// assumes we are passed time in the note's own local coordinates.
      double percenttime = time0 / this.Duration_G();
      double Loudness = ((1.0 - percenttime) * Loudness_G(0.0)) + (percenttime * Loudness_G(1.0));
      amp.num = Math.sin(time0 * (Base_Frequency) * TwoPi) * Loudness;
      //double cycles = Frequency_Integral_Bent_Octave(slope, ybase, time0);
      return amp.num;
    }
    public double Get_Max_Amplitude() {
      return 1.0;
    }
    /* ************************************************************************************************************************ */
    public/* virtual */ void Render_Audio(Render_Context rc, Wave_Carrier Wave) {
      long samplecnt = 0;
      double Time;
      RefDouble amp = new RefDouble();
      double Note_Time_Offset;

      double Absolute_Pitch = rc.Absolute_YTranspose + this.octave;

      double Note_Absolute_Start_Time = rc.Absolute_Time + this.Start_Time_G();
      double Local_Clip_Absolute_Start_Time = Math.max(Note_Absolute_Start_Time, rc.Clip_Time_Start);
      double Local_Clip_Absolute_End_Time = Math.min(Local_Clip_Absolute_Start_Time + this.Duration_G(), rc.Clip_Time_End);
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
    Render_Context rctemp;
    double My_Absolute_Start_Pitch;
    double My_Absolute_Start_Time;
    double Local_Clip_Absolute_Start_Time;
    long Absolute_Sample_Start_Dex;
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_Start(Render_Context rc) {// stateful rendering
      rctemp = rc;
      My_Absolute_Start_Pitch = rctemp.Absolute_YTranspose + this.octave;
      My_Absolute_Start_Time = rctemp.Absolute_Time + this.Start_Time_G();
      Local_Clip_Absolute_Start_Time = Math.max(My_Absolute_Start_Time, rctemp.Clip_Time_Start);
      Absolute_Sample_Start_Dex = (long) Math.ceil(Local_Clip_Absolute_Start_Time * (double) rctemp.Sample_Rate);// index of first sample of this note.
      /*
       * why all this crap?
       * we are passed the absolute time,octave org.
       * we must first add my local coords (why do I have a local org when parent should set it?)
       * then from local coords we see if I start and end outside of the window.  so we are passed both the org offset, and the clip window.
       *
       * one rule: get rid of local origin xy.  parent can define that.  root parent has final org.
       *
       * so absolute clip start is compared to my absolute T origin passed to me.  take the greater.
       * if clip start is beyond my end we bail.
       * if my start is beyond clip end we bail.
       * 
       * sample start is just clip start, translated into an array index.
       * 
       */
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_To(double Hasta, Wave_Carrier Wave) {
      long samplecnt = 0;
      double Time;
      RefDouble amp = new RefDouble();
      double Note_Time_Offset;

      double Local_Clip_Absolute_End_Time = Math.min(Local_Clip_Absolute_Start_Time + this.Duration_G(), rctemp.Clip_Time_End);
      if (Local_Clip_Absolute_Start_Time >= Local_Clip_Absolute_End_Time) {
        Wave.WaveForm = new double[0];
        Wave.Duration = 0;
        return;
      }/* zero-length */
      Wave.Start_Time = Local_Clip_Absolute_Start_Time;

      long Absolute_Sample_End = (long) Math.floor(Local_Clip_Absolute_End_Time * (double) rctemp.Sample_Rate);// index of last sample of this note.
      Wave.Start_Index = Absolute_Sample_Start_Dex;

      long Num_Samples = Absolute_Sample_End - Absolute_Sample_Start_Dex;
      Wave.WaveForm = new double[(int) (Num_Samples + Slop)];

      double Absolute_Frequency = Octave_To_Frequency(My_Absolute_Start_Pitch);

      /* Note_Time_Offset is the difference between the beginning of this note and the place we start rendering, defined by clip start time. */
      Note_Time_Offset = (Absolute_Sample_Start_Dex * rctemp.Sample_Interval) - My_Absolute_Start_Time;// this time is right.  first it is aligned to T0 (origin of the universe), then cropped to local note coords
      for (samplecnt = 0; samplecnt < Num_Samples; samplecnt++) {
        Time = Note_Time_Offset + (rctemp.Sample_Interval * (double) samplecnt);
        this.Play_Me_Local_Time(Time, Absolute_Frequency, amp);
        Wave.WaveForm[(int) samplecnt] = amp.num;
      }
    }
    /* #endregion */
    /* ************************************************************************************************************************ */
    public void Change_Time(double Time) {
      this.Start_Time_S(Time);
    }
    /* Drawable interface */
    /* ************************************************************************************************************************ */
    public ArrayList<Drawable> Get_My_Children() {/* Drawable */
      return null;
    }
    public void Draw_Me(Drawing_Context dc) {/* Drawable */
      /* Note  */
      Drawing_Context mydc = new Drawing_Context(dc, this);
      Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
      mydc.gr.fillOval((int) (pnt.x) - (int) Radius, (int) (pnt.y) - (int) Radius, (int) Diameter, (int) Diameter);
      //mydc.gr.fillOval((int) (mydc.Absolute_X * xscale) - 5, (int) (mydc.Absolute_Y * yscale) - 5, 10, 10);
      // fill triangle here.
      double amphgt = 0.2;
      Polygon pgon = new Polygon();
      int[] xpoints = new int[3];
      int[] ypoints = new int[3];

      Point2D.Double endpnt = mydc.To_Screen(mydc.Absolute_X + this.Duration_G(), mydc.Absolute_Y);
      Point2D.Double pnt0 = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y - amphgt);
      Point2D.Double pnt1 = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y + amphgt);

      xpoints[0] = (int) pnt0.x;
      ypoints[0] = (int) pnt0.y;

      xpoints[1] = (int) pnt1.x;
      ypoints[1] = (int) pnt1.y;

      xpoints[2] = (int) endpnt.x;
      ypoints[2] = (int) endpnt.y;

      mydc.gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

      Color color = Color.cyan;
      //color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.1f); //Red

      //Color color = new Color(1, 0, 0, 0.5); //Red
      //color.getAlpha();  mydc.gr.setPaint(color);

      mydc.gr.setColor(color);
      //mydc.gr.drawPolygon(xpoints, ypoints, 3);
      mydc.gr.fillPolygon(xpoints, ypoints, 3);

      mydc.gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

    }
    /* ************************************************************************************************************************ */
    public Playable_Drawable Xerox_Me() {
      Note child = null;
      try {
        child = (Note) this.clone();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
    }
    /* ************************************************************************************************************************ */
    public String Name_G() {
      return MyName;
    }
    /* ************************************************************************************************************************ */
    public String Name_S(String value) {
      return MyName = value;
    }
  };
  /* ************************************************************************************************************************ */
  public Chorus_Vine Make_Phrase_Musical(double Base_Octave) {
    Chorus_Vine vine = new Chorus_Vine();
    vine.Loudness_S(0.0, vine.Loudness_S(1.0, 0.75));
    double step = 1.0 / 12.0;
    double TStart = 0.0, TEnd = 1.0 + step;
    double Duration = 0.3;
    double Spacing = -0.1;// 0.0001;
    double Note_Local_Delay = 0.0;

    Note not1;
    {
      not1 = new Note();
      not1.Start_Time_S(Note_Local_Delay);
      not1.Duration_S(Duration);
      not1.Loudness_S(0.0, 0.75);
      not1.Name_S("Phrase Note 0");
      vine.Add_Note(not1, TStart, Base_Octave + step * 2.0);
      TStart += not1.Duration_G() + Spacing;
    }
    {
      not1 = new Note();
      not1.Start_Time_S(Note_Local_Delay);
      not1.Duration_S(Duration / 2);
      not1.Loudness_S(0.0, 0.75);
      not1.Name_S("Phrase Note 1");
      vine.Add_Note(not1, TStart, Base_Octave + step * 0.0);
      TStart += not1.Duration_G() + Spacing;
    }
    {
      not1 = new Note();
      not1.Start_Time_S(Note_Local_Delay);
      not1.Duration_S(Duration * 2);
      not1.Loudness_S(0.0, 0.75);
      not1.Name_S("Phrase Note 2");
      vine.Add_Note(not1, TStart, Base_Octave + step * 5.0);
      TStart += not1.Duration_G() + Spacing;
    }
    return vine;
  }
  /* ************************************************************************************************************************ */
  public Chorus_Vine Make_Phrase(double Base_Octave, Boolean dodrone) {
    Chorus_Vine vine = new Chorus_Vine();
    vine.Loudness_S(0.0, vine.Loudness_S(1.0, 0.75));
    double step = 1.0 / 12.0;
    double TStart = 0.0, TEnd = 1.0 + step;
    double Duration = 0.125;
    double Spacing = 0.0;// 0.0001;
    double Note_Local_Delay = 0.0;
    //Duration *= 0.5; //Duration *= 0.5;

    if (dodrone) {// attempted drone
      Note drone = new Note();
      drone.Start_Time_S(Note_Local_Delay);
      drone.Duration_S(Duration * 14);
      //not0.Set_Octave(Base_Octave - 1.2);
      drone.Loudness_S(1.0, 1.0);
      vine.Add_Note(drone, TStart, Base_Octave - 1.2);
      TStart += Duration + Spacing;
    }
    int ecnt = 0;
    for (double ncnt = 0; ncnt < TEnd; ncnt += step) {
      Note not1 = new Note();
      not1.Start_Time_S(Note_Local_Delay);
      not1.Duration_S(Duration);// +Duration / 2;
      //not1.Set_Octave(Base_Octave + ncnt);
      not1.Loudness_S(0.0, 0.75);
      vine.Add_Note(not1, TStart, Base_Octave + ncnt);
      TStart += Duration + Spacing;
      ecnt++;
    }
    return vine;
  }
  /* ************************************************************************************************************************ */
  public Chorus_Vine Make_Chord(double TStart, double Duration, double base_octave) {
    double step = 1.0 / 12.0;
    Chorus_Vine cv = new Chorus_Vine();
    cv.Loudness_S(0.0, 1.0);
    cv.Loudness_S(1.0, 1.0);

    Note not1;
    not1 = new Note();
    not1.Duration_S(Duration);
    not1.Loudness_S(0.0, 0.50);
    not1.Name_S("Chord Note 0");
    cv.Add_Note(not1, TStart, base_octave - ((15 * step) + 0.01));

    not1 = new Note();
    not1.Duration_S(Duration);
    not1.Loudness_S(0.0, 0.50);
    not1.Name_S("Chord Note 1");
    cv.Add_Note(not1, TStart, base_octave - ((10 * step) - 0.00));

    cv.Duration_S(not1.Duration_G());
    return cv;
  }
  /* ************************************************************************************************************************ */
  public Root_Playable Compose_More() {
    double Octave_A440 = 8.78135971352466;
    Chorus_Vine vine = new Chorus_Vine();
    vine.Loudness_S(0.0, vine.Loudness_S(1.0, 0.75));
    double step = 1.0 / 12.0;
    double TStart = 0.0, TEnd = 1.0 + step;
    double Spacing = 0.1;
    if (true) {
      Chorus_Vine subvine;

      subvine = Make_Phrase_Musical(0.0);
      subvine.Name_S("Owl");
      vine.Add_Note(subvine, TStart, Octave_A440);
      TStart += subvine.Duration_G() - 0.4;

      subvine = Make_Phrase_Musical(0.0);
      subvine.Name_S("Cat");
      vine.Add_Note(subvine, TStart, Octave_A440 - 1.0);
      TStart += subvine.Duration_G() - 0.4;

      Chorus_Vine cv = null;
      for (int cnt = 0; cnt < 2; cnt++) {
        double base_octave = Octave_A440;
        if (true) {
          cv = Make_Chord(0.0, subvine.Duration_G(), 0.0);
          cv.Name_S("Chord");
          double loudtemp = cv.Loudness_G(1.0) * 0.8;
          cv.Loudness_S(0.0, cv.Loudness_S(1.0, loudtemp));
          vine.Add_Note(cv, TStart, base_octave + cnt * step * 2.5);
          TStart += cv.Duration_G() - 0.6;
        }
        //vine.Duration = cv.End_Time;
      }

      TStart += cv.Duration_G() + Spacing;
      //vine.Duration = 2.5;
      vine.Duration_S(TStart);
      //vine.Duration = 1.0;
    }
    //Root_Playable rp = new Root_Playable(ph0);
    Root_Playable rp = new Root_Playable(vine);
    return rp;
  }
  /* ************************************************************************************************************************ */
  public void Compose_Vine() {
    double Octave_A440 = 8.78135971352466;
    Root_Playable rp;
    Chorus_Vine vine = new Chorus_Vine();
    vine.Loudness_S(0.0, vine.Loudness_S(1.0, 0.75));
    double step = 1.0 / 12.0;
    double TStart = 0.0, TEnd = 1.0 + step;
    double Duration = 0.125;
    double Spacing = 0.0;// 0.0001;
    double Note_Local_Delay = 0.0;
    //Duration *= 0.5; Duration *= 0.5;

    int Sample_Rate = 44100;
    {
      rp = Compose_More();
      rootplay = rp;
    }
    String fpathout;
    fpathout = audiopath + "test_vine.violin.wav";
    fpathout = audiopath + "test_vine.sine.wav";
    fpathout = audiopath + "test_vine.all.wav";
    fpathout = audiopath + "test_vine.music.java.wav";

    WaveDouble outbuf = new WaveDouble();
    //rp.Sing_To_Array(Sample_Rate, rp.Start_Time, rp.End_Time / 1000.0, out outbuf);
    rp.Sing_To_Array(Sample_Rate, rp.Start_Time_G(), rp.End_Time_G(), outbuf);
    if (false) {
      rp.Sing_To_File(fpathout, Sample_Rate, rp.Start_Time_G(), rp.End_Time_G());
    }
  }
  /* ************************************************************************************************************************ */
  public static double Octave_To_Frequency(double Fresh_Octave) {
    return Math.pow(2.0, Fresh_Octave);
  }
  /* ************************************************************************************************************************ */
  public static double Frequency_To_Octave(double Fresh_Frequency) {
    return Math.log(Fresh_Frequency) / Math.log(2.0);
  }
  /* ************************************************************************************************************************ */
  static double Frequency_Integral_Bent_Frequency(double slope, double ybase, double xval) {// http://www.quickmath.com   bent note math
    double frequency = slope * (xval * xval) * 0.5 + (ybase * xval);// returns the number of cycles since T0, assuming linear change to frequency.
    // (ybase * xval) + (slope * xval*xval)/2
    return frequency;
  }
  /* ************************************************************************************************************************ */
  static double Frequency_Integral_Bent_Octave(double slope, double ybase, double xval) {// http://www.quickmath.com   bent note math
    double frequency_from_octave_integral = Math.pow(2.0, (ybase + slope * xval)) / (slope * Math.log(2.0));// returns the number of cycles since T0, assuming linear change to octave.
    return frequency_from_octave_integral;
  }
  /* ************************************************************************************************************************ */
  static public void Transfer_To_Array(Wave_Carrier Wave, WaveDouble outbuf) {
    double amp = 0.0;
    int Num_Samples = Wave.WaveForm.length;
    outbuf.numsamples = Num_Samples;
    outbuf.Init(Num_Samples);
    for (int cnt = 0; cnt < Wave.WaveForm.length; cnt++) {
      amp = Wave.WaveForm[cnt];
      outbuf.wave[cnt] = amp;
    }
  }
  /* ************************************************************************************************************************ */
  static public Boolean Hit_Test_Line(Point2D.Double pnt, Line2D.Double lin, double Radius, Point2D.Double hitpnt) {
    // If you draw a line through pnt that hits the line at a right angle, return the point where it hits the line.
    // Also, return true if the intersection is between the lines endpoints AND within Radius distance of the line.
    //double Radius = 5;
    Boolean hit = false;
    Point2D.Double deltaline = new Point2D.Double(lin.x2 - lin.x1, lin.y2 - lin.y1);//  ;delta.x = lin.x2 - lin.x1;
    double linelen = Math.hypot(deltaline.x, deltaline.y);
    Point2D.Double normline = new Point2D.Double(deltaline.x / linelen, deltaline.y / linelen);// this is normalized to length 1.0

    Point2D.Double deltapnt = new Point2D.Double(pnt.x - lin.x1, pnt.y - lin.y1);//  ;delta.x = lin.x2 - lin.x1;

    double DotProduct = (deltapnt.x * normline.x + deltapnt.y * normline.y);

    //double along_the_way = DotProduct / linelen; hitpnt.setLocation(lin.x1 + (deltaline.x * along_the_way), lin.y1 + (deltaline.y * along_the_way));
    hitpnt.setLocation(lin.x1 + (normline.x * DotProduct), lin.y1 + (normline.y * DotProduct));
    if (DotProduct >= 0.0) {
      if (DotProduct <= linelen) {
        double Distance = pnt.distance(hitpnt);
        if (Distance <= Radius) {/* then this is a hit. */
          hit = true;
        }
      }
    }
    return hit;
  }
}
