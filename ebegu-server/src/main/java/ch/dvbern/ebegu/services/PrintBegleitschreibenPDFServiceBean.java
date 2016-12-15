package ch.dvbern.ebegu.services;
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

import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.enums.EbeguVorlageKey;
import ch.dvbern.ebegu.errors.MergeDocException;
import ch.dvbern.ebegu.types.DateRange;
import ch.dvbern.ebegu.vorlagen.GeneratePDFDocumentHelper;
import ch.dvbern.ebegu.vorlagen.begleitschreiben.BegleitschreibenPrintImpl;
import ch.dvbern.ebegu.vorlagen.begleitschreiben.BegleitschreibenPrintMergeSource;
import ch.dvbern.lib.doctemplate.common.DocTemplateException;
import ch.dvbern.lib.doctemplate.docx.DOCXMergeEngine;

import javax.annotation.Nonnull;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Implementiert VerfuegungsGenerierungPDFService
 */
@Stateless
@Local(PrintBegleitschreibenPDFService.class)
public class PrintBegleitschreibenPDFServiceBean extends AbstractPrintService implements PrintBegleitschreibenPDFService {

	@Inject
	private Authorizer authorizer;

	@Nonnull
	@Override
	public byte[] printBegleitschreiben(@Nonnull Gesuch gesuch) throws MergeDocException {

		Objects.requireNonNull(gesuch, "Das Argument 'gesuch' darf nicht leer sein");
		authorizer.checkReadAuthorization(gesuch);

		DOCXMergeEngine docxME = new DOCXMergeEngine("Begleitschreiben");

		try {
			final DateRange gueltigkeit = gesuch.getGesuchsperiode().getGueltigkeit();
			InputStream is = getVorlageStream(gueltigkeit.getGueltigAb(),
				gueltigkeit.getGueltigBis(), EbeguVorlageKey.VORLAGE_BEGLEITSCHREIBEN);
			Objects.requireNonNull(is, "Vorlage fuer Begleitschreiben nicht gefunden");
			byte[] bytes = new GeneratePDFDocumentHelper().generatePDFDocument(docxME.getDocument(is, new BegleitschreibenPrintMergeSource(new BegleitschreibenPrintImpl(gesuch))));
			is.close();
			return bytes;
		} catch (IOException |

				DocTemplateException e) {
			throw new MergeDocException("printBegleitschreiben()",
				"Bei der Generierung der Begleitschreibenvorlage ist ein Fehler aufgetreten", e, new Objects[] {});
		}
	}
}