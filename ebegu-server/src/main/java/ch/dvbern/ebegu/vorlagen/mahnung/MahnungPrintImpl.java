package ch.dvbern.ebegu.vorlagen.mahnung;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.dvbern.ebegu.entities.KindContainer;
import ch.dvbern.ebegu.entities.Mahnung;
import ch.dvbern.ebegu.enums.MahnungTyp;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.ebegu.vorlagen.AufzaehlungPrint;
import ch.dvbern.ebegu.vorlagen.AufzaehlungPrintImpl;
import ch.dvbern.ebegu.vorlagen.BriefPrintImpl;

/**
 * Copyright (c) 2016 DV Bern AG, Switzerland
 * <p>
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 * <p>
 * Created by medu on 16/11/2016.
 */
public class MahnungPrintImpl extends BriefPrintImpl implements ManhungPrint {

	private final Mahnung mahnung;
	private Mahnung vorgaengerMahnung;

	public MahnungPrintImpl(Mahnung mahnung, Optional<Mahnung> vorgaengerMahnung) {

		super(mahnung.getGesuch());
		this.mahnung = mahnung;

		if (mahnung.getMahnungTyp() == MahnungTyp.ZWEITE_MAHNUNG && vorgaengerMahnung.isPresent()) {
			this.vorgaengerMahnung = vorgaengerMahnung.get();
		} else if (mahnung.getMahnungTyp() == MahnungTyp.ZWEITE_MAHNUNG) {
			throw new UnsupportedOperationException("Vorganger Mahnung für zweite Mahnung fehlt!");
		}
	}

	private String concatenateAngebot(List<String> listAngebot) {
		StringBuilder angebot = new StringBuilder();

		for (int i = 0; i < listAngebot.size(); i++) {
			angebot.append(listAngebot.get(i));
			if (i + 2 == listAngebot.size() && listAngebot.size() > 1) {
				angebot.append(" und ");
			} else if (i + 1 < listAngebot.size()) {
				angebot.append(", ");
			}
		}

		return angebot.toString();
	}


	@Override
	public String getAngebotFull() {

		List<String> listAngebot = new ArrayList<>();

		for (KindContainer kindContainer : getGesuch().getKindContainers()) {
			listAngebot.addAll(
				kindContainer.getBetreuungen().stream()
					.map(betreuung -> betreuung.getKind().getKindJA().getFullName() + " (" + betreuung.getInstitutionStammdaten().getInstitution().getName() + ")")
					.collect(Collectors.toList()));
		}

		return concatenateAngebot(listAngebot);

	}

	@Override
	public String getAngebotShort() {

		List<String> listAngebot = getGesuch().getKindContainers().stream()
			.filter(kindContainer -> !kindContainer.getBetreuungen().isEmpty())
			.map(kindContainer -> kindContainer.getKindJA().getFullName())
			.collect(Collectors.toList());

		return concatenateAngebot(listAngebot);

	}

	@Override
	public String getEingangsDatum() {
		LocalDate eingangsdatum = mahnung.getGesuch().getEingangsdatum() != null ? mahnung.getGesuch().getEingangsdatum() : LocalDate.now();
		return Constants.DATE_FORMATTER.format(eingangsdatum);
	}

	@Override
	public List<AufzaehlungPrint> getFehlendeUnterlagen() {

		List<AufzaehlungPrint> fehlendeUnterlagen = new ArrayList<>();

		String[] splitFehlendeUnterlagen = mahnung.getBemerkungen().split("[" + System.getProperty("line.separator") + "]+");
		for (String fehlendeUnterlage : splitFehlendeUnterlagen) {
			fehlendeUnterlagen.add(new AufzaehlungPrintImpl(fehlendeUnterlage));
		}
		return fehlendeUnterlagen;
	}

	@Override
	public String getMahnFristDatum() {
		return Constants.DATE_FORMATTER.format(mahnung.getDatumFristablauf());
	}

	@Override
	public String getErsteMahnDatum() {
		if (mahnung.getMahnungTyp() == MahnungTyp.ZWEITE_MAHNUNG && vorgaengerMahnung != null) {
			return Constants.DATE_FORMATTER.format(vorgaengerMahnung.getTimestampErstellt());
		} else {
			return "";
		}
	}

	@Override
	public String getKontaktStelle() {
		return "während der Bürozeiten zur Verfügung (Telefonnummer 031 321 51 15 und per E-Mail kinderbetreuung@bern.ch)";
	}

}
