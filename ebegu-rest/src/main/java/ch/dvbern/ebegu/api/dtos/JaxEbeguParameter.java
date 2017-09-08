package ch.dvbern.ebegu.api.dtos;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.dvbern.ebegu.enums.EbeguParameterKey;

/**
 * DTO fuer zeitabhängige E-BEGU-Parameter
 */
@XmlRootElement(name = "ebeguparameter")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxEbeguParameter extends JaxAbstractDateRangedDTO {

	private static final long serialVersionUID = 2539868697910194410L;

	/**
	 * value of the parameter
	 */
	@NotNull
	private String value = null;

	@NotNull
	private EbeguParameterKey name = null;

	private boolean proGesuchsperiode;


	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public EbeguParameterKey getName() {
		return name;
	}

	public void setName(EbeguParameterKey name) {
		this.name = name;
	}

	public boolean isProGesuchsperiode() {
		return proGesuchsperiode;
	}

	public void setProGesuchsperiode(boolean proGesuchsperiode) {
		this.proGesuchsperiode = proGesuchsperiode;
	}
}
