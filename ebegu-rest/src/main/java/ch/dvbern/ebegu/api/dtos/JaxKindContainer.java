package ch.dvbern.ebegu.api.dtos;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO fuer Kind Container
 */
@XmlRootElement(name = "kind")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxKindContainer extends JaxAbstractDTO {

	private static final long serialVersionUID = -8912537186244981782L;

	@Valid
	private JaxKind kindGS;

	@Valid
	private JaxKind kindJA;

	@NotNull
	private Set<JaxBetreuung> betreuungen = new LinkedHashSet<>();

	@Min(1)
	private Integer kindNummer = 1;

	@Min(1)
	private Integer nextNumberBetreuung = 1;

	@Nullable
	private Boolean kindMutiert;


	public JaxKind getKindGS() {
		return kindGS;
	}

	public void setKindGS(JaxKind kindGS) {
		this.kindGS = kindGS;
	}

	public JaxKind getKindJA() {
		return kindJA;
	}

	public void setKindJA(JaxKind kindJA) {
		this.kindJA = kindJA;
	}

	public Set<JaxBetreuung> getBetreuungen() {
		return betreuungen;
	}

	public void setBetreuungen(Set<JaxBetreuung> betreuungen) {
		this.betreuungen = betreuungen;
	}

	public Integer getKindNummer() {
		return kindNummer;
	}

	public void setKindNummer(Integer kindNummer) {
		this.kindNummer = kindNummer;
	}

	public Integer getNextNumberBetreuung() {
		return nextNumberBetreuung;
	}

	public void setNextNumberBetreuung(Integer nextNumberBetreuung) {
		this.nextNumberBetreuung = nextNumberBetreuung;
	}

	public Boolean isKindMutiert() {
		return kindMutiert;
	}

	public void setKindMutiert(Boolean kindMutiert) {
		this.kindMutiert = kindMutiert;
	}
}
