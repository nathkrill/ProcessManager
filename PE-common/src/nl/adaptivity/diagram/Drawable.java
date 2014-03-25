package nl.adaptivity.diagram;

public interface Drawable extends Bounded {

  static final int STATE_DEFAULT=0x0;
  static final int STATE_TOUCHED=0x1;
  static final int STATE_SELECTED=0x2;
  static final int STATE_FOCUSSED=0x4;
  static final int STATE_DISABLED=0x8;
  static final int STATE_CUSTOM1=0x10;
  static final int STATE_CUSTOM2=0x20;
  static final int STATE_CUSTOM3=0x40;
  static final int STATE_CUSTOM4=0x80;
  static final int STATE_DRAG=0x100;
  static final int STATE_MASK=0xffff;


  /**
   * Draw the drawable to the given canvas. The drawing will use a top left of (0,0).
   * The canvas will translate coordinates.
   * @param pCanvas The canvas to draw on.
   * @param pClipBounds The part of the drawing to draw. Outside no drawing is needed.
   */
  <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds);

  @Override
  public Drawable getItemAt(double pX, double pY);

  /**
   * Get the current state of the drawable. Individual implementations should specify what each state value means.
   * The <code>0</code> value however means the default.
   * @return The current state of the drawable.
   */
  public int getState();

  /**
   * Set the current state of the drawable.
   * @param pState
   */
  public void setState(int pState);
}
