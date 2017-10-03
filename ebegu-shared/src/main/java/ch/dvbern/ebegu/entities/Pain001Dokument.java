package ch.dvbern.ebegu.entities;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.Audited;

/**
 * Entitaet zum Speichern von GeneratedDokument in der Datenbank.
 */
@Audited
@Entity
@EntityListeners({ WriteProtectedDokumentListener.class })
public class Pain001Dokument extends WriteProtectedDokument {

	private static final long serialVersionUID = -3981085201151840861L;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_pain001dokument_zahlungsauftrag_id"), nullable = false)
	private Zahlungsauftrag zahlungsauftrag;

	public Pain001Dokument() {
	}

	public Zahlungsauftrag getZahlungsauftrag() {
		return zahlungsauftrag;
	}

	public void setZahlungsauftrag(Zahlungsauftrag zahlungsauftrag) {
		this.zahlungsauftrag = zahlungsauftrag;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.appendSuper(super.toString())
			.append("zahlungsauftrag", zahlungsauftrag)
			.toString();
	}
}
