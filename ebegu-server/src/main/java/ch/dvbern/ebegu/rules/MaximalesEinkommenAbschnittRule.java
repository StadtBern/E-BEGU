package ch.dvbern.ebegu.rules;

import ch.dvbern.ebegu.dto.FinanzielleSituationResultateDTO;
import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import ch.dvbern.ebegu.types.DateRange;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Setzt fuer die Zeitabschnitte das Massgebende Einkommen. Sollte der Maximalwert uebschritte werden so wird das Pensum auf 0 gesetzt
 * ACHTUNG: Diese Regel gilt nicht fuer Kita und Tageseltern Kleinkinder.  Bei Tageseltern Schulkinder und Tagesstaetten
 * gibt es keine Reduktion des Anspruchs.
 * Regel 16.7 Maximales Einkommen
 */
public class MaximalesEinkommenAbschnittRule extends AbstractAbschnittRule {




	public MaximalesEinkommenAbschnittRule(DateRange validityPeriod) {
		super(RuleKey.MAXIMALES_EINKOMMEN, RuleType.GRUNDREGEL_DATA, validityPeriod);
	}

	@Nonnull
	@Override
	protected List<VerfuegungZeitabschnitt> createVerfuegungsZeitabschnitte(@Nonnull Betreuung betreuung, @Nonnull List<VerfuegungZeitabschnitt> zeitabschnitte, @Nonnull FinanzielleSituationResultateDTO finSitResultatDTO) {
		// TODO Einkommensverschlechterung(en) berücksichtigen mit deren Stichdatum (immer 1. des Monats)
		// TODO Gehen wir hier davon aus, dass die "EinkommensverschlechterungsRegel" schon die Schnitze für anderes Einkommen gemacht hat?
		List<VerfuegungZeitabschnitt> einkommensAbschnitte = new ArrayList<>();
		VerfuegungZeitabschnitt finanzielleSituationAbschnitt = new VerfuegungZeitabschnitt(betreuung.extractGesuchsperiode().getGueltigkeit());
		finanzielleSituationAbschnitt.setMassgebendesEinkommen(readMassgebendesEinkommen(finSitResultatDTO));
		einkommensAbschnitte.add(finanzielleSituationAbschnitt);
		return einkommensAbschnitte;
	}

	/**
	 * Beim auslesen des Massgebenden Einkommens ist die FinanzielleSituationResultatDTO bzw die
	 * Einkommensverschlechterung relevant. Das heisst je nach Datum ist das massgebende Einkommen anders
	 */
	private BigDecimal readMassgebendesEinkommen(FinanzielleSituationResultateDTO finSitResultatDTO) {
		return finSitResultatDTO.getMassgebendesEinkommen();
	}
}