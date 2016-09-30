package ch.dvbern.ebegu.vorlagen.verfuegung;
/*
* Copyright (c) 2016 DV Bern AG, Switzerland
*
* Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
* geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
* insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
* elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
* Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
*
* Ersteller: zeab am: 12.08.2016
*/

import ch.dvbern.ebegu.util.Constants;

import java.math.BigDecimal;

public class VerfuegungZeitabschnittPrintImpl implements VerfuegungZeitabschnittPrint {

	private ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt verfuegungZeitabschnitt;

	public VerfuegungZeitabschnittPrintImpl(ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt verfuegungZeitabschnitt) {
		this.verfuegungZeitabschnitt = verfuegungZeitabschnitt;
	}

	/**
	 * @return Von
	 */
	public String getVon() {

		return Constants.DATE_FORMATTER.format(verfuegungZeitabschnitt.getGueltigkeit().getGueltigAb());
	}

	/**
	 * @return Bis
	 */
	public String getBis() {

		return Constants.DATE_FORMATTER.format(verfuegungZeitabschnitt.getGueltigkeit().getGueltigBis());
	}

	/**
	 * @return Betreuung
	 */
	public int getBetreuung() {

		return verfuegungZeitabschnitt.getBetreuungspensum();
	}

	/**
	 * @return Anspruch
	 */
	public int getAnspruch() {

		return verfuegungZeitabschnitt.getAnspruchberechtigtesPensum();
	}

	/**
	 * @return BGPensum
	 */
	public int getBGPensum() {
		//hier wird das Minimum von (Rest)anspruch und von Betreuung zurueckgegeben. Dies enspricht der Definition des BG-Pensum
		return Math.min(getBetreuung(), getAnspruch());
	}

	/**
	 * @return Vollkosten
	 */
	public BigDecimal getVollkosten() {

		return verfuegungZeitabschnitt.getVollkosten();
	}

	/**
	 * @return Elternbeitrag
	 */
	public BigDecimal getElternbeitrag() {

		return verfuegungZeitabschnitt.getElternbeitrag();
	}

	/**
	 * @return Verguenstigung
	 */
	public BigDecimal getVerguenstigung() {

		return verfuegungZeitabschnitt.getVerguenstigung();
	}
}
