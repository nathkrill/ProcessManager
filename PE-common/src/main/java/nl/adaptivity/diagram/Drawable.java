package nl.adaptivity.diagram;

public interface Drawable extends Bounded, Cloneable {

  int STATE_DEFAULT=0x0;
  int STATE_TOUCHED=0x1;
  int STATE_SELECTED=0x2;
  int STATE_FOCUSSED=0x4;
  int STATE_DISABLED=0x8;
  int STATE_CUSTOM1=0x10;
  int STATE_CUSTOM2=0x20;
  int STATE_CUSTOM3=0x40;
  int STATE_CUSTOM4=0x80;
  int STATE_DRAG=0x100;
  int STATE_ACTIVE=0x200;
  int STATE_MASK=0xffff;


  /**
   * Draw the drawable to the given canvas. The drawing will use a top left of (0,0).
   * The canvas will translate coordinates.
   * @param pCanvas The canvas to draw on.
   * @param pClipBounds The part of the drawing to draw. Outside no drawing is needed.
   */
  <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds);

  @Override
  Drawable getItemAt(double pX, double pY);

  /**
   * Override the definition of {@link Object#clone()} to ensure the right
   * return type and make it public.
   *
   * @return A copy.
   */
  Drawable clone();

  /**
   * Get the current state of the drawable. Individual implementations should specify what each state value means.
   * The <code>0</code> value however means the default.
   * @return The current state of the drawable.
   */
  int getState();

  /**
   * Set the current state of the drawable.
   * @param pState
   */
  void setState(int pState);


  void move(double pX, double pY);

  void setPos(double pLeft, double pTop);

}
