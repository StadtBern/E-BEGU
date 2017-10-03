package ch.dvbern.ebegu.entities;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import ch.dvbern.ebegu.enums.WizardStepName;
import ch.dvbern.ebegu.enums.WizardStepStatus;
import ch.dvbern.ebegu.util.Constants;
import org.hibernate.envers.Audited;

/**
 * Diese Entitaet speichert den Status von jedem Schritt im Wizzard (left menu). Ausserdem eine Bemerkung fuer jeden
 * Schritt kann auch gespeichert werden.
 */
@Entity
@Audited
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames = { "wizardStepName", "gesuch_id" }, name = "UK_wizardstep_gesuch_stepname")
)
public class WizardStep extends AbstractEntity {

	private static final long serialVersionUID = -9032284720578372570L;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "FK_wizardstep_gesuch_id"), nullable = false)
	private Gesuch gesuch;

	@NotNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private WizardStepName wizardStepName;

	@NotNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private WizardStepStatus wizardStepStatus = WizardStepStatus.UNBESUCHT;

	@Size(max = Constants.DB_TEXTAREA_LENGTH)
	@Nullable
	@Column(nullable = true, length = Constants.DB_TEXTAREA_LENGTH)
	private String bemerkungen;

	@NotNull
	@Column(nullable = false)
	private Boolean verfuegbar = false;

	public Gesuch getGesuch() {
		return gesuch;
	}

	public void setGesuch(Gesuch gesuch) {
		this.gesuch = gesuch;
	}

	public WizardStepName getWizardStepName() {
		return wizardStepName;
	}

	public void setWizardStepName(WizardStepName stepName) {
		this.wizardStepName = stepName;
	}

	public WizardStepStatus getWizardStepStatus() {
		return wizardStepStatus;
	}

	public void setWizardStepStatus(WizardStepStatus stepStatus) {
		this.wizardStepStatus = stepStatus;
	}

	@Nullable
	public String getBemerkungen() {
		return bemerkungen;
	}

	public void setBemerkungen(@Nullable String bemerkungen) {
		this.bemerkungen = bemerkungen;
	}

	public Boolean getVerfuegbar() {
		return verfuegbar;
	}

	public void setVerfuegbar(Boolean verfuegbar) {
		this.verfuegbar = verfuegbar;
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
		if (!(other instanceof WizardStep)) {
			return false;
		}
		final WizardStep otherWizardStep = (WizardStep) other;
		return Objects.equals(getWizardStepName(), otherWizardStep.getWizardStepName()) &&
			Objects.equals(getWizardStepStatus(), otherWizardStep.getWizardStepStatus()) &&
			Objects.equals(getBemerkungen(), otherWizardStep.getBemerkungen()) &&
			Objects.equals(getVerfuegbar(), otherWizardStep.getVerfuegbar());
	}
}
