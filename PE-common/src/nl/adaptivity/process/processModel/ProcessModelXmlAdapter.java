package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel> {

  @Override
  public XmlProcessModel marshal(ProcessModel pProcessModel) throws Exception {
    return new XmlProcessModel(pProcessModel);
  }

  @Override
  public ProcessModel unmarshal(XmlProcessModel pModel) throws Exception {
    return new ProcessModel(pModel);
  }

}
