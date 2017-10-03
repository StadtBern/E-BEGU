package ch.dvbern.ebegu.entities;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import ch.dvbern.ebegu.validators.CheckBetreuungsmitteilung;
import ch.dvbern.ebegu.validators.CheckBetreuungsmitteilungDatesOverlapping;
import org.hibernate.envers.Audited;

/**
 * Entitaet zum Speichern von Betreuungsmitteilung in der Datenbank.
 */
@CheckBetreuungsmitteilung
@CheckBetreuungsmitteilungDatesOverlapping
@Audited
@Entity
public class Betreuungsmitteilung extends Mitteilung {

	private static final long serialVersionUID = 489324250868016126L;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "betreuungsmitteilung")
	private Set<BetreuungsmitteilungPensum> betreuungspensen = new TreeSet<>();

	private boolean applied;

	public Set<BetreuungsmitteilungPensum> getBetreuungspensen() {
		return betreuungspensen;
	}

	public void setBetreuungspensen(Set<BetreuungsmitteilungPensum> betreuungspensen) {
		this.betreuungspensen = betreuungspensen;
	}

	public boolean isApplied() {
		return applied;
	}

	public void setApplied(boolean applied) {
		this.applied = applied;
	}

	@Override
	public boolean isSame(AbstractEntity other) {
		//noinspection ObjectEquality
		if (this == other) {
			return true;
		}
		if (other == null || !getClass().equals(other.getClass())) {
			return false;
		}
		if (!super.isSame(other)) {
			return false;
		}
		if (!(other instanceof Betreuungsmitteilung)) {
			return false;
		}
		final Betreuungsmitteilung otherBetreuungsmitteilung = (Betreuungsmitteilung) other;
		return isApplied() == otherBetreuungsmitteilung.isApplied();
	}

}
