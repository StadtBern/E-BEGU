package ch.dvbern.ebegu.testfaelle;

import java.time.LocalDate;
import java.util.Collection;

import ch.dvbern.ebegu.entities.Familiensituation;
import ch.dvbern.ebegu.entities.FamiliensituationContainer;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.Gesuchsperiode;
import ch.dvbern.ebegu.entities.InstitutionStammdaten;
import ch.dvbern.ebegu.enums.EnumFamilienstatus;
import ch.dvbern.ebegu.enums.EnumGesuchstellerKardinalitaet;

/**
 * Superklasse für ASIV-Testfaelle
 */
public abstract class AbstractASIVTestfall extends AbstractTestfall {

	public AbstractASIVTestfall(Gesuchsperiode gesuchsperiode, Collection<InstitutionStammdaten> institutionStammdatenList,
		boolean betreuungenBestaetigt) {
		super(gesuchsperiode, institutionStammdatenList, betreuungenBestaetigt);
	}

	public abstract Gesuch createMutation(Gesuch erstgesuch);

	protected Gesuch createAlleinerziehend(Gesuch gesuch, LocalDate ereignisdatum) {
		// Familiensituation
		Familiensituation familiensituation = new Familiensituation();
		familiensituation.setFamilienstatus(EnumFamilienstatus.ALLEINERZIEHEND);
		familiensituation.setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ALLEINE);
		FamiliensituationContainer familiensituationContainer = new FamiliensituationContainer();
		familiensituationContainer.setFamiliensituationJA(familiensituation);
		familiensituation.setAenderungPer(ereignisdatum);

		Familiensituation familiensituationErstgesuch = new Familiensituation();
		familiensituationErstgesuch.setFamilienstatus(EnumFamilienstatus.VERHEIRATET);
		familiensituationErstgesuch.setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ZU_ZWEIT);
		familiensituationErstgesuch.setGemeinsameSteuererklaerung(Boolean.TRUE);
		familiensituationContainer.setFamiliensituationErstgesuch(familiensituationErstgesuch);

		gesuch.setFamiliensituationContainer(familiensituationContainer);
		return gesuch;
	}

	protected Gesuch createVerheiratet(Gesuch gesuch, LocalDate ereignisdatum) {
		// Familiensituation
		Familiensituation familiensituation = new Familiensituation();
		familiensituation.setFamilienstatus(EnumFamilienstatus.VERHEIRATET);
		familiensituation.setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ZU_ZWEIT);
		familiensituation.setGemeinsameSteuererklaerung(Boolean.TRUE);
		FamiliensituationContainer familiensituationContainer = new FamiliensituationContainer();
		familiensituationContainer.setFamiliensituationJA(familiensituation);
		familiensituation.setAenderungPer(ereignisdatum);

		Familiensituation familiensituationErstgesuch = new Familiensituation();
		familiensituationErstgesuch.setFamilienstatus(EnumFamilienstatus.ALLEINERZIEHEND);
		familiensituationErstgesuch.setGesuchstellerKardinalitaet(EnumGesuchstellerKardinalitaet.ALLEINE);
		familiensituationContainer.setFamiliensituationErstgesuch(familiensituationErstgesuch);

		gesuch.setFamiliensituationContainer(familiensituationContainer);
		return gesuch;
	}
}
