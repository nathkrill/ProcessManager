package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.HandleMap.Handle;

import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;


public interface IProcessModelRef<T extends ProcessNode<T>>  extends Handle<ProcessModel<T>>{

  public abstract String getName();

}