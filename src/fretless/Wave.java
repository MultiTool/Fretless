package fretless;

import fretless.TunePadLogic.Transformer;
import java.util.ArrayList;

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
    public ArrayList<TunePadLogic.Transformer> Parents;
    public Playable() {
      Parents = new ArrayList<>();
    }
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
  public class Group extends Playable {
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
}
