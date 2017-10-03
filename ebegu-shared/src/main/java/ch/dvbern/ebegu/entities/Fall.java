package ch.dvbern.ebegu.entities;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import ch.dvbern.ebegu.dto.suchfilter.lucene.EBEGUGermanAnalyzer;
import ch.dvbern.ebegu.dto.suchfilter.lucene.Searchable;
import ch.dvbern.ebegu.util.Constants;
import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.builtin.LongBridge;

/**
 * Entitaet zum Speichern von Fall in der Datenbank.
 */
@Audited
@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(columnNames = "fallNummer", name = "UK_fall_nummer"),
		@UniqueConstraint(columnNames = "besitzer_id", name = "UK_fall_besitzer")
	},
	indexes = {
		@Index(name = "IX_fall_fall_nummer", columnList = "fallNummer"),
		@Index(name = "IX_fall_besitzer", columnList = "besitzer_id"),
		@Index(name = "IX_fall_verantwortlicher", columnList = "verantwortlicher_id"),
		@Index(name = "IX_fall_mandant", columnList = "mandant_id")
	}
)
@Indexed
@Analyzer(impl = EBEGUGermanAnalyzer.class)
public class Fall extends AbstractEntity implements HasMandant, Searchable {

	private static final long serialVersionUID = -9154456879261811678L;

	@NotNull
	@Column(nullable = false)
	@Min(1)
	@Field(bridge = @FieldBridge(impl = LongBridge.class))
	private long fallNummer = 1;

	@Nullable
	@ManyToOne(optional = true)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_fall_verantwortlicher_id"))
	private Benutzer verantwortlicher = null; // Mitarbeiter des JA

	@Nullable
	@ManyToOne(optional = true)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_fall_besitzer_id"))
	@IndexedEmbedded
	private Benutzer besitzer = null; // Erfassender (im IAM eingeloggter) Gesuchsteller

	/**
	 * nextNumberKind ist die Nummer, die das naechste Kind bekommen wird. Aus diesem Grund ist es by default 1
	 * Dieses Feld darf nicht mit der Anzahl der Kinder verwechselt werden, da sie sehr unterschiedlich sein koennen falls mehrere Kinder geloescht wurden
	 */
	@NotNull
	@Min(1)
	@Column(nullable = false)
	private Integer nextNumberKind = 1;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_fall_mandant_id"))
	private Mandant mandant;

	public long getFallNummer() {
		return fallNummer;
	}

	public void setFallNummer(long fallNummer) {
		this.fallNummer = fallNummer;
	}

	@Nullable
	public Benutzer getVerantwortlicher() {
		return verantwortlicher;
	}

	public void setVerantwortlicher(@Nullable Benutzer verantwortlicher) {
		this.verantwortlicher = verantwortlicher;
	}

	@Nullable
	public Benutzer getBesitzer() {
		return besitzer;
	}

	public void setBesitzer(@Nullable Benutzer besitzer) {
		this.besitzer = besitzer;
	}

	public Integer getNextNumberKind() {
		return nextNumberKind;
	}

	public void setNextNumberKind(Integer nextNumberKind) {
		this.nextNumberKind = nextNumberKind;
	}

	@Override
	public Mandant getMandant() {
		return mandant;
	}

	public void setMandant(Mandant mandant) {
		this.mandant = mandant;
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
		if (!(other instanceof Fall)) {
			return false;
		}
		final Fall otherFall = (Fall) other;
		return Objects.equals(getFallNummer(), otherFall.getFallNummer());
	}

	@Transient
	public String getPaddedFallnummer() {
		return StringUtils.leftPad(String.valueOf(this.getFallNummer()), Constants.FALLNUMMER_LENGTH, '0');
	}

	@Nonnull
	@Override
	public String getSearchResultId() {
		return getId();
	}

	@Nonnull
	@Override
	public String getSearchResultSummary() {
		return getPaddedFallnummer();
	}

	@Nullable
	@Override
	public String getSearchResultAdditionalInformation() {
		return toString();
	}

	@Nullable
	@Override
	public String getOwningGesuchId() {
		//haben wir hier nicht da der Fall nicht zu einem Gesuch gehoert
		return null;
	}

	@Override
	public String getOwningFallId() {
		return getId();
	}
}
