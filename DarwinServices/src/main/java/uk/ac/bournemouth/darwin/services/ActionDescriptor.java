package uk.ac.bournemouth.darwin.services;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.JaxbUriAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.net.URI;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = ActionDescriptor.ELEMENTNAME, namespace = Constants.USER_MESSAGE_HANDLER_NS)
public class ActionDescriptor {

  static final String ELEMENTNAME = "action";

  private String mTitle;

  private String mDescription;

  private URI mIcon;

  private URI mLocation;

  @XmlAttribute(name = "title")
  public String getTitle() {
    return mTitle;
  }

  public void setTitle(final String title) {
    mTitle = title;
  }

  @XmlValue
  public String getDescription() {
    return mDescription;
  }

  public void setDescription(final String description) {
    mDescription = description;
  }

  @XmlAttribute(name = "icon")
  @XmlJavaTypeAdapter(type = URI.class, value = JaxbUriAdapter.class)
  public URI getIcon() {
    return mIcon;
  }


  public void setIcon(final URI icon) {
    mIcon = icon;
  }


  @XmlAttribute(name = "href")
  @XmlJavaTypeAdapter(type = URI.class, value = JaxbUriAdapter.class)
  public URI getLocation() {
    return mLocation;
  }


  public void setLocation(final URI location) {
    mLocation = location;
  }


}