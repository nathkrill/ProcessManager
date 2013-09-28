package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Positioned;


public class DiagramNode<T extends Positioned> implements Positioned {

  private T aTarget;

  private double aX;

  private double aY;

  private final double aLeftExtend;

  private final double aRightExtend;

  private final double aTopExtend;

  private final double aBottomExtend;

  private List<DiagramNode<T>> aLeft;

  private List<DiagramNode<T>> aRight;

  public DiagramNode(T pTarget, double pLeftExtend, double pRightExtend, double pTopExtend, double pBottomExtend) {
    aTarget = pTarget;
    aLeft = new ArrayList<DiagramNode<T>>();
    aRight = new ArrayList<DiagramNode<T>>();
    aX = pTarget.getX();
    aY = pTarget.getY();
    aLeftExtend = pLeftExtend;
    aRightExtend = pRightExtend;
    aTopExtend = pTopExtend;
    aBottomExtend = pBottomExtend;
  }

  private DiagramNode(DiagramNode<T> pDiagramNode, double pX, double pY) {
    aTarget = pDiagramNode.aTarget;
    aX = pX;
    aY = pY;
    aLeftExtend = pDiagramNode.aLeftExtend;
    aRightExtend = pDiagramNode.aRightExtend;
    aTopExtend = pDiagramNode.aTopExtend;
    aBottomExtend = pDiagramNode.aBottomExtend;
  }

  public T getTarget() {
    return aTarget;
  }

  /** Get the size to the left of the gravity point. */
  public double getLeftExtend() {
    return aLeftExtend;
  }

  /** Get the size to the right of the gravity point. */
  public double getRightExtend() {
    return aRightExtend;
  }

  /** Get the size to the top of the gravity point. */
  public double getTopExtend() {
    return aTopExtend;
  }

  /** Get the size to the bottom of the gravity point. */
  public double getBottomExtend() {
    return aBottomExtend;
  }

  public DiagramNode<T> withX(double pX) {
    return new DiagramNode<T>(this, pX, aY);
  }

  public DiagramNode<T> withY(double pY) {
    return new DiagramNode<T>(this, aX, pY);
  }

  public void setX(double pX) {
    aX = pX;
  }

  public void setY(double pY) {
    aY = pY;
  }

  @Override
  public double getX() {
    return aX;
  }

  @Override
  public double getY() {
    return aY;
  }

  public double getLeft() {
    return aX - aLeftExtend;
  }

  public double getRight() {
    return aX + aRightExtend;
  }

  public double getTop() {
    return aY - aTopExtend;
  }

  public double getBottom() {
    return aY + aBottomExtend;
  }

  public boolean rightOverlaps(DiagramNode<T> pRegion, double xSep, double ySep) {
    if (pRegion.getLeft()<(getLeft())) {
      return false;
    }
    if (pRegion.getLeft()>(getRight()+xSep)) {
      return false;
    }
    if (pRegion.getTop()>(getBottom()+ySep)) {
      return false;
    }
    if (pRegion.getBottom()<(getTop()-ySep)) {
      return false;
    }
    return true;
  }

  public boolean downOverlaps(DiagramNode<T> pRegion, double xSep, double ySep) {
    if (pRegion.getRight()<(getLeft()-xSep)) {
      return false;
    }
    if (pRegion.getLeft()>(getRight()+xSep)) {
      return false;
    }
    if (pRegion.getTop()>(getTop())) {
      return false;
    }
    if (pRegion.getBottom()<(getTop()-ySep)) {
      return false;
    }
    return true;
  }

  public List<DiagramNode<T>> getLeftNodes() {
    return aLeft;
  }

  public List<DiagramNode<T>> getRightNodes() {
    return aRight;
  }
}
