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
import tunepad.TunePadLogic.*;

/*
Chorus_Vine is a really important playable type.  It handles all the grouping of all other playable objects.

It is similar to to grouping in vector art, but all the contents must always be sorted by time.  
So everything in the group hangs from of a single weaving time line.

 */

/* ************************************************************************************************************************ */
public class Chorus_Vine extends Note_List_Base implements TunePadLogic.DropBox {/* .Playable_Drawable */

  double octave, frequency;
  public String MyName = "None";
  Dictionary<Playable, Note_Box> backlist;// maps playable children back to their note_box containers
    /* ************************************************************************************************************************ */
  public Chorus_Vine() {
    this.Loudness_S(0.0, 1.0);
    this.Loudness_S(1.0, 1.0);
    backlist = new Hashtable<TunePadLogic.Playable, Note_Box>();
  }
  /* ************************************************************************************************************************ */
  public void Add_Note(TunePadLogic.Playable_Drawable freshnote, double Time, double Pitch) {
    Note_Box marker = new Note_Box(freshnote, Time, Pitch);
    marker.End_Time_S(Time + freshnote.Duration_G());
    backlist.put(freshnote, marker);
    Add_Note_Box(marker);
  }
  /* ************************************************************************************************************************ */
  public void Remove_Note(TunePadLogic.Playable_Drawable gonote) {
    Note_Box marker = backlist.get(gonote);
    Remove_Note_Box(marker);
    backlist.remove(gonote);
    TunePadLogic.Delete(marker);
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Add_Note_Box(Note_Box freshnote) {
    Note_Box marker = null;
    double endtime = freshnote.End_Time_G();
    int dex = Tree_Search(freshnote.Start_Time_G(), 0, this.size());
    this.add(dex, freshnote);

    if (dex > 0)// if dex is not the first note in the vine
    {
      Note_Box prev = this.get(dex - 1);
      prev.Transfer_Overlaps(freshnote);// now transfer all ends to me that overlap me.
    }

    //dex++;// non-self-inclusive.
    while (dex < this.size()) {
      marker = this.get(dex);
      if (marker.Start_Time_G() < endtime) {
        marker.Add_Overlap(freshnote);
        dex++;
      } else {
        break;
      }
    }
    Update_Duration();
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Remove_Note_Box(Note_Box gonote) {
    Note_Box marker = null;
    double endtime = gonote.End_Time_G();
    int dex = Tree_Search(gonote.Start_Time_G(), 0, this.size());

    //dex++;// non-self-inclusive.
    while (dex < this.size()) {
      marker = this.get(dex);
      if (marker.Start_Time_G() < endtime) {
        marker.Remove_Overlap(gonote);
        dex++;
      } else {
        break;
      }
    }
    this.remove(gonote);
    Update_Duration();
  }
  /* ************************************************************************************************************************ */
  public void Remove_All_Notes() {
    backlist = new Hashtable<TunePadLogic.Playable, Note_Box>();
    this.clear();
    Update_Duration();
  }
  /* ************************************************************************************************************************ */
  private void Update_Duration() {
    if (this.size() > 0) {
      Note_Box Final_Note = this.get(this.size() - 1);
      this.Duration_S(Final_Note.Get_Last_Released().End_Time_G());
    } else {
      this.Duration_S(0.0);
    }
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Move_Note(Note_Box freshnote) {
    this.remove(freshnote);
    this.Add_Note_Box(freshnote);
  }
  /* ************************************************************************************************************************ */
  @Override
  protected int Compare(Note_Box n0, Note_Box n1) {
    return Double.compare(n0.Start_Time_G(), n1.Start_Time_G());
  }
  /* ************************************************************************************************************************ */
  @Override
  protected double Get_Comparison_Value(Note_Box nbx) {
    return nbx.Start_Time_G();
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Render_Audio(TunePadLogic.Render_Context rc, TunePadLogic.Wave_Carrier Wave) {/* Chorus_Vine */
    /* pass the torch of context coordinates */
    TunePadLogic.Render_Context LocalRC = new TunePadLogic.Render_Context(rc);
    LocalRC.Add_Transpose(this.Start_Time_G(), this.octave);// Now *I* am the absolute coordinates!  MoohoohooHahahahaha!

    /*
    Doctrine:
    Everybody's self-contained origin point is its offset from its parent origin point.
    coordinates are internal to parent, at least until every playable has its own location box.
     */

    int ncnt;
    double Sample_Interval = rc.Sample_Interval;

    double My_Absolute_Time_Origin = LocalRC.Absolute_Time;// my start time is in my parent's coordinates.

    double T0 = My_Absolute_Time_Origin;// absolute start and end times of this vine.
    double T1 = T0 + this.Duration_G();
    T0 = Math.max(T0, rc.Clip_Time_Start);
    T1 = Math.min(T1, rc.Clip_Time_End);

    double Local_Clip_Start = T0 - My_Absolute_Time_Origin;// My_Absolute_Time_Origin is the absolute X coordinate of my root.
    double Local_Clip_End = T1 - My_Absolute_Time_Origin;// Local_Clip_Start is now our internal coordinate for the clip start.

    //LocalRC.Clip_Time_Start = T0; LocalRC.Clip_Time_End = T1;

    //if (Local_Clip_Start >= Local_Clip_End) { Wave.WaveForm = new double[0]; return; }// bail.  result is beyond my start or end limit.

    // Tree_Search needs local coordinates (internal to me).
    int notedex0 = this.Tree_Search(Local_Clip_Start, 0, this.size());
    if (notedex0 > 0) {/* back up by one if the sample starts before this note. */
      if (notedex0 >= this.size()) {// this is wrong.
        Wave.Start_Time = this.End_Time_G();
        Wave.WaveForm = new double[0];
        return;// bail.  result is beyond my end limit.
      } else if (Local_Clip_Start < this.get(notedex0).Start_Time_G()) {
        notedex0--;
      }
    }

    // should wave starttime be absolute time?  why not?  it is ONLY used for merging the wave back to the main.
    int Num_Samples = (int) Math.ceil((T1 - T0) * rc.Sample_Rate);
    if (Num_Samples <= 0) {
      Wave.WaveForm = new double[0];
      return;
    }// bail out.  result is beyond my start or end limit.
    Wave.Start_Time = T0;
    Wave.WaveForm = new double[Num_Samples + TunePadLogic.Slop];// sample start time is absolute.

    int notedex1 = this.size();

    TunePadLogic.Wave_Carrier SubWave = new TunePadLogic.Wave_Carrier();// this is a parameter for my children to fill

    {// either render these overlaps, or add them to the list.
      ArrayList<Note_Box> nbxlist = new ArrayList<Note_Box>();

      {/* accumulate all notes that overlap this time slice. */
        Note_Box nbx = this.get(notedex0);/* first get the notes still sustaining from before this time slice. */
        nbx.Get_Ends_After(Local_Clip_Start, nbxlist);

        notedex0++;// jump to the next note (only if we coded for self-inclusive overlapping).

        /* Next get all the notes that *start* playing within this time slice. */
        ncnt = notedex0;
        double Note_Start_Time = Local_Clip_Start;// compare local to local.
        while (Note_Start_Time <= Local_Clip_End && ncnt < notedex1) {
          Note_Box note = this.get(ncnt);
          nbxlist.add(note);
          Note_Start_Time = note.Start_Time_G();
          ncnt++;
        }
      }

      /* now render all the notes we accumulated.  pass the absolute time and pitch. */
      for (ncnt = 0; ncnt < nbxlist.size(); ncnt++) {/* do all rendering and summing. */
        Note_Box nbx2 = nbxlist.get(ncnt);
        nbx2.Render_Audio(LocalRC, SubWave);
        int mystartdex = (int) ((SubWave.Start_Time - T0) * rc.Sample_Rate);
        for (double amp : SubWave.WaveForm) {
          // now map the time to the array index.  add time to my t0?  then mult by samplerate?
          Wave.WaveForm[mystartdex] += amp;
          mystartdex++;
        }
        SubWave.Clear();
      }
    }

    TunePadLogic.Delete(SubWave);
    TunePadLogic.Delete(LocalRC);

    //------------------------------------

    // all we want is the time offset of the first sample.  it will be either 0.0 or the relative clip
    // relative clip is absolute clip minus my start time.
    double Offset = Math.max(0.0, Wave.Start_Time - My_Absolute_Time_Origin);
    double Local_Time;
    for (ncnt = 0; ncnt < Wave.WaveForm.length; ncnt++)// apply loudness envelope
    {
      Local_Time = Offset + (((double) ncnt) * rc.Sample_Interval);

      double percenttime = Local_Time / this.Duration_G();
      double Loudness = ((1.0 - percenttime) * Loudness_G(0.0)) + (percenttime * Loudness_G(1.0));
      Wave.WaveForm[ncnt] *= Loudness;
    }
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Render_Audio_Start(TunePadLogic.Render_Context rc) {
  }// stateful rendering
    /* ************************************************************************************************************************ */
  @Override
  public void Render_Audio_To(double Hasta, TunePadLogic.Wave_Carrier Wave) {
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Set_Octave(double Fresh_Octave) {
    this.octave = Fresh_Octave;
    this.frequency = TunePadLogic.Octave_To_Frequency(Fresh_Octave);
  }
  /* ************************************************************************************************************************ */
  @Override
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
  @Override
  public double End_Time_G() {
    return this.Start_Time_G() + this.Duration_G();
  }
  /* ************************************************************************************************************************ */
  //#region Playable Members
  double Radius = 5;
  double Diameter = Radius * 2.0;
  @Override
  public Boolean Hit_Test_Stack(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
    /* Chorus_Vine  */
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.size(); cnt++) {
      TunePadLogic.Playable Child = this.get(cnt);
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
  public Boolean Hit_Test_Container(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
    /* Chorus_Vine  */
    if (this == Stack.Exclude) {
      return false;
    }
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    double Line_Radius = 5.0;
    Boolean linehit = false;
    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.size(); cnt++) {
      TunePadLogic.Playable Child = this.get(cnt);
      if (Child.Hit_Test_Container(mydc, Xloc, Yloc, Child_Depth, Stack)) {
        found = true;
        break;
      }
    }
    if (found) {/* child hit preempts hitting me */
      Stack.Set(Depth, this);
    } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
      Stack.Init(Depth + 1);/* we hit me directly */
      Stack.Set(Depth, this);
      Stack.DropBox_Found = this;
      found = true;
    } else {
      // look for line hit here.

      /*
       *  no matter what, when we hit a container we hit its line.  if we hit the handle of the container, we have just hit
       * the start of the very first line.
       * so maybe we look for line-hittiness first?  or handle-hit first?
       *
       * possible containers:
       * vine (me), repeatbox/vine, temposcale/vine, shear notebender?
       *
       * the issues with a repeatbox, temposcale or notebender are that each needs a handle unrelated to the end of the last note.
       *each can have a line that extends beyond the end of the last note, yes?  but it will look like a note and be confusing.
       * extended line in a repeater is ok because the child goes on forever.
       *
       * maybe the extended line of a temposcale will be a different color, with a different symbol at the end.
       *
       * and a notebender?  same as a temposcale really, may as well use a special line for that too.  maybe the same as with temposcale.
       *
       *
       * OK the next priority is perhaps animation while playing?  Highlight each note as it sounds.
       * so how were we supposed to do that?  render the audio in stripes.  but how to know when each note begins?  gotta rethink this.
       *
       * so: animate during play, and allow drag and drop.  dnd between containers, within a container.
       *
       * dnd with ampvelopes is not really dnd.  it is just moving, no migration.  
       *
       * dnd is a matter of 1. animate moving object, and 2. relocate object once dropped.
       *
       * any way to prevent play-click from being mistaken for drag?  maybe cntrl-click-drag is for moving?
       *
       */
      {
        int xprev, yprev, xloc, yloc;

        Point2D.Double pnt = new Point2D.Double(Xloc, Yloc);

        Line2D.Double lin = new Line2D.Double();
        Point2D.Double hitpnt = new Point2D.Double();

        Point2D.Double loc = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

        xprev = (int) loc.x;
        yprev = (int) loc.y;

        for (int cnt = 0; cnt < this.size(); cnt++) {
          Note_Box child = this.get(cnt);

          loc = mydc.To_Screen(mydc.Absolute_X + child.Start_Time_G(), (mydc.Absolute_Y + child.Get_Pitch()));

          lin.setLine(xprev, yprev, loc.x, loc.y);
          linehit = TunePadLogic.Hit_Test_Line(pnt, lin, Line_Radius, hitpnt);
          if (linehit) {
            Stack.Init(Depth + 1);
            Stack.Set(Depth, this);
            Stack.DropBox_Found = this;
            break;
          }

          xprev = (int) loc.x;
          yprev = (int) loc.y;
        }
        found = linehit;
      }
    }
    return found;
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
  public double Get_Pitch() {
    return octave;
  }
  public double Get_Max_Amplitude() {
    Boolean snargle = true;
    return 1.0;// snargle
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
  /* Drawable interface */
  /* ************************************************************************************************************************ */
  public ArrayList<TunePadLogic.Drawable> Get_My_Children() {/* Drawable */
    return null;
  }
  public void Draw_Me(TunePadLogic.Drawing_Context dc) {/* Drawable */
    /* Chorus_Vine  */
    TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
    mydc.gr.setColor(Color.blue);

    double clipstart = dc.Clip_Start;
    double clipend = dc.Clip_End;

    /*
    Polygon pgon = new Polygon();
    int[] xpoints = new int[3];
    int[] ypoints = new int[3];
    
    mydc.gr.drawPolygon(xpoints, ypoints, 3);
     */
    //mydc.gr.drawRect((int) (mydc.Absolute_X * xscale), (int) (mydc.Absolute_Y * yscale), (int) (this.Duration_G() * xscale), 100);//public void drawRect(int x, int y, int width, int height) {
    Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
    mydc.gr.fillOval((int) (pnt.x) - (int) Radius, (int) (pnt.y) - (int) Radius, (int) Diameter, (int) Diameter);

    //mydc.gr.fillOval((int) (mydc.Absolute_X * xscale) - 5, (int) (mydc.Absolute_Y * yscale) - 5, 10, 10);
    int xprev, yprev, xloc, yloc;

    Point2D.Double loc = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

    xprev = (int) loc.x;
    yprev = (int) loc.y;

    for (int cnt = 0; cnt < this.size(); cnt++) {
      Note_Box child = this.get(cnt);
      child.Draw_Me(mydc);
      loc = mydc.To_Screen(mydc.Absolute_X + child.Start_Time_G(), (mydc.Absolute_Y + child.Get_Pitch()));
      mydc.gr.setColor(Color.black);
      mydc.gr.drawLine(xprev, yprev, (int) loc.x, (int) loc.y);
      xprev = (int) loc.x;
      yprev = (int) loc.y;
    }
  }
  /* ************************************************************************************************************************ */
  public Chorus_Vine Xerox_Me_Typed() {
    Chorus_Vine child = null;
    child = (Chorus_Vine) this.clone();
    child.Remove_All_Notes();
    for (int cnt = 0; cnt < this.size(); cnt++) {
      Note_Box subnote = this.get(cnt).Xerox_Me_Typed();
      child.Add_Note(subnote.My_Note, subnote.Start_Time_G(), subnote.Get_Pitch());
    }
    return child;
  }
  /* ************************************************************************************************************************ */
  public Playable_Drawable Xerox_Me() {
    return Xerox_Me_Typed();
  }
  public void Container_Insert(Playable_Drawable NewChild, double Time, double Pitch) {
    this.Add_Note(NewChild, Time, Pitch);
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
class Note_List_Base extends ArrayList<Note_List_Base.Note_Box> {
  /* ************************************************************************************************************************ */
  public static class Note_Box implements Playable_Drawable {/* Note_Box exists to provide a local offset origin to its contents, and to link back to its list container. */

    public Playable_Drawable My_Note;
    double octave, frequency;
    private Note_Box_List My_Overlaps;/* should my note be a member of my overlaps?  then we can just play them as a group. */

    public String MyName;
    /* ************************************************************************************************************************ */
    public Note_Box() {
      My_Overlaps = new Note_Box_List();
      Loudness_S(1.0, Loudness_S(0.0, 1.0));
    }
    /* ************************************************************************************************************************ */
    public Note_Box(Playable_Drawable freshnote) {
      this();
      My_Note = freshnote;
    }
    /* ************************************************************************************************************************ */
    public Note_Box(Playable_Drawable freshnote, double Time, double Pitch) {
      this(freshnote);
      this.Start_Time_S(Time);
      this.Set_Octave(Pitch);
    }
    /* ************************************************************************************************************************ */
    public void Add_Overlap(Note_Box freshnote) {
      this.My_Overlaps.Add_Note_Box(freshnote);
    }
    /* ************************************************************************************************************************ */
    public void Remove_Overlap(Note_Box gonote) {
      this.My_Overlaps.Remove_Note_Box(gonote);
    }
    /* ************************************************************************************************************************ */
    public Note_Box Get_Last_Released() {
      Note_Box last = My_Overlaps.Get_Last_Released();
      if (last == null) {
        last = this;
      }
      return last;
    }
    /* ************************************************************************************************************************ */
    public void Get_Ends_After(double Time_Limit, Note_Box[] overlaps, TunePadLogic.RefInt Num_Found) {
      int ocnt = 0;
      while (ocnt < this.My_Overlaps.size()) {// only works if list of tails includes myself
        Note_Box tail = this.My_Overlaps.get(ocnt);
        if (tail.End_Time_G() >= Time_Limit) {
          overlaps[ocnt] = tail;
          ocnt++;
        } else {
          break;
        }
      }
      Num_Found.num = ocnt;
    }
    /* ************************************************************************************************************************ */
    public void Get_Ends_After(double Time_Limit, ArrayList<Note_Box> overlaps) {
      for (int ocnt = 0; ocnt < this.My_Overlaps.size(); ocnt++) {// only works if list of tails includes myself
        Note_Box tail = this.My_Overlaps.get(ocnt);
        if (tail.End_Time_G() >= Time_Limit) {
          overlaps.add(tail);
        } else {
          break;
        }
      }
    }
    /* ************************************************************************************************************************ */
    public void Transfer_Overlaps(Note_Box recipient) {
      double Time_Limit;
      Time_Limit = recipient.Start_Time_G();

      recipient.My_Overlaps.clear(); // recipient.My_Overlaps.AddRange();
      Get_Ends_After(Time_Limit, recipient.My_Overlaps);
    }
    //#region Playable Members
    public Boolean Hit_Test_Stack(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Hit_Stack Stack) {
      /* Note_Box  */
      TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);

      Boolean found = this.My_Note.Hit_Test_Stack(mydc, Xloc, Yloc, Depth + 1, Stack);
      if (found) {
        Stack.Set(Depth, this);
      }
      return found;
    }
    /* ************************************************************************************************************************ */
    public Boolean Hit_Test_Container(TunePadLogic.Drawing_Context dc, double Xloc, double Yloc, int Depth, TunePadLogic.Target_Container_Stack Stack) {
      /* Note_Box  */
      /* look for any contact with the container hits of any children I might own. */
      if (this == Stack.Exclude) {
        return false;
      }
      TunePadLogic.Drawing_Context mydc = new TunePadLogic.Drawing_Context(dc, this);
      Boolean found = this.My_Note.Hit_Test_Container(mydc, Xloc, Yloc, Depth + 1, Stack);
      if (found) {
        Stack.Set(Depth, this);
      }
      return found;
    }
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
      return this.My_Note.Duration_G();
    }
    @Override
    public void Duration_S(double value) {
      this.My_Note.Duration_S(value);
    }
    @Override
    public double Get_Pitch() {
      return octave;
    }
    public double Get_Max_Amplitude() {
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
    public void Render_Audio(Render_Context rc, TunePadLogic.Wave_Carrier Wave) {
      TunePadLogic.Render_Context LocalRC = new TunePadLogic.Render_Context(rc);
      LocalRC.Add_Transpose(this.Start_Time_G(), this.octave);
      My_Note.Render_Audio(LocalRC, Wave);
      TunePadLogic.Delete(LocalRC);
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_Start(TunePadLogic.Render_Context rc) {
    }// stateful rendering
    /* ************************************************************************************************************************ */
    @Override
    public void Render_Audio_To(double Hasta, TunePadLogic.Wave_Carrier Wave) {
    }
    //#endregion
    // Drawable interface
        /* ************************************************************************************************************************ */
    public ArrayList<TunePadLogic.Drawable> Get_My_Children() {// Drawable
      return null;
    }
    public void Draw_Me(TunePadLogic.Drawing_Context dc) {// Drawable
      /* Note_Box  */
      Drawing_Context mydc = new Drawing_Context(dc, this);
      dc.gr.setColor(Color.green);
      // Point2D.Double pnt = mydc.To_Screen(this.Start_Time_G(),this.Get_Pitch());
      Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_X, mydc.Absolute_Y);
      mydc.gr.fillOval((int) (pnt.x) - 5, (int) (pnt.y) - 5, 10, 10);
      //mydc.gr.fillOval((int) (mydc.Absolute_X * xscale) - 5, (int) (mydc.Absolute_Y * yscale) - 5, 10, 10);
      My_Note.Draw_Me(mydc);
    }
    /* ************************************************************************************************************************ */
    public Note_Box Xerox_Me_Typed() {
      Note_Box child = null;
      try {
        child = (Note_Box) this.clone();
        child.My_Note = this.My_Note.Xerox_Me();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
    }
    /* ************************************************************************************************************************ */
    public Playable_Drawable Xerox_Me() {
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
  /* ************************************************************************************************************************ */
  public/* virtual */ void Add_Note_Box(Note_Box freshnote) {
  }
  /* ************************************************************************************************************************ */
  public/* virtual */ void Remove_Note_Box(Note_Box gonote) {
  }
  /* ************************************************************************************************************************ */
  public/* virtual */ void Move_Note(Note_Box freshnote) {
    this.remove(freshnote);
    this.Add_Note_Box(freshnote);
  }
  /* ************************************************************************************************************************ */
  public/* virtual */ int Tree_Search(double Time, int minloc, int maxloc) {
    int medloc;
    while (minloc < maxloc) {
      medloc = (minloc + maxloc) >> 1; /* >>1 is same as div 2, only faster. */
      if (Time <= this.get(medloc).Start_Time_G()) {
        maxloc = medloc;
      }/* has to go through here to be found. */ else {
        minloc = medloc + 1;
      }
    }
    return minloc;
  }
  /* ************************************************************************************************************************ */
  public/* virtual */ void Sort_Me() {
    Collections.sort(this, new byStartTime());
  }
  private class byStartTime implements java.util.Comparator {
    @Override
    public int compare(Object n0, Object n1) {
      // int sdif = ((Note_Box) n0).Start_Time_G().CompareTo(((Note_Box) n1).Start_Time_G());
      int sdif = Double.compare(((Note_Box) n0).Start_Time_G(), ((Note_Box) n1).Start_Time_G());
      return sdif;
    }
  }
  /* ************************************************************************************************************************ */
  protected /* virtual */ int Compare(Note_Box n0, Note_Box n1) {
    return Double.compare(n0.Start_Time_G(), n1.Start_Time_G());
  }
  /* ************************************************************************************************************************ */
  protected/* virtual */ double Get_Comparison_Value(Note_Box nbx) {
    return nbx.Start_Time_G();
  }
}
/* ************************************************************************************************************************ */
class Note_Box_List extends Note_List_Base {
  /* ************************************************************************************************************************ */
  @Override
  public void Add_Note_Box(Note_Box freshnote) {
    int dex = Tree_Search(freshnote.End_Time_G(), 0, this.size());// sorted by end time
    this.add(dex, freshnote);
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Remove_Note_Box(Note_Box gonote) {
    this.remove(gonote);
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Move_Note(Note_Box freshnote) {
    this.remove(freshnote);
    this.Add_Note_Box(freshnote);
  }
  /* ************************************************************************************************************************ */
  public Note_Box Get_Last_Released() {// assumes sorted by end time
    if (this.size() == 0) {
      return null;
    }
    return this.get(0);// for sort-descending.
  }
  /* ************************************************************************************************************************ */
  @Override
  public int Tree_Search(double Time, int minloc, int maxloc) {// sorting by end time, sonest first
    int medloc;
    while (minloc < maxloc) {
      medloc = (minloc + maxloc) >> 1; /* >>1 is same as div 2, only faster. */
      if (Time >= this.get(medloc).End_Time_G()) {
        maxloc = medloc;
      }/* has to go through here to be found. */ // for sort-descending.
      //if (Time <= this[medloc].End_Time) { maxloc = medloc; }/* has to go through here to be found. */
      else {
        minloc = medloc + 1;
      }
    }
    return minloc;
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Sort_Me() {// sorting by end time
    //this.Sort(delegate(Note_Box n0, Note_Box n1) { return n0.End_Time.CompareTo(n1.End_Time); });// sort ascending, soonest endtime first in the array.
    Collections.sort(this, new byEndTime());
  }
  /* ************************************************************************************************************************ */
  public int Tree_Search_Test(Note_Box Target_Time, int minloc, int maxloc) {// sorting by end time
    double Time = this.Get_Comparison_Value(Target_Time);
    int medloc;
    while (minloc < maxloc) {
      medloc = (minloc + maxloc) >> 1; /* >>1 is same as div 2, only faster. */
      if (Time <= this.Get_Comparison_Value(this.get(medloc))) {
        maxloc = medloc;
      }/* has to go through here to be found. */ else {
        minloc = medloc + 1;
      }
    }
    return minloc;
  }
  /* ************************************************************************************************************************ */
  @Override
  protected int Compare(Note_Box n0, Note_Box n1) {
    return -Double.compare(n0.End_Time_G(), n1.End_Time_G());
  }
  private class byEndTime implements java.util.Comparator {
    @Override
    public int compare(Object n0, Object n1) {
      return Compare((Note_Box) n0, (Note_Box) n1);
    }
  }
  /* ************************************************************************************************************************ */
  @Override
  protected double Get_Comparison_Value(Note_Box nbx) {
    return -nbx.End_Time_G();
  }
}
