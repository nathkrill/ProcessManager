package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;


@XmlRootElement(name = Join.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class Join extends ProcessNodeImpl {

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTNAME = "join";

  private int aMin;

  private int aMax;

  private ArrayList<Object> aPred;

  public Join(final Collection<ProcessNodeImpl> pNodes, final int pMin, final int pMax) {
    super(pNodes);
    setMin(pMin);
    setMax(Math.min(pNodes.size(), pMax));
    if ((getMin() < 1) || (getMin() > pNodes.size()) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public Join() {}

  public static Join andJoin(final ProcessNodeImpl... pNodes) {
    return new Join(Arrays.asList(pNodes), pNodes.length, pNodes.length);
  }

  public void setMax(final int max) {
    aMax = max;
  }

  @XmlAttribute(required = true)
  public int getMax() {
    return aMax;
  }

  public void setMin(final int min) {
    aMin = min;
  }

  @XmlAttribute(required = true)
  public int getMin() {
    return aMin;
  }

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  @Override
  public void skip() {
    //    JoinInstance j = pProcessInstance.getJoinInstance(this, pPredecessor);
    //    if (j.isFinished()) {
    //      return;
    //    }
    //    j.incSkipped();
    //    throw new UnsupportedOperationException("Not yet correct");
  }

  @XmlElement(name = "predecessor")
  @XmlIDREF
  public Collection<Object> getPred() {
    if (aPred == null) {
      aPred = new ArrayList<>();
    }
    return aPred;
  }

  @XmlElement(name = "predecessor")
  @XmlIDREF
  @Override
  public Collection<ProcessNodeImpl> getPredecessors() {
    if (aPred != null) {
      final Collection<ProcessNodeImpl> pred = super.getPredecessors();
      for (final Object o : aPred) {
        if (o instanceof ProcessNode) {
          pred.add((ProcessNodeImpl) o);
        }
      }
      aPred = null;
      return pred;
    }
    return super.getPredecessors();
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

}
