package ch.dvbern.ebegu.rules;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;

/**
 * Interface für alle Berechnungs-Regeln in Ki-Tax.
 */
public interface Rule {

	/**
	 * @return Datum von dem an die Regel gilt
	 */
	@Nonnull
	LocalDate validFrom();

	/**
	 * @return Datum bis zu dem die Regel gilt
	 */
	@Nonnull
	LocalDate validTo();

	/**
	 * @param stichtag
	 * @return true wenn die Regel am Strichtag gueltig sit
	 */
	boolean isValid(@Nonnull LocalDate stichtag);

	/**
	 *
	 * @return den {@link RuleType} Enumwert dieser Regel
	 */
	@Nonnull
	RuleType getRuleType();

	/**
	 *
	 * @return einzigartiger Key fuer diese Regel
	 */
	@Nonnull
	RuleKey getRuleKey();

	/**
	 * Diese Methode fuehrt die eigentliche Berechnung durch die von der Regel abgebildet wird
	 * @param betreuung Die Betreuung fuer die Berechnet wird
	 * @param zeitabschnitte Die Zeitabschnitte die bereits ermittelt wurden
	 * @return gemergete Liste von bestehenden und neu berechneten Zeitabschnitten
	 */
	@Nonnull
	List<VerfuegungZeitabschnitt> calculate(Betreuung betreuung, @Nonnull List<VerfuegungZeitabschnitt> zeitabschnitte);

	/**
	 * Gibt zurueck, ob die Regel fuer die Berechnung der Familiensituation (Fam-Groesse, Einkommen, Abzug fuer Fam-Groesse)
	 * relevant ist
     */
	boolean isRelevantForFamiliensituation();
}
