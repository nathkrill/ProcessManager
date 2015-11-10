package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class EditableProcessModel {

  private final ProcessModel aProcessModel;

  List<EditableProcessNode> aNodes;

  public EditableProcessModel(final ProcessModel processModel) {
    aProcessModel = processModel;
  }

  public Collection<EditableProcessNode> getNodes() {
    if (aNodes == null) {
      final List<ProcessNode> nodes = aProcessModel.getNodes();
      aNodes = new ArrayList<EditableProcessNode>(nodes.size());
      for (final ProcessNode node : nodes) {
        aNodes.add(EditableProcessNode.create(node));
      }
    }
    return aNodes;
  }

  public void layout() {
    aProcessModel.layout();
    // TODO layout nodes
  }

}
