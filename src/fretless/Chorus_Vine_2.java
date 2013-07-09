/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fretless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.Color;
import java.util.*;
import java.util.ArrayList;

import java.awt.geom.*;
import fretless.TunePadLogic.*;

/*
 Chorus_Vine is a really important playable type.  It handles all the grouping of all other playable objects.

 It is similar to to grouping in vector art, but all the contents must always be sorted by time.  
 So everything in the group hangs from of a single weaving time line.

 */

/* ************************************************************************************************************************ */
public class Chorus_Vine_2 extends Note_List_Base_2 implements Wave.IDropBox {/* .Playable_Drawable */

  double octave, frequency;
  public String MyName = "None";
  Dictionary<Wave.IPlayable, Note_Box> backlist;// maps playable children back to their note_box containers
    /* ************************************************************************************************************************ */
  public Chorus_Vine_2() {
    this.Loudness_S(0.0, 1.0);
    this.Loudness_S(1.0, 1.0);
    backlist = new Hashtable<Wave.IPlayable, Note_Box>();
  }
  /* ************************************************************************************************************************ */
  public void Add_Note(Wave.IPlayable freshnote, double Time, double Pitch) {
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
    backlist = new Hashtable<>();
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
  public double End_Time_G() {
    return this.Start_Time_G() + this.Duration_G();
  }
  /* ************************************************************************************************************************ */
  //#region Playable Members
  double Radius = 5;
  double Diameter = Radius * 2.0;
  @Override
  public Boolean Hit_Test_Stack(Wave.Drawing_Context dc, double Xloc, double Yloc, int Depth, Wave.Hit_Stack Stack) {
    /* Chorus_Vine  */
    Point2D scrpnt = dc.To_Screen(dc.Absolute_XForm.Start_Time, dc.Absolute_XForm.Octave);
    // look for child hits first
    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.size(); cnt++) {
      Wave.Transformer TChild = this.get(cnt);
      if (TChild.Hit_Test_Stack(dc, Xloc, Yloc, Child_Depth, Stack)) {
        found = true;
        break;
      }
    }
    if (!found) {/* child hit preempts hitting me */
      if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
        Stack.Init(Depth + 1);// terminate stack
        //this.Diameter = 2.0 * (this.Radius = 10.0);
        found = true;
      }
    }
    return found;
  }
  /* ************************************************************************************************************************ */
  @Override
  public Boolean Hit_Test_Container(Wave.Drawing_Context dc, double Xloc, double Yloc, int Depth, Wave.Target_Container_Stack Stack) {
    /* Chorus_Vine  */
    Point2D scrpnt = dc.To_Screen(dc.Absolute_XForm.Start_Time, dc.Absolute_XForm.Octave);
    double Line_Radius = 5.0;
    Boolean linehit = false;
    Boolean found = false;
    int Child_Depth = Depth + 1;
    for (int cnt = 0; cnt < this.size(); cnt++) {
      Wave.Transformer TChild = this.get(cnt);
      if (TChild.Hit_Test_Container(dc, Xloc, Yloc, Child_Depth, Stack)) {
        found = true;
        break;
      }
    }
    if (found) {/* child hit preempts hitting me */

    } else if (Math.hypot(Xloc - scrpnt.getX(), Yloc - scrpnt.getY()) <= this.Radius) {
      Stack.Init(Depth + 1);/* we hit me directly */
      Stack.DropBox_Found = this;
      found = true;
    } else {
      // look for line hit here.
      {
        int xprev, yprev;

        Point2D.Double pnt = new Point2D.Double(Xloc, Yloc);

        Line2D.Double lin = new Line2D.Double();
        Point2D.Double hitpnt = new Point2D.Double();

        Point2D.Double loc = dc.To_Screen(dc.Absolute_XForm.Start_Time, dc.Absolute_XForm.Octave);

        xprev = (int) loc.x;
        yprev = (int) loc.y;

        for (int cnt = 0; cnt < this.size(); cnt++) {
          Note_Box child = this.get(cnt);

          loc = dc.To_Screen(dc.Absolute_XForm.Start_Time + child.Start_Time_G(), (dc.Absolute_XForm.Octave + child.Octave_G()));

          lin.setLine(xprev, yprev, loc.x, loc.y);
          linehit = TunePadLogic.Hit_Test_Line(pnt, lin, Line_Radius, hitpnt);
          if (linehit) {
            Stack.Init(Depth + 1);
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
  public double Octave_G() {
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
  public void Draw_Me(Wave.Drawing_Context dc) {/* Drawable */
    /* Chorus_Vine  */
    Wave.Drawing_Context mydc = new Wave.Drawing_Context(dc, this);
    mydc.gr.setColor(Color.blue);

    double clipstart = dc.Clip_Start;
    double clipend = dc.Clip_End;

    Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_XForm.Start_Time, mydc.Absolute_XForm.Octave);
    mydc.gr.fillOval((int) (pnt.x) - (int) Radius, (int) (pnt.y) - (int) Radius, (int) Diameter, (int) Diameter);

    //mydc.gr.fillOval((int) (mydc.Absolute_X * xscale) - 5, (int) (mydc.Absolute_Y * yscale) - 5, 10, 10);
    int xprev, yprev, xloc, yloc;

    Point2D.Double loc = mydc.To_Screen(mydc.Absolute_XForm.Start_Time, mydc.Absolute_XForm.Octave);

    xprev = (int) loc.x;
    yprev = (int) loc.y;

    for (int cnt = 0; cnt < this.size(); cnt++) {
      Note_Box child = this.get(cnt);
      child.Draw_Me(mydc);
      loc = mydc.To_Screen(mydc.Absolute_XForm.Start_Time + child.Start_Time_G(), (mydc.Absolute_XForm.Octave + child.Octave_G()));
      mydc.gr.setColor(Color.black);
      mydc.gr.drawLine(xprev, yprev, (int) loc.x, (int) loc.y);
      xprev = (int) loc.x;
      yprev = (int) loc.y;
    }
  }
  /* ************************************************************************************************************************ */
  public Chorus_Vine_2 Xerox_Me_Typed() {
    Chorus_Vine_2 child = null;
    child = (Chorus_Vine_2) this.clone();
    child.Remove_All_Notes();
    for (int cnt = 0; cnt < this.size(); cnt++) {
      Note_Box subnote = this.get(cnt).Xerox_Me_Typed();
      child.Add_Note(subnote.MyPlayable, subnote.Start_Time_G(), subnote.Octave_G());
    }
    return child;
  }
  /* ************************************************************************************************************************ */
  @Override
  public Wave.IPlayable Xerox_Me() {
    return Xerox_Me_Typed();
  }
  /* ************************************************************************************************************************ */
  @Override
  public void Container_Insert(Wave.Playable NewChild, double Time, double Pitch) {
    this.Add_Note(NewChild, Time, Pitch);
  }
  @Override
  public ArrayList<Wave.Transformer> Get_Parents() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  @Override
  public Wave.CursorBase Launch_Cursor(Wave.Render_Context rc) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  @Override
  public Wave.CursorBase Launch_Cursor(Wave.Render_Context rc, double t0) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  @Override
  public Wave.CursorBase Launch_Cursor(Wave.Drawing_Context dc) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
/* ************************************************************************************************************************ */
class Note_List_Base_2 extends ArrayList<Note_List_Base_2.Note_Box> {
  /* ************************************************************************************************************************ */
  public static class Note_Box extends Wave.Transformer {/* Note_Box exists to provide a local offset origin to its contents, and to link back to its list container. */

    //public Playable_Drawable My_Note;
    double octave, frequency;
    private Note_Box_List_2 My_Overlaps;/* should my note be a member of my overlaps?  then we can just play them as a group. */

    public String MyName;
    /* ************************************************************************************************************************ */
    public Note_Box() {
      My_Overlaps = new Note_Box_List_2();
      Loudness_Scale_S(1.0);
    }
    /* ************************************************************************************************************************ */
    public Note_Box(Wave.IPlayable freshnote) {
      this();
      this.MyPlayable = freshnote;
    }
    /* ************************************************************************************************************************ */
    public Note_Box(Wave.IPlayable freshnote, double Time, double Pitch) {
      this(freshnote);
      this.Start_Time_S(Time);
      this.Octave_S(Pitch);
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
    @Override
    public Boolean Hit_Test_Stack(Wave.Drawing_Context dc, double Xloc, double Yloc, int Depth, Wave.Hit_Stack Stack) {
      /* Note_Box  */
      Wave.Drawing_Context mydc = new Wave.Drawing_Context(dc, this);
      Point2D scrpnt = mydc.To_Screen(mydc.Absolute_XForm.Start_Time, mydc.Absolute_XForm.Octave);

      Boolean found = this.MyPlayable.Hit_Test_Stack(mydc, Xloc, Yloc, Depth + 1, Stack);
      if (found) {
        Stack.Set(Depth, this);
      }
      return found;
    }
    /* ************************************************************************************************************************ */
    @Override
    public Boolean Hit_Test_Container(Wave.Drawing_Context dc, double Xloc, double Yloc, int Depth, Wave.Target_Container_Stack Stack) {
      /* Note_Box  */
      Wave.Drawing_Context mydc = new Wave.Drawing_Context(dc, this);
      Boolean found = this.MyPlayable.Hit_Test_Container(mydc, Xloc, Yloc, Depth + 1, Stack);
      if (found) {
        Stack.Set(Depth, this);
      }
      return found;
    }
    /* ************************************************************************************************************************ */
    @Override
    public void Octave_S(double Fresh_Octave) {
      this.octave = Fresh_Octave;
      this.frequency = TunePadLogic.Octave_To_Frequency(Fresh_Octave);
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
    public double Octave_G() {
      return octave;
    }
    @Override
    public double Get_Max_Amplitude() {
      return 1.0;
    }
    double Loudness_Value_0, Loudness_Value_1;
    @Override
    public double Loudness_Scale_G() {
      return Loudness_Value_0;
    }
    @Override
    public double Loudness_Scale_S(double value) {
      Loudness_Value_0 = value;
      return value;
    }
    @Override
    public void Render_Audio(Wave.Render_Context rc, TunePadLogic.Wave_Carrier Wave) {
      Wave.Render_Context LocalRC = new Wave.Render_Context(rc);
      LocalRC.Add_Transpose(this.Start_Time_G(), this.octave);
      Wave.CursorBase cb = this.MyPlayable.Launch_Cursor(LocalRC);
      //       .Render_Audio(LocalRC, Wave);
      TunePadLogic.Delete(LocalRC);
    }
    //#endregion
    // Drawable interface
    @Override
    public void Draw_Me(Wave.Drawing_Context dc) {// Drawable
      /* Note_Box  */
      Wave.Drawing_Context mydc = new Wave.Drawing_Context(dc, this);
      dc.gr.setColor(Color.green);
      // Point2D.Double pnt = mydc.To_Screen(this.Start_Time_G(),this.Octave_G());
      Point2D.Double pnt = mydc.To_Screen(mydc.Absolute_XForm.Start_Time, mydc.Absolute_XForm.Octave);
      mydc.gr.fillOval((int) (pnt.x) - 5, (int) (pnt.y) - 5, 10, 10);
      //mydc.gr.fillOval((int) (mydc.Absolute_X * xscale) - 5, (int) (mydc.Absolute_Y * yscale) - 5, 10, 10);
      Wave.CursorBase cb = this.MyPlayable.Launch_Cursor(dc);
      cb.Draw_Next_Chunk(dc);
    }
    /* ************************************************************************************************************************ */
    public Note_Box Xerox_Me_Typed() {
      Note_Box child = null;
      try {
        child = (Note_Box) this.clone();
        child.MyPlayable = this.MyPlayable.Xerox_Me();
      } catch (CloneNotSupportedException ex) {
        Logger.getLogger(TunePadLogic.class.getName()).log(Level.SEVERE, null, ex);
      }
      return child;
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
class Note_Box_List_2 extends Note_List_Base_2 {
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
