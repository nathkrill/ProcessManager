//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.15 at 03:25:47 PM BST 
//


package nl.adaptivity.process.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.*;

import nl.adaptivity.process.exec.Task.TaskState;


/**
 * <p>Java class for ActivityResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivityResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="taskState">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Available"/>
 *             &lt;enumeration value="Taken"/>
 *             &lt;enumeration value="Started"/>
 *             &lt;enumeration value="Complete"/>
 *             &lt;enumeration value="Failed"/>
 *             &lt;enumeration value="Cancelled"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActivityResponseType")
@XmlRootElement(name=ActivityResponse.ELEMENTNAME, namespace = "http://adaptivity.nl/ProcessEngine/")
public class ActivityResponse {

    public static final String ELEMENTNAME = "ActivityResponse";
    public static final String TASKSTATEATTRNAME = "taskState";
    @XmlAnyElement(lax = false)
    protected List<Object> body;
    
    @XmlTransient
    private TaskState aTaskState;

    // Default constructor for jaxb use
    protected ActivityResponse() {}
    
    public ActivityResponse(TaskState pTaskState, Object... pResult) {
      aTaskState = pTaskState;
      getBody().addAll(Arrays.asList(pResult));
    }

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getBody() {
        if (body == null) {
            body = new ArrayList<Object>();
        }
        return this.body;
    }

    /**
     * Gets the value of the taskState property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @XmlAttribute(name = TASKSTATEATTRNAME)
    public String getTaskState() {
        return aTaskState.name();
    }

    /**
     * Sets the value of the taskState property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaskState(String value) {
        aTaskState = TaskState.valueOf(value);
    }

}
