package ch.dvbern.ebegu.rules;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import ch.dvbern.ebegu.enums.MsgKey;
import ch.dvbern.ebegu.types.DateRange;

import javax.annotation.Nonnull;

/**
 * Regel für Wohnsitz in Bern (Zuzug und Wegzug):
 * - Durch Adresse definiert
 * - Anspruch vom ersten Tag des Zuzugs
 * - Anspruch bis 2 Monate nach Wegzug, auf Ende Monat
 * Verweis 16.8 Der zivilrechtliche Wohnsitz
 */
public class WohnsitzCalcRule extends AbstractCalcRule {


	public WohnsitzCalcRule(@Nonnull DateRange validityPeriod) {
		super(RuleKey.WOHNSITZ, RuleType.REDUKTIONSREGEL, validityPeriod);
	}

	@SuppressWarnings("PMD.CollapsibleIfStatements")
	@Override
	protected void executeRule(@Nonnull Betreuung betreuung, @Nonnull VerfuegungZeitabschnitt verfuegungZeitabschnitt) {
		if (betreuung.getBetreuungsangebotTyp().isJugendamt()) {
			if (singleGesuchstellerIsNotBern(betreuung, verfuegungZeitabschnitt)
				|| coupleAndBothNotBern(betreuung, verfuegungZeitabschnitt)) {
				verfuegungZeitabschnitt.setAnspruchberechtigtesPensum(0);
				verfuegungZeitabschnitt.addBemerkung(RuleKey.WOHNSITZ, MsgKey.WOHNSITZ_MSG);
			}

		}
	}

	private boolean coupleAndBothNotBern(Betreuung betreuung, VerfuegungZeitabschnitt verfuegungZeitabschnitt) {
		return betreuung.extractGesuch().getGesuchsteller2() != null
			&& verfuegungZeitabschnitt.isWohnsitzNichtInGemeindeGS1()
			&& verfuegungZeitabschnitt.isWohnsitzNichtInGemeindeGS2();

	}

	private boolean singleGesuchstellerIsNotBern(@Nonnull Betreuung betreuung, @Nonnull VerfuegungZeitabschnitt verfuegungZeitabschnitt) {
		return betreuung.extractGesuch().getGesuchsteller2() == null && verfuegungZeitabschnitt.isWohnsitzNichtInGemeindeGS1();
	}
}
