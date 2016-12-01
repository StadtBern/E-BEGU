package ch.dvbern.ebegu.vorlagen.nichteintreten;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.ebegu.vorlagen.BriefPrintImpl;
import ch.dvbern.ebegu.vorlagen.PrintUtil;

/**
 * Copyright (c) 2016 DV Bern AG, Switzerland
 * <p>
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 * <p>
 * Created by medu on 28/11/2016.
 */
public class NichteintretenPrintImpl extends BriefPrintImpl implements NichteintretenPrint {

	private Betreuung betreuung;

	public NichteintretenPrintImpl(Betreuung betreuung) {

		super(betreuung.extractGesuch());

		this.betreuung = betreuung;

	}

	@Override
	public String getFallNummer() {
		return PrintUtil.createFallNummerString(getGesuch());
	}

	@Override
	public String getFallDatum() {
		return Constants.DATE_FORMATTER.format(getGesuch().getFall().getTimestampErstellt());
	}

	@Override
	public String getPeriode() {
		return "(" + getGesuch().getGesuchsperiode().getGueltigkeit().getGueltigAb().getYear()
			+ "/" + getGesuch().getGesuchsperiode().getGueltigkeit().getGueltigBis().getYear() + ")";
	}

	@Override
	public String getAngebotVon() {
		return Constants.DATE_FORMATTER.format(getGesuch().getGesuchsperiode().getGueltigkeit().getGueltigAb());
	}

	@Override
	public String getAngebotBis() {
		return Constants.DATE_FORMATTER.format(getGesuch().getGesuchsperiode().getGueltigkeit().getGueltigBis());
	}

	@Override
	//Paola Huber, Angebot Kindertagesstätte Elfenau (16.000701.2.1)
	public String getAngebotName() {
		return betreuung.getKind().getKindJA().getFullName()
			+ ", Angebot " + betreuung.getInstitutionStammdaten().getInstitution().getName()
			+ " (" + betreuung.getBGNummer() + ")";
	}
}
