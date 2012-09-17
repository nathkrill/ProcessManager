package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


@XmlRootElement(name=EndNode.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNode extends ProcessNode{

  private List<XmlExportType> aExports;

  public EndNode(ProcessNode pPrevious) {
    super(pPrevious);
  }

  public EndNode() {
  }

  private static final long serialVersionUID = 220908810658246960L;
  public static final String ELEMENTNAME = "end";

  @Override
  public boolean condition(Task<?> pInstance) {
    return true;
  }

  @XmlAttribute(name="predecessor", required=true)
  @XmlIDREF
  public ProcessNode getPredecessor() {
    Collection<ProcessNode> ps = getPredecessors();
    if (ps==null || ps.size()!=1) {
      return null;
    }
    return ps.iterator().next();
  }

  public void setPredecessor(ProcessNode predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  @XmlElement(name="export")
  public List<XmlExportType> getExports() {
    if (aExports == null) {
        aExports = new ArrayList<XmlExportType>();
    }
    return aExports;
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    return new ArrayList<ProcessNode>(0);
  }

  @Override
  public <T, U extends Task<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance) {
    return true;
  }

  @Override
  public <T, U extends Task<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance) {
    return true;
  }

  @Override
    public <T, U extends Task<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance) {
  //    pProcessInstance.finish();
      return true;
    }

}
