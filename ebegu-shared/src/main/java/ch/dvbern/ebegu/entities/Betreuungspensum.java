package ch.dvbern.ebegu.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import ch.dvbern.ebegu.types.DateRange;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

/**
 * Entity fuer Betreuungspensen.
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
@Audited
@Entity
public class Betreuungspensum extends AbstractPensumEntity implements Comparable<Betreuungspensum> {

	private static final long serialVersionUID = -9032857320571372370L;

	@NotNull
	@Column(nullable = false)
	private Boolean nichtEingetreten = false;

	public Betreuungspensum() {
	}

	public Betreuungspensum(BetreuungsmitteilungPensum betPensumMitteilung) {
		this.setGueltigkeit(new DateRange(betPensumMitteilung.getGueltigkeit()));
		this.setPensum(betPensumMitteilung.getPensum());
		this.setNichtEingetreten(false); //can not be set through BetreuungsmitteilungPensum
	}

	public Betreuungspensum(DateRange gueltigkeit) {
		this.setGueltigkeit(gueltigkeit);
	}

	public Boolean getNichtEingetreten() {
		return nichtEingetreten;
	}

	public void setNichtEingetreten(Boolean nichtEingetreten) {
		this.nichtEingetreten = nichtEingetreten;
	}

	@Override
	public int compareTo(Betreuungspensum o) {
		CompareToBuilder builder = new CompareToBuilder();
		builder.append(this.getGueltigkeit(), o.getGueltigkeit());
		builder.append(this.getId(), o.getId());
		return builder.toComparison();
	}

	public Betreuungspensum copyForMutation(Betreuungspensum mutation) {
		super.copyForMutation(mutation);
		mutation.setNichtEingetreten(this.getNichtEingetreten());
		return mutation;
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
		if (!(other instanceof Betreuungspensum)) {
			return false;
		}
		final Betreuungspensum otherBetreuungspensum = (Betreuungspensum) other;
		return Objects.equals(getNichtEingetreten(), otherBetreuungspensum.getNichtEingetreten());
	}
}
