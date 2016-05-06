package ch.dvbern.ebegu.entities;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

/**
 * Container-Entity für die Kinder: Diese muss für jeden Benutzertyp (GS, JA, SV) einzeln geführt werden,
 * damit die Veränderungen / Korrekturen angezeigt werden können.
 */
@Audited
@Entity
public class KindContainer extends AbstractEntity {

	private static final long serialVersionUID = -6784985260190035840L;

	@NotNull
	@ManyToOne(optional = false)
	private Gesuch gesuch;

	@OneToOne (optional = true, cascade = CascadeType.ALL, orphanRemoval = true)
	private Kind kindGS;

	@OneToOne (optional = true, cascade = CascadeType.ALL, orphanRemoval = true)
	private Kind kindJA;


	public Gesuch getGesuch() {
		return gesuch;
	}

	public void setGesuch(Gesuch gesuch) {
		this.gesuch = gesuch;
//		if (gesuch != null && (gesuch.getKindContainers() == null || !gesuch.getKindContainers().contains(this))) {
//			gesuch.addKindContainer(this);
//		}
	}

	public Kind getKindGS() {
		return kindGS;
	}

	public void setKindGS(Kind kindGS) {
		this.kindGS = kindGS;
	}

	public Kind getKindJA() {
		return kindJA;
	}

	public void setKindJA(Kind kindJA) {
		this.kindJA = kindJA;
	}

}
