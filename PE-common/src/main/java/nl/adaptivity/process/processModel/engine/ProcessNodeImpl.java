package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.IdFactory;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import java.sql.SQLException;
import java.util.Collection;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ JoinImpl.class, SplitImpl.class, JoinSplitImpl.class, ActivityImpl.class, EndNodeImpl.class, StartNodeImpl.class })
public abstract class ProcessNodeImpl extends ProcessNodeBase<ProcessNodeImpl>  {

//  private Collection<? extends IXmlImportType> mImports;
//
//  private Collection<? extends IXmlExportType> mExports;

  protected ProcessNodeImpl(@Nullable final ProcessModelBase<ProcessNodeImpl> ownerModel) {
    super(ownerModel);
    if (ownerModel!=null) {
      mOwnerModel.addNode(this);
    }
  }


  public ProcessNodeImpl(final ProcessModelBase<ProcessNodeImpl> ownerModel, @NotNull final Collection<? extends Identifiable> predecessors) {
    this(ownerModel);
    if ((predecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((predecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(predecessors);
  }

  @Override
  public String getId() {
    String id = super.getId();
    if (id == null) {
      id = IdFactory.create();
      setId(id);
    }
    return id;
  }

  /**
   * Should this node be able to be provided?
   *
   *
   * @param transaction
   * @param instance The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  public abstract boolean condition(final Transaction transaction, IProcessNodeInstance<?> instance);

  /**
   * Take action to make task available
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction transaction, IMessageService<T, U> messageService, U instance) throws SQLException;

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> messageService, U instance);

  public abstract <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> messageService, U instance);

  @NotNull
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(getClass().getSimpleName()).append(" (").append(getId());
    if ((this.getPredecessors() == null) || (getMaxPredecessorCount()==0)) {
      result.append(')');
    } else {
      final int predCount = this.getPredecessors().size();
      if (predCount != 1) {
        result.append(", pred={");
        for (final Identifiable pred : getPredecessors()) {
          result.append(pred.getId()).append(", ");
        }
        if (result.charAt(result.length() - 2) == ',') {
          result.setLength(result.length() - 2);
        }
        result.append("})");
      } else {
        result.append(", pred=").append(getPredecessors().iterator().next().getId());
        result.append(')');
      }
    }
    return result.toString();
  }


}
