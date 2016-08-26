package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.errors.MergeDocException;

import javax.annotation.Nonnull;
import java.util.List;

/*
* Copyright (c) 2016 DV Bern AG, Switzerland
*
* Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
* geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
* insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
* elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
* Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
*
* Ersteller: zeab am: 09.08.2016
*/
public interface PrintVerfuegungPDFService {

	/**
	 * Bereitet die Verfuegungsdokumente auf
	 *
	 * @param gesuch das Gesuch
	 * @return Liste der generierten Verfuegungsdokumente  pro Betreuung
	 * @throws MergeDocException Falls bei der Verfuegungsgenerierung einen Fehler auftritt
	 */
	@Nonnull
	List<byte[]> printVerfuegung(@Nonnull Gesuch gesuch) throws MergeDocException;
}
