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

import java.awt.geom.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.*;
// import tunepad.TunePadLogic.DropBox;
// import tunepad.TunePadLogic.Playable_Drawable;
import tunepad.TunePadLogic.*;

/*
import javax.media.opengl.GL;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.media.opengl.GLCanvas;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.sun.opengl.util.Animator;
 */
/**
 *
 * @author JCAtkeson
 */
/* *************************************************************************************************** */
public class Drawing_Canvas extends javax.swing.JPanel {/* JFrame */
  /* *************************************************************************************************** */

  TunePadLogic tpl;
  TunePadLogic.Root_Playable rootplay;
  TunePadLogic.Playable_Drawable Selected;
  Drifter_Box Selected_Context;
  TunePadLogic.Hit_Stack Stack;
  TunePadLogic.Target_Container_Stack Target_Stack;
  TunePadLogic.Drawing_Context Main_DC;
  BufferedImage Buffer;
  Graphics2D GlobalGraphics;
  public Drawing_Canvas() {
    this.setName("Drawing_Canvas");
    {
      tpl = new TunePadLogic();
      rootplay = tpl.rootplay;
      Stack = new TunePadLogic.Hit_Stack();
      Target_Stack = tpl.new Target_Container_Stack();
      Selected_Context = new Drifter_Box();
    }
    this.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        Drawing_CanvasMouseClicked(evt);
      }
      @Override
      public void mousePressed(java.awt.event.MouseEvent evt) {
        Drawing_CanvasMousePressed(evt);
      }
      @Override
      public void mouseReleased(java.awt.event.MouseEvent evt) {
        Drawing_CanvasMouseReleased(evt);
      }
    });
    this.addMouseMotionListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseDragged(java.awt.event.MouseEvent evt) {
        Drawing_CanvasMouseDragged(evt);
      }
      @Override
      public void mouseMoved(java.awt.event.MouseEvent evt) {
      }
    });
    /*
    MouseListener ml = new MouseAdapter(){
    public void mousePressed(MouseEvent e){
    JComponent jc = (JComponent)e.getSource();
    TransferHandler th = jc.getTransferHandler();
    th.exportAsDrag(jc, e, TransferHandler.COPY);
    }
    };
    
     */
    this.addComponentListener(new ComponentListener() {
      @Override
      public void componentResized(ComponentEvent evt) {/* This method is called after the component's size changes */
        Component c = (Component) evt.getSource();
        Dimension newSize = c.getSize();/* Get new size */
        Buffer = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_INT_ARGB);
        GlobalGraphics = Buffer.createGraphics();
        Plot_Tree(GlobalGraphics);
      }
      @Override
      public void componentMoved(ComponentEvent e) {
      }
      @Override
      public void componentShown(ComponentEvent e) {
      }
      @Override
      public void componentHidden(ComponentEvent e) {
      }
    });
  }
  /* *************************************************************************************************** */
  @Override
  public void setSize(int width, int height) {
    System.out.println("setSize");
  }
  /* *************************************************************************************************** */
  private void Drawing_CanvasMouseClicked(java.awt.event.MouseEvent evt) {// TODO add your handling code here:
  }
  int mousex = 0, mousey = 0;
  /* *************************************************************************************************** */
  private void Drawing_CanvasMousePressed(java.awt.event.MouseEvent evt) {// TODO add your handling code here:
    double xloc = evt.getX();
    double yloc = evt.getY();
    Stack.Clear();
    rootplay.Hit_Test_Stack(Main_DC, xloc, yloc, 0, Stack);
    if (Stack != null) {
      if (Stack.Stack_Depth > 0) {// if something has been picked and is being dragged around.
        Selected = Stack.End();
        Selected_Context.Content = Selected;
        TunePadLogic.Render_Context rc = new TunePadLogic.Render_Context();
        Stack.Create_Audio_Transform(rc);
        /*
        what we really want here is to create the transform of the selection context box.
        but we must also update that transform continuously as we drag.
        also as we drag, we must do a continuous container search - other problem.
        
        when dropping, if we hit a container then join it however.
        
        if dropping in space?  either revert the whole structure, or drop in space to invisible root.  in that case we'd just throw the selected box into the root too.  maybe clone it first.
        
        and yes, we need clone for all playables.  
        
         */
      }
    }

    /*
     * here we grab the object.  do we move only on mouse release, or on every mouse move?
     *
     * safer to only remove and insert the object on mouse release, but in the long run we will have to animate the dragging process.
     *
     * the first instance the mouse is dragged, we break it free of the tree.
     *
     * then animate if need be.  - rapid removal, insertion, and repainting will work if we use the right mutex and the machine is fast enough.
     *
     *
     * but first, on mouse release, we need a way to find the place in the song we hit.
     * that means returning on wires as well as vine dots.
     * really vine dots will just count as T0 on the first wire.
     *
     * So we will need a wire search, and or general container search for all drop-downs.  if they drop in empty space the object just reverts to its starting place.
     *
     */
  }
  /* *************************************************************************************************** */
  private void Drawing_CanvasMouseDragged(java.awt.event.MouseEvent evt) {// TODO add your handling code here:
    boolean hit = false;/* Future drag and drop support */
    if (Stack != null) {
      if (Stack.Stack_Depth > 0) {// if something has been picked and is being dragged around.

        mousex = evt.getX();
        mousey = evt.getY();
        TunePadLogic.Playable_Drawable drifter = Selected;/* Stack.End(); */
        //TunePadLogic.Drawing_Context dc = new TunePadLogic.Drawing_Context(GlobalGraphics);
        Drawing_Context dc = new Drawing_Context(Main_DC);
        Drawing_Context Create_Drawing_Transform = Stack.Create_Drawing_Transform(dc);

        {
          Point2D.Double ps = dc.From_Screen(mousex, mousey);
          Selected_Context.Start_Time_S(ps.x);
          Selected_Context.Set_Octave(ps.y);

          dc.Absolute_X = ps.x;
          dc.Absolute_Y = ps.y;
          Plot_Tree(GlobalGraphics);
          drifter.Draw_Me(dc);
          repaint();
        }

        Target_Stack.Clear();
        Target_Stack.Exclude = Stack.End();/* do not insert into self */
        hit = rootplay.Hit_Test_Container(Main_DC, mousex, mousey, 0, Target_Stack);
        if (hit) {
          boolean nop = true;
          JComponent jc = (JComponent) evt.getSource();
          TransferHandler th = jc.getTransferHandler();
          // th.exportAsDrag(jc, evt, TransferHandler.COPY);
          if (Target_Stack != null) {
            if (Target_Stack.Stack_Depth > 0) {// if something has been hit
              nop = true;
              /* just wanna animate */
            }
          }
        }
        /* the object we are dragging will be whatever is at the end of the stack.
         * we will drag it out of its old stack and into a new context.
         *
         * animate?  yes probably first.
         * first just animate a moving ball.
         *
         * the hard part comes when we decide where to drop it.
         *
         * well, first we figure out what kind of thing the ball is when we click on it.
         * after that, the ball can look at all the nearby objects we feed to it.  
         * if the object is a real place to go, then the ball returns true and our mouse method asks the target to highlight itself.
         * the highlighted target can be passed over and un-highlighted, so the 'current target' is a global object we can change and forget with ball movement.
         *
         * maybe even the ball itself can keep a pointer to the current target?  we would need a box object to hold both the playable and the target.
         *
         *
         * oh wait, the other hard part comes in deciding what it was we selected.  things we can select:
         * playable root - uses stack
         * loudness envelope handle - probably need stack anyway to play.
         * bendable tail? - probably stack also
         *
         * so one approach:
         * all selectables are descended from interface 'selectable' or Music_Thing, and when we drag we type-check and cast as needed.
         * or, we have 3 global variables, and all are set
         *
         * hmm, so hitting always returns a stack.  all that changes with different objects is the very tail end of the stack.
         *
         * lets focus on ampvelopes.  each handle points to its playable owner, ok?  so draw me redraws the whole owner I guess.  could also redraw just my slice,
         * but that would take time parameters for redraw.  (maybe that is a good idea for all drawables).
         *   what about replaying?  replay the whole owning playable?  or if not, then what part?  replay whatever the ampvelope covers I guess.  again a time range.
         *
         * all ampvelopes are trapezoids, or pairs of triangles.  all have a range.
         *
         * so what?  when we select or drag, we always get a stack.  perhaps the end of the stack has a 'selectable' pointer?
         *
         * or, maybe an ampvelope handle IS a playable/drawable.  it just plays/draws its own parent, in its own range.
         *
         * but, can we relocate an ampvelope?  within its own playable, yes.
         *
         * so really the only difference is in where they can land.  a draggable can test any potential target for dropability.
         *
         * 
         *
         * first part of dragging is the animation.  drag just a stock ball.
         * second part of dragging is to find the target.  in this case it is only one thing, a container.
         *
         * does finding the target mean returning a stack for the target?  probably.  but we dont just find the target identity.
         * we must also find the location within the target to which we are closest.
         * 
         *
         * so we have target_stack extends stack, also has some xy vars, or what?  xy vars inside a single container are enough.  (proove this).
         *
         *
         */
      }
    }
    /* rootplay.Hit_Test_Stack(Main_DC, xloc, yloc, 0, Stack); */
  }
  /* *************************************************************************************************** */
  private void Drawing_CanvasMouseReleased(java.awt.event.MouseEvent evt) {
    double hgt = this.getHeight();

    Point2D.Double pnt;
    mousex = evt.getX();
    mousey = evt.getY();
    double xloc = evt.getX();
    double yloc = evt.getY();

    pnt = Main_DC.From_Screen(evt.getX(), evt.getY());

    // TunePadLogic.Wave_Carrier wave = tpl.new Wave_Carrier();  Stack.Render_Audio(wave);
    // now we need some kind of threaded Stack.Play();
    if (Selected != null) {
      // th.exportAsDrag(jc, evt, TransferHandler.COPY);
      if (Target_Stack != null) {
        if (Target_Stack.Stack_Depth > 0) {// if something has been hit
          boolean nop = true;
          /* drop it here */
          Playable_Drawable End = Target_Stack.End();
          DropBox Grabber = Target_Stack.DropBox_Found;

          /*
          we want the coords local to the target?  internal coords?  
          
          and once again, do we really want to have multiple parents for one node?
          get rid of that, and everyone has their own coords, and stateful rendering becomes easy.
          
          we need the 
          
           */
          Render_Context rc = new Render_Context();
          Target_Stack.Create_Audio_Transform(rc);/* make transform between target container and screen. */

          Drawing_Context dc = new Drawing_Context(Main_DC);
          Target_Stack.Create_Drawing_Transform(dc);/* make transform between target container and screen. */

          Point2D.Double innerpnt;
          innerpnt = dc.From_Screen(evt.getX(), evt.getY());

          Grabber.Container_Insert(Selected, innerpnt.x, innerpnt.y);
        }
      }
      Selected = null;
      Selected_Context.Content = null;
    }
    Target_Stack.Clear();
    Stack.Clear();
    rootplay.Hit_Test_Stack(Main_DC, xloc, yloc, 0, Stack);

    /*
    mow we need a way to render the audio of a stack leaf.
     * first go down to the end of the stack, adding up the render_context values.  rc will include clipping.
     *
     * then, at the bottom, generate the wave.  then just return it!  so going to the end of the stack is just a loop.  no recursion.
     *
     * one key point: a stack can only render the object that is at the end of it, from the start of that object.
     *
     * the alternative is to just make a full time slice based on where we clicked (not hit test), and render that subrange of the whole song.
     *
     * for now, hit testing feels better.  and the render goes to the end of the leaf hiet?  or until the music is interrupted.
     *
     *
     * then comes the big job:  MOVING A NOTE, or moving a whole playable.
     * it is 100% just a graphics and topology issue until we let go, then it is 100% an audio issue.  never both at once, yes?
    
     * so we:
     * hit test, find object
     * drag object, refreshing image and topology.  does topology change our stack?
     *
     *
     */
    double pitch = pnt.y;// (hgt - evt.getY()) / hgt;
    double octave_offset = 7.0;
    double fo_220 = 7.7813597135246608;// - octave_offset;
    double fo_440 = 8.78135971352466;// - octave_offset;
    // turn octave into frequency
    //pitch /= 400.0;// scale for window size
    double frequency = Math.pow(2.0, (fo_440 - 1.0) + pitch);// convert octave to frequency
    Stack_Sounder th = new Stack_Sounder();
    //th.pitch = frequency; th.YTranspose = pitch;
    th.start();
    this.repaint();
  }
  /* *************************************************************************************************** */
  public class Stack_Sounder extends Thread {
    @Override
    public void run() {
      Sing_Stack();
    }
  }
  /* *************************************************************************************************** */
  public class Sounder extends Thread {
    public double pitch = 0;
    public double YTranspose = 0;
    @Override
    public void run() {
      Sing1(YTranspose);
      //Sing(pitch);// http://java.sun.com/docs/books/tutorial/uiswing/examples/dnd/DropDemoProject/src/dnd/DropDemo.java
    }
  }
  /*
   * first we create the whole sing in the initializer.
   * the root to that is global.
   *
   * next, on click, we render a half(?) second.
   * then, as soon as that is rendered, we pass it to a duplicate of Sing() below.  Only it will take a wave as an input.
   *
   * We see about latentcy, and choppiness.
   *
   */
  /* ************************************************************************************************************************ */
  public void Sing_To_File(TunePadLogic.Wave_Carrier wc, String FilePath) {
    TunePadLogic.WaveDouble outbuf = new TunePadLogic.WaveDouble();
    TunePadLogic.Transfer_To_Array(wc, outbuf);
    for (int scnt = 0; scnt < outbuf.numsamples; scnt++) {
      outbuf.wave[scnt] *= 0.75;// arbitrary scaling to match C# code
    }
    StdAudio.save(FilePath, outbuf.wave);
  }
  SourceDataLine source = null;
  /* *************************************************************************************************** */
  public void Sing_Stack() {
    int sampleRate = 8000;
    sampleRate = 44100;
    int bitspersample = 8;
    bitspersample = 16;
    int bytespersample = bitspersample / 8;
    double halfsample = 127.0;
    halfsample = 32767.0;
    double seconds = 2.0;
    TunePadLogic.Wave_Carrier wc = new TunePadLogic.Wave_Carrier();
    Stack.Render_Audio(wc);
    if (wc == null) {
      return;
    }
    if (wc.WaveForm == null) {
      return;
    }
    seconds = ((double) wc.WaveForm.length) / (double) sampleRate;
    //Sing_To_File(wc, "click.wav");

    if (source != null) {
      source.write(new byte[0], 0, 0);
      source.stop();
      source.close();
    }
    try {
      AudioFormat af = new AudioFormat((float) sampleRate, bitspersample, 1, true, true);

      DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
      source = (SourceDataLine) AudioSystem.getLine(info);
      source.open(af);
      source.start();
      byte[] buf = new byte[(int) (sampleRate * seconds * bytespersample)];
      double damp = 0.5;//0.75
      double amplitude = 1.0;
      int iamp;
      int bufcnt = 0;
      for (int i = 0; i < wc.WaveForm.length; i++) {
        amplitude = (wc.WaveForm[i] * damp * halfsample);
        iamp = (int) amplitude;
        buf[bufcnt] = (byte) (iamp >> 8);
        bufcnt++;
        buf[bufcnt] = (byte) (iamp & 0xff);
        bufcnt++;
      }
      source.write(buf, 0, buf.length);
      source.drain();
      source.stop();
      source.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  /* *************************************************************************************************** */
  public void Sing1(double YTranspose) {
    int sampleRate = 8000;
    sampleRate = 44100;
    int bitspersample = 8;
    bitspersample = 16;
    int bytespersample = bitspersample / 8;
    double halfsample = 127.0;
    halfsample = 32767.0;
    double seconds = 2.0;
    TunePadLogic.Render_Context rc = new TunePadLogic.Render_Context();
    rc.Absolute_YTranspose = 0.3;
    rc.Absolute_YTranspose = 1.0;
    rc.Absolute_YTranspose = 0.0;
    rc.Absolute_YTranspose = YTranspose;
    rc.Sample_Rate = sampleRate;
    rc.Clip_Time_Start = 0.0;
    rc.Clip_Time_End = seconds;// 0.5;// rootplay.Duration_G();
    //rc.Clip_Time_End = rootplay.Duration_G();
    //rc.Clip_Time_End = 1.0;// rootplay.Duration_G();
    TunePadLogic.Wave_Carrier wc = new TunePadLogic.Wave_Carrier();
    rootplay.Render_Audio(rc, wc);
    double RAD = 2.0 * Math.PI;
    if (source != null) {
      source.write(new byte[0], 0, 0);
      source.stop();
      source.close();
    }
    try {
      AudioFormat af = new AudioFormat((float) sampleRate, bitspersample, 1, true, true);

      DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
      source = (SourceDataLine) AudioSystem.getLine(info);
      source.open(af);
      source.start();
      byte[] buf = new byte[(int) (sampleRate * seconds * bytespersample)];
      double damp = 0.5;//0.75
      double amplitude = 1.0;
      int iamp;
      int bufcnt = 0;
      for (int i = 0; i < wc.WaveForm.length; i++) {
        amplitude = (wc.WaveForm[i] * damp * halfsample);
        iamp = (int) amplitude;
        buf[bufcnt] = (byte) (iamp >> 8);
        bufcnt++;
        buf[bufcnt] = (byte) (iamp & 0xff);
        bufcnt++;
      }
      source.write(buf, 0, buf.length);
      source.drain();
      source.stop();
      source.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  /* *************************************************************************************************** */
  public void Sing(double pitch) {// http://mindprod.com/jgloss/sound.html#SYNTHESISED
    double seconds = 0.5;
    int sampleRate = 8000;
    double frequency;// = 1000.0 + pitch;
    //frequency = 440.0 + pitch;
    //frequency = 220.0 + pitch;
    frequency = pitch;

    double RAD = 2.0 * Math.PI;
    if (source != null) {
      source.write(new byte[0], 0, 0);
      source.stop();
      source.close();
    }
    try {
      AudioFormat af = new AudioFormat((float) sampleRate, 8, 1, true, true);
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
      source = (SourceDataLine) AudioSystem.getLine(info);
      source.open(af);
      source.start();
      byte[] buf = new byte[(int) (sampleRate * seconds)];
      double amplitude = 1.0;
      for (int i = 0; i < buf.length; i++) {
        amplitude = 1.0 - ((double) i) / (double) buf.length;
        buf[i] = (byte) (Math.sin(RAD * frequency / sampleRate * i) * amplitude * 127.0);
      }
      source.write(buf, 0, buf.length);
      source.drain();
      source.stop();
      source.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  /* *************************************************************************************************** */
  public void Plot_Tree(Graphics2D g2d) {
    ColorMe(g2d);
    {
      Main_DC = new TunePadLogic.Drawing_Context(g2d);
      //TunePadLogic.Drawing_Context dc = tpl.new Drawing_Context(g2);
      Main_DC.Absolute_X = 0.0;
      Main_DC.Absolute_Y = 0.0;
      Main_DC.Trans_X = 50;
      Main_DC.Trans_Y = this.getHeight();
      rootplay.Draw_Me(Main_DC);
      {
        Main_DC.gr.setColor(Color.red);
        Main_DC.gr.drawRect(mousex, mousey, 20, 20);
      }
      //Selected.Draw_Me(dc); will need to enclose selected in note box.
    }
  }
  /* *************************************************************************************************** */
  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.drawImage(this.Buffer, null, this);
    //http://www.realapplets.com/tutorial/DoubleBuffering.html
    /* http://download.oracle.com/javase/tutorial/2d/images/drawonimage.html */
  }
  /* *************************************************************************************************** */
  @Override
  public void update(Graphics g) {
    paint(g);
  }
  /* *************************************************************************************************** */
  public void ColorMe(Graphics2D g2) {
    Dimension d = this.getSize();
    GradientPaint gradient;
    Color startColor = new Color(1.0f, 0.0f, 0.0f);//Color startColor = Color.red;
    startColor = new Color(1.0f, 1.0f, 0.0f);//Color startColor = Color.red;
    Color endColor = new Color(0.0f, 0.0f, 1.0f);//Color endColor = Color.blue;

    gradient = new GradientPaint(0, 0, startColor, 0, d.height, endColor);// A non-cyclic gradient
    g2.setPaint(gradient);
    g2.fillRect(0, 0, d.width, d.height);
  }
  /* *************************************************************************************************** */
  public class Box_Mapper {
    double realleft, realright, realwdt;
    double realtop, realbottom, realhgt;
    double scrleft, scrright, scrwdt;
    double scrtop, scrbottom, scrhgt;
    double x_ratio_to_screen, y_ratio_to_screen;
    Box_Mapper() {
      Set_Screen(0, 0, 1, 1);// default initializations.
      Set_Real(0, 0, 1, 1);
    }
    public class PointF {
      public double x, y;
    }
    //-------------------------------------------------------------------
    void Set_Screen(double xleft, double ytop, double xright, double ybottom) {
      scrleft = xleft;
      scrright = xright;
      scrtop = ytop;
      scrbottom = ybottom;
      scrwdt = xright - xleft;
      scrhgt = ybottom - ytop;
      set_ratios();
    }
    //-------------------------------------------------------------------
    void Set_Real(double xleft, double ytop, double xright, double ybottom) {
      realleft = xleft;
      realright = xright;
      realtop = ytop;
      realbottom = ybottom;
      realwdt = xright - xleft;
      realhgt = ybottom - ytop;
      set_ratios();
    }
    //-------------------------------------------------------------------
    void set_ratios() {
      x_ratio_to_screen = scrwdt / realwdt;
      y_ratio_to_screen = scrhgt / realhgt;
    }
    //-------------------------------------------------------------------
    double Map_X_To_Screen(double xreal) {
      return scrleft + ((xreal - realleft) * x_ratio_to_screen);
    }
    //-------------------------------------------------------------------
    double Map_Y_To_Screen(double yreal) {
      return scrtop + ((yreal - realtop) * y_ratio_to_screen);
    }
    //-------------------------------------------------------------------
    double Map_X_To_Real(double xscr) {
      return realleft + ((xscr - scrleft) / x_ratio_to_screen);
    }
    //-------------------------------------------------------------------
    double Map_Y_To_Real(double yscr) {
      return realtop + ((yscr - scrtop) / y_ratio_to_screen);
    }
    //-------------------------------------------------------------------
    void Map_To_Screen(double xreal, double yreal, PointF scr) {// double xscr, double yscr) {
      scr.x = Map_X_To_Screen(xreal);
      scr.y = Map_Y_To_Screen(yreal);
    }
    //-------------------------------------------------------------------
    void Map_To_Real(double xscr, double yscr, PointF real) {
      real.x = Map_X_To_Real(xscr);
      real.y = Map_Y_To_Real(yscr);
    }
  }
  /* *************************************************************************************************** */
  public interface Draggable {
    public boolean Valid_Destination(TunePadLogic.Playable_Drawable target);
  }
  /* *************************************************************************************************** */
}
