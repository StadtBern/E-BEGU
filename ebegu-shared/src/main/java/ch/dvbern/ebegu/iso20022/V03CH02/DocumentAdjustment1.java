
package ch.dvbern.ebegu.iso20022.V03CH02;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DocumentAdjustment1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DocumentAdjustment1">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Amt" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}ActiveOrHistoricCurrencyAndAmount"/>
 *         &lt;element name="CdtDbtInd" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}CreditDebitCode" minOccurs="0"/>
 *         &lt;element name="Rsn" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}Max4Text" minOccurs="0"/>
 *         &lt;element name="AddtlInf" type="{http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd}Max140Text" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DocumentAdjustment1", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", propOrder = {
    "amt",
    "cdtDbtInd",
    "rsn",
    "addtlInf"
})
public class DocumentAdjustment1 {

    @XmlElement(name = "Amt", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd", required = true)
    protected ActiveOrHistoricCurrencyAndAmount amt;
    @XmlElement(name = "CdtDbtInd", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd")
    @XmlSchemaType(name = "string")
    protected CreditDebitCode cdtDbtInd;
    @XmlElement(name = "Rsn", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd")
    protected String rsn;
    @XmlElement(name = "AddtlInf", namespace = "http://www.six-interbank-clearing.com/de/pain.001.001.03.ch.02.xsd")
    protected String addtlInf;

    /**
     * Gets the value of the amt property.
     * 
     * @return
     *     possible object is
     *     {@link ActiveOrHistoricCurrencyAndAmount }
     *     
     */
    public ActiveOrHistoricCurrencyAndAmount getAmt() {
        return amt;
    }

    /**
     * Sets the value of the amt property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActiveOrHistoricCurrencyAndAmount }
     *     
     */
    public void setAmt(ActiveOrHistoricCurrencyAndAmount value) {
        this.amt = value;
    }

    /**
     * Gets the value of the cdtDbtInd property.
     * 
     * @return
     *     possible object is
     *     {@link CreditDebitCode }
     *     
     */
    public CreditDebitCode getCdtDbtInd() {
        return cdtDbtInd;
    }

    /**
     * Sets the value of the cdtDbtInd property.
     * 
     * @param value
     *     allowed object is
     *     {@link CreditDebitCode }
     *     
     */
    public void setCdtDbtInd(CreditDebitCode value) {
        this.cdtDbtInd = value;
    }

    /**
     * Gets the value of the rsn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRsn() {
        return rsn;
    }

    /**
     * Sets the value of the rsn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRsn(String value) {
        this.rsn = value;
    }

    /**
     * Gets the value of the addtlInf property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddtlInf() {
        return addtlInf;
    }

    /**
     * Sets the value of the addtlInf property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddtlInf(String value) {
        this.addtlInf = value;
    }

}
