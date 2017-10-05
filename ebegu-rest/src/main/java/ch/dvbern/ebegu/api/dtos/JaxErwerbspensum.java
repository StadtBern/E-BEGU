package ch.dvbern.ebegu.api.dtos;

import ch.dvbern.ebegu.enums.Taetigkeit;
import ch.dvbern.ebegu.enums.Zuschlagsgrund;

import javax.annotation.Nullable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * DTO fuer Erwerbspensum
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxErwerbspensum extends JaxAbstractPensumDTO {

	private static final long serialVersionUID = -2495737706808699744L;

	@NotNull
	@Enumerated(EnumType.STRING)
	private Taetigkeit taetigkeit;

	@NotNull
	private boolean zuschlagZuErwerbspensum;

	@Enumerated(EnumType.STRING)
	private Zuschlagsgrund zuschlagsgrund;

	@Min(0)
	private Integer zuschlagsprozent;

	@Nullable
	private String bezeichnung;

	public Taetigkeit getTaetigkeit() {
		return taetigkeit;
	}

	public void setTaetigkeit(Taetigkeit taetigkeit) {
		this.taetigkeit = taetigkeit;
	}

	public boolean getZuschlagZuErwerbspensum() {
		return zuschlagZuErwerbspensum;
	}

	public void setZuschlagZuErwerbspensum(boolean zuschlagZuErwerbspensum) {
		this.zuschlagZuErwerbspensum = zuschlagZuErwerbspensum;
	}

	public Zuschlagsgrund getZuschlagsgrund() {
		return zuschlagsgrund;
	}

	public void setZuschlagsgrund(Zuschlagsgrund zuschlagsgrund) {
		this.zuschlagsgrund = zuschlagsgrund;
	}

	public Integer getZuschlagsprozent() {
		return zuschlagsprozent;
	}

	public void setZuschlagsprozent(Integer zuschlagsprozent) {
		this.zuschlagsprozent = zuschlagsprozent;
	}

	@Nullable
	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(@Nullable String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}
}
