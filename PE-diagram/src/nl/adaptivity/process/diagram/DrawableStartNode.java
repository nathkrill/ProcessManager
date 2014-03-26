package nl.adaptivity.process.diagram;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;
import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.StartNode;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode> implements DrawableProcessNode{

  private int aState = STATE_DEFAULT;


  public DrawableStartNode(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableStartNode(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  public DrawableStartNode(DrawableStartNode pOrig) {
    super(pOrig);
    aState = pOrig.aState;
  }

  @Override
  public DrawableStartNode clone() {
    if (getClass()==DrawableStartNode.class) {
      return new DrawableStartNode(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Override
  public Rectangle getBounds() {
    final double hsw = STROKEWIDTH/2;
    return new Rectangle(getX()-STARTNODERADIUS-hsw, getY()-STARTNODERADIUS-hsw, STARTNODERADIUS*2+STROKEWIDTH, STARTNODERADIUS*2+STROKEWIDTH);
  }

  @Override
  public void move(double pX, double pY) {
    setX(getX()+pX);
    setY(getY()+pY);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    final double realradius=STARTNODERADIUS+(STROKEWIDTH/2);
    return ((Math.abs(pX-getX())<=realradius) && (Math.abs(pY-getY())<=realradius)) ? this : null;
  }

  @Override
  public int getState() {
    return aState ;
  }

  @Override
  public void setState(int pState) {
    aState = pState;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    if (hasPos()) {
      final double realradius=STARTNODERADIUS+(STROKEWIDTH/2);
      PEN_T fillPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINEBG, aState & ~STATE_TOUCHED);

      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawCircle(realradius, realradius, STARTNODERADIUS, touchedPen);
      }

      pCanvas.drawFilledCircle(realradius, realradius, realradius, fillPen);
    }
  }

  public static DrawableStartNode from(DrawableProcessModel pOwner, StartNode<?> pN) {
    DrawableStartNode result = new DrawableStartNode(pOwner);
    copyProcessNodeAttrs(pN, result);
    result.getImports().clear();
    result.getImports().addAll(pN.getImports());
    return result;
  }

}
