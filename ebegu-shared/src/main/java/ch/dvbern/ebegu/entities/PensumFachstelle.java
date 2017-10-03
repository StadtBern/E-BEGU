package ch.dvbern.ebegu.entities;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import ch.dvbern.ebegu.util.EbeguUtil;
import org.hibernate.envers.Audited;

/**
 * Entity fuer PensumFachstelle.
 */
@Audited
@Entity
public class PensumFachstelle extends AbstractPensumEntity {

	private static final long serialVersionUID = -9132257320978374570L;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_pensum_fachstelle_fachstelle_id"))
	private Fachstelle fachstelle;

	public PensumFachstelle() {
	}

	public Fachstelle getFachstelle() {
		return fachstelle;
	}

	public void setFachstelle(Fachstelle fachstelle) {
		this.fachstelle = fachstelle;
	}

	public PensumFachstelle copyForMutation(PensumFachstelle mutation) {
		super.copyForMutation(mutation);
		copyForMutationOrErneuerung(mutation);
		return mutation;
	}

	public PensumFachstelle copyForErneuerung(PensumFachstelle mutation) {
		super.copyForErneuerung(mutation);
		copyForMutationOrErneuerung(mutation);
		return mutation;
	}

	private void copyForMutationOrErneuerung(PensumFachstelle mutation) {
		mutation.setFachstelle(this.getFachstelle());
	}

	@Override
	public boolean isSame(AbstractEntity other) {
		//noinspection ObjectEquality
		if (this == other) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (other == null || !getClass().equals(other.getClass())) {
			return false;
		}
		if (!super.isSame(other)) {
			return false;
		}
		if (!(other instanceof PensumFachstelle)) {
			return false;
		}
		final PensumFachstelle otherPensumFachstelle = (PensumFachstelle) other;
		return EbeguUtil.isSameObject(getFachstelle(), otherPensumFachstelle.getFachstelle());
	}
}
