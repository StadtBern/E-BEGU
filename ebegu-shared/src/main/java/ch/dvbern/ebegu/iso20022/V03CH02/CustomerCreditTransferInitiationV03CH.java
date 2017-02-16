
package ch.dvbern.ebegu.iso20022.V03CH02;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CustomerCreditTransferInitiationV03-CH complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CustomerCreditTransferInitiationV03-CH">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GrpHdr" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}GroupHeader32-CH"/>
 *         &lt;element name="PmtInf" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}PaymentInstructionInformation3-CH" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerCreditTransferInitiationV03-CH", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", propOrder = {
    "grpHdr",
    "pmtInf"
})
public class CustomerCreditTransferInitiationV03CH {

    @XmlElement(name = "GrpHdr", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", required = true)
    protected GroupHeader32CH grpHdr;
    @XmlElement(name = "PmtInf", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", required = true)
    protected List<PaymentInstructionInformation3CH> pmtInf;

    /**
     * Gets the value of the grpHdr property.
     * 
     * @return
     *     possible object is
     *     {@link GroupHeader32CH }
     *     
     */
    public GroupHeader32CH getGrpHdr() {
        return grpHdr;
    }

    /**
     * Sets the value of the grpHdr property.
     * 
     * @param value
     *     allowed object is
     *     {@link GroupHeader32CH }
     *     
     */
    public void setGrpHdr(GroupHeader32CH value) {
        this.grpHdr = value;
    }

    /**
     * Gets the value of the pmtInf property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pmtInf property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPmtInf().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PaymentInstructionInformation3CH }
     * 
     * 
     */
    public List<PaymentInstructionInformation3CH> getPmtInf() {
        if (pmtInf == null) {
            pmtInf = new ArrayList<PaymentInstructionInformation3CH>();
        }
        return this.pmtInf;
    }

}
