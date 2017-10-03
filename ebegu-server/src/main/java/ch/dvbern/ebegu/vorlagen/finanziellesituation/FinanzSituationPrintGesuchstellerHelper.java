package ch.dvbern.ebegu.vorlagen.finanziellesituation;
/*
* Copyright (c) 2016 DV Bern AG, Switzerland
*
* Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
* geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
* insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
* elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
* Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
*
* Ersteller: zeab am: 23.08.2016
*/

import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.GesuchstellerContainer;
import org.apache.commons.lang3.Validate;

public final class FinanzSituationPrintGesuchstellerHelper {

	/**
	 * Konstruktor
	 */
	private FinanzSituationPrintGesuchstellerHelper() {
		// NOP
	}

	/**
	 * Erstellt das FinanzSituationGesuchsteller fuer Gesuchsteller 1
	 *
	 * @return FinanzSituationGesuchsteller
	 */
	public static FinanzSituationPrintGesuchsteller getFinanzSituationGesuchsteller1(Gesuch gesuch) {

		GesuchstellerContainer gesuchsteller1 = gesuch.getGesuchsteller1();
		Validate.notNull(gesuchsteller1);
		Validate.notNull(gesuchsteller1.getFinanzielleSituationContainer());
		FinanzSituationPrintGesuchsteller finanzSituationPrintGesuchsteller = new FinanzSituationPrintGesuchsteller(gesuchsteller1.getFinanzielleSituationContainer().getFinanzielleSituationJA(), //
			gesuchsteller1.getEinkommensverschlechterungContainer() != null ? gesuchsteller1.getEinkommensverschlechterungContainer().getEkvJABasisJahrPlus1() : null, //
			gesuchsteller1.getEinkommensverschlechterungContainer() != null ? gesuchsteller1.getEinkommensverschlechterungContainer().getEkvJABasisJahrPlus2() : null, //
			gesuch.extractEinkommensverschlechterungInfo());

		return finanzSituationPrintGesuchsteller;
	}

	/**
	 * Erstellt das FinanzSituationGesuchsteller fuer Gesuchsteller 2
	 *
	 * @return FinanzSituationGesuchsteller
	 */
	public static FinanzSituationPrintGesuchsteller getFinanzSituationGesuchsteller2(Gesuch gesuch) {

		GesuchstellerContainer gesuchsteller2 = gesuch.getGesuchsteller2();
		if (gesuchsteller2 != null) {
			Validate.notNull(gesuchsteller2.getFinanzielleSituationContainer());
			FinanzSituationPrintGesuchsteller finanzSituationPrintGesuchsteller2 = new FinanzSituationPrintGesuchsteller(
				gesuchsteller2.getFinanzielleSituationContainer().getFinanzielleSituationJA(), //
				gesuchsteller2.getEinkommensverschlechterungContainer() != null ? gesuchsteller2.getEinkommensverschlechterungContainer().getEkvJABasisJahrPlus1() : null, //
				gesuchsteller2.getEinkommensverschlechterungContainer() != null ? gesuchsteller2.getEinkommensverschlechterungContainer().getEkvJABasisJahrPlus2() : null, //
				gesuch.extractEinkommensverschlechterungInfo());
			return finanzSituationPrintGesuchsteller2;
		}
		return null;
	}
}
