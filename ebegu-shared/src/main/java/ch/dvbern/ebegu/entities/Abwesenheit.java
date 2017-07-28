package ch.dvbern.ebegu.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.Valid;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

/**
 * Entity fuer Abwesenheit.
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
@Audited
@Entity
public class Abwesenheit extends AbstractDateRangedEntity implements Comparable<Abwesenheit> {

	private static final long serialVersionUID = -6776981643150835840L;


	@Valid
	@OneToOne(optional = true, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "abwesenheitJA")
	private AbwesenheitContainer abwesenheitContainer;

	public Abwesenheit() {
	}

	public AbwesenheitContainer getAbwesenheitContainer() {
		return abwesenheitContainer;
	}

	public void setAbwesenheitContainer(AbwesenheitContainer abwesenheitContainer) {
		this.abwesenheitContainer = abwesenheitContainer;
	}

	@Override
	public int compareTo(Abwesenheit o) {
		CompareToBuilder builder = new CompareToBuilder();
		builder.append(this.getGueltigkeit(), o.getGueltigkeit());
		builder.append(this.getId(), o.getId());
		return builder.toComparison();
	}

	public Abwesenheit copyForMutation(Abwesenheit mutation) {
		return (Abwesenheit) super.copyForMutation(mutation);
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
		return super.isSame(other);
	}
}
