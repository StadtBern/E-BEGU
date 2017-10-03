package ch.dvbern.ebegu.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import ch.dvbern.ebegu.enums.ApplicationPropertyKey;
import org.hibernate.envers.Audited;

import static ch.dvbern.ebegu.util.Constants.DB_DEFAULT_MAX_LENGTH;
import static ch.dvbern.ebegu.util.Constants.DB_TEXTAREA_LENGTH;

/**
 * Entitaet zum Speichern von diversen Applikationsproperties in der Datenbank.
 */
@Audited
@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames = "name", name = "UK_application_property_name")
)
public class ApplicationProperty extends AbstractEntity {

	private static final long serialVersionUID = -7687645920282879260L;
	@NotNull
	@Column(nullable = false, length = DB_DEFAULT_MAX_LENGTH)
	@Enumerated(EnumType.STRING)
	private ApplicationPropertyKey name;

	@Size(max = DB_TEXTAREA_LENGTH)
	@NotNull
	@Column(nullable = false, length = DB_TEXTAREA_LENGTH)
	private String value;


	public ApplicationProperty() {
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
		if (!(other instanceof ApplicationProperty)) {
			return false;
		}
		final ApplicationProperty otherApplicationProperty = (ApplicationProperty) other;
		return getName() == otherApplicationProperty.getName() &&
			Objects.equals(getValue(), otherApplicationProperty.getValue());
	}

	public ApplicationProperty(final ApplicationPropertyKey key, final String value) {
		this.name = key;
		this.value = value;
	}

	public ApplicationPropertyKey getName() {
		return name;
	}

	public void setName(final ApplicationPropertyKey name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

}
