
package ch.dvbern.ebegu.iso20022.V03CH02;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CashAccount16-CH_Id complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CashAccount16-CH_Id">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Id" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}AccountIdentification4Choice-CH"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CashAccount16-CH_Id", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", propOrder = {
    "id"
})
public class CashAccount16CHId {

    @XmlElement(name = "Id", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", required = true)
    protected AccountIdentification4ChoiceCH id;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link AccountIdentification4ChoiceCH }
     *     
     */
    public AccountIdentification4ChoiceCH getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccountIdentification4ChoiceCH }
     *     
     */
    public void setId(AccountIdentification4ChoiceCH value) {
        this.id = value;
    }

}
