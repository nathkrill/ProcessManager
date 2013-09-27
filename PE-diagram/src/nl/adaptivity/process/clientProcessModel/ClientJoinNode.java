package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Join<T> {

  private ProcessNodeSet<T> aPredecessors;

  private ProcessNodeSet<T> aSuccessors;

  private int aMin;

  private int aMax;

  @Override
  public ProcessNodeSet<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = ProcessNodeSet.processNodeSet();
    }
    return aSuccessors;
  }

  @Override
  public ProcessNodeSet<T> getPredecessors() {
    return aPredecessors;
  }

  @Override
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    aPredecessors = ProcessNodeSet.processNodeSet(pPredecessors);
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    aSuccessors.add(pNode);
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void setMax(int pMax) {
    aMax = pMax;
  }

  @Override
  public int getMax() {
    return aMax;
  }

  @Override
  public void setMin(int pMin) {
    aMin = pMin;
  }

  @Override
  public int getMin() {
    return aMin;
  }

}
