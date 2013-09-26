package nl.adaptivity.process.processModel;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public interface ProcessModel<T extends ProcessNode<T>> {

  /**
   * Get the amount of end nodes in the model
   *
   * @return The amount of end nodes.
   */
  public int getEndNodeCount();

  /**
   * Get a reference node for this model.
   *
   * @return A reference node.
   */
  public IProcessModelRef<T> getRef();

  /**
   * Get the process node with the given id.
   * @param pNodeId The node id to look up.
   * @return The process node with the id.
   */
  public T getNode(String pNodeId);

  public Collection<? extends T> getModelNodes();

  public String getName();

  public Principal getOwner();

  public Set<String> getRoles();

  public Collection<? extends StartNode<T>> getStartNodes();

}