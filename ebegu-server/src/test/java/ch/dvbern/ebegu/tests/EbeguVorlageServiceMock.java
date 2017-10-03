package ch.dvbern.ebegu.tests;

import java.time.LocalDate;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import ch.dvbern.ebegu.entities.EbeguVorlage;
import ch.dvbern.ebegu.entities.Vorlage;
import ch.dvbern.ebegu.enums.EbeguVorlageKey;
import ch.dvbern.ebegu.services.EbeguVorlageServiceBean;
import ch.dvbern.ebegu.types.DateRange;

/**
 * Copyright (c) 2016 DV Bern AG, Switzerland
 * <p>
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 * <p>
 * Created by medu on 21/11/2016.
 */
public class EbeguVorlageServiceMock extends EbeguVorlageServiceBean {

	@Nonnull
	@Override
	public Optional<EbeguVorlage> getEbeguVorlageByDatesAndKey(@Nonnull LocalDate abDate, @Nonnull LocalDate bisDate, @Nonnull EbeguVorlageKey ebeguVorlageKey, EntityManager em) {
		EbeguVorlage ebeguVorlage = new EbeguVorlage(ebeguVorlageKey, new DateRange(abDate, bisDate));
		Vorlage vorlage = new Vorlage();

		switch (ebeguVorlageKey) {
		case VORLAGE_MAHNUNG_1:
			vorlage.setFilepfad("vorlagen/1_Mahnung.docx");
			break;
		case VORLAGE_MAHNUNG_2:
			vorlage.setFilepfad("vorlagen/2_Mahnung.docx");
			break;
		case VORLAGE_NICHT_EINTRETENSVERFUEGUNG:
			vorlage.setFilepfad("vorlagen/Nichteintretensverfuegung.docx");
			break;
		case VORLAGE_INFOSCHREIBEN_MAXIMALTARIF:
			vorlage.setFilepfad("vorlagen/Infoschreiben_Maxtarif.docx");
			break;
		case VORLAGE_FREIGABEQUITTUNG:
			vorlage.setFilepfad("vorlagen/Freigabequittung.docx");
			break;
		case VORLAGE_BEGLEITSCHREIBEN:
			vorlage.setFilepfad("vorlagen/Begleitschreiben.docx");
			break;
		case VORLAGE_FINANZIELLE_SITUATION:
			vorlage.setFilepfad("vorlagen/Berechnungsgrundlagen.docx");
			break;
		case VORLAGE_BRIEF_TAGESELTERN_SCHULKINDER:
			vorlage.setFilepfad("vorlagen/Verfuegungsmuster_tageseltern_schulkinder.docx");
			break;
		case VORLAGE_BRIEF_TAGESSTAETTE_SCHULKINDER:
			vorlage.setFilepfad("vorlagen/Verfuegungsmuster_tagesstaette_schulkinder.docx");
			break;
		case VORLAGE_VERFUEGUNG_KITA:
			vorlage.setFilepfad("vorlagen/Verfuegungsmuster_kita.docx");
			break;
		case VORLAGE_VERFUEGUNG_TAGESELTERN_KLEINKINDER:
			vorlage.setFilepfad("vorlagen/Verfuegungsmuster_tageseltern_kleinkinder.docx");
			break;
		default:
			break;
		}

		ebeguVorlage.setVorlage(vorlage);
		return Optional.of(ebeguVorlage);
	}
}
