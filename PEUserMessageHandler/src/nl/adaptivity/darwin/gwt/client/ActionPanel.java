package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.base.client.CompletionListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;


public class ActionPanel {

  private static ActionPanelUiBinder groupUiBinder = GWT.create(ActionPanelUiBinder.class);

  @UiTemplate("ActionPanelGroup.ui.xml")
  interface ActionPanelUiBinder extends UiBinder<Element, ActionPanel> { /**/}

  DivElement aDivElement;

  public ActionPanel() {
    //    aDivElement = Document.get().createDivElement();
    //    setElement(aDivElement);
  }

  public static void load(final CompletionListener pCompletionListener) {
    new RequestBuilder(RequestBuilder.GET, "PEUserMessageHandler/actions");

    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
  }

}
