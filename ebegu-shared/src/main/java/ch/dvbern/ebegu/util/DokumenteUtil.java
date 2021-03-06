/*
 * Ki-Tax: System for the management of external childcare subsidies
 * Copyright (C) 2017 City of Bern Switzerland
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.dvbern.ebegu.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import ch.dvbern.ebegu.entities.DokumentGrund;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.KindContainer;
import ch.dvbern.ebegu.enums.DokumentGrundPersonType;
import ch.dvbern.ebegu.enums.GeneratedDokumentTyp;
import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * Allgemeine Utils fuer Dokumente
 */
public final class DokumenteUtil {

	private DokumenteUtil() {
	}

	/**
	 * Zusammenfügen der benötigten Dokument-Gruende (Dokumente die gem. den Angeben des GS gebraucht werden  und der
	 * Dokument-Gruende auf der DB (vorhandene Dokumente). Das entspricht allso einer Union der beiden Sets
	 */
	public static Set<DokumentGrund> mergeNeededAndPersisted(Set<DokumentGrund> dokumentGrundsNeeded, Collection<DokumentGrund> persistedDokumentGrunds, Gesuch gesuch) {

		Set<DokumentGrund> dokumentGrundsMerged = new HashSet<>();
		Set<DokumentGrund> persistedDokumentAdded = new HashSet<>();

		// Ersetzen des Placeholder mit dem vorhandenen Dokument, falls schon ein Dokument gespeichert wurde...
		for (DokumentGrund dokumentGrundNeeded : dokumentGrundsNeeded) {
			Set<DokumentGrund> persistedForNeeded = getPersistedForNeeded(persistedDokumentGrunds, dokumentGrundNeeded, gesuch);

			if (!persistedForNeeded.isEmpty()) {
				persistedDokumentAdded.addAll(persistedForNeeded);
				dokumentGrundsMerged.addAll(persistedForNeeded);
			} else {
				dokumentGrundsMerged.add(dokumentGrundNeeded);
			}
		}

		//Hinzufügen der vorhandenen Dokumente welche jedoch eigentlich nicht mehr benötigt werden.
		persistedDokumentGrunds.removeAll(persistedDokumentAdded);
		for (DokumentGrund persistedDokumentGrund : persistedDokumentGrunds) {
			persistedDokumentGrund.setNeeded(false);
			dokumentGrundsMerged.add(persistedDokumentGrund);
		}

		return dokumentGrundsMerged;

	}

	private static Set<DokumentGrund> getPersistedForNeeded(Collection<DokumentGrund> persistedDokumentGrunds, DokumentGrund dokumentGrundNeeded, Gesuch gesuch) {
		Set<DokumentGrund> persisted = new HashSet<>();
		for (DokumentGrund persistedDokumentGrund : persistedDokumentGrunds) {
			if (compareDokumentGrunds(persistedDokumentGrund, dokumentGrundNeeded, gesuch) == 0) {
				persisted.add(persistedDokumentGrund);
			}
		}
		return persisted;
	}

	/**
	 * Compares two DokumentGrund. In order to support the old version where the name of the linked person
	 * was saved statically in the DokumentGrund we cannot just compare both elements as usual. We need to check
	 * if it is an old DokumentGrund (FREETEXT) and in that case we have to extract the name of the Gesuchsteller
	 * or Kind to compare it with the fullName.
	 */
	public static int compareDokumentGrunds(DokumentGrund persistedDok, DokumentGrund neededDok, final Gesuch gesuch) {
		CompareToBuilder builder = new CompareToBuilder();
		builder.append(persistedDok.getDokumentGrundTyp(), neededDok.getDokumentGrundTyp());
		builder.append(persistedDok.getDokumentTyp(), neededDok.getDokumentTyp());
		if (persistedDok.getTag() != null && neededDok.getTag() != null) {
			builder.append(persistedDok.getTag(), neededDok.getTag());
		}
		if (persistedDok.getPersonType() != null && neededDok.getPersonType() != null) {
			// when persistedDokument is an old one ie.e. FREETEXT, then we compare fullname of
			// the persisted one with the calculated name of the needed one.
			if (persistedDok.getPersonType() == DokumentGrundPersonType.FREETEXT) {
				final String fullNameNeededDok = fullNameNeededDok(gesuch, neededDok.getPersonType(), neededDok.getPersonNumber(), neededDok.getFullName());
				if (persistedDok.getFullName() != null && fullNameNeededDok != null) {
					builder.append(persistedDok.getFullName(), fullNameNeededDok);
				}
			} else {
				// in this case the persistedDok was created after the implementation of personType
				// and can therefore be compared normally. In this case fullName doesn't matter
				builder.append(persistedDok.getPersonType(), neededDok.getPersonType());
				if (persistedDok.getPersonNumber() != null && neededDok.getPersonNumber() != null) {
					builder.append(persistedDok.getPersonNumber(), neededDok.getPersonNumber());
				}
			}
		}
		return builder.toComparison();
	}

	private static String fullNameNeededDok(Gesuch gesuch, DokumentGrundPersonType personType, Integer personNumber, String fullName) {
		if (personType != null) {
			if (personType == DokumentGrundPersonType.GESUCHSTELLER) {
				if (personNumber == 1 && gesuch.getGesuchsteller1() != null) {
					return gesuch.getGesuchsteller1().extractFullName();
				} else if (personNumber == 2 && gesuch.getGesuchsteller2() != null) {
					return gesuch.getGesuchsteller2().extractFullName();
				}
			} else if (personType == DokumentGrundPersonType.KIND) {
				final KindContainer kindContainer = gesuch.extractKindFromKindNumber(personNumber);
				if (kindContainer != null) {
					return kindContainer.getKindJA().getFullName();
				}
			}
		}
		return fullName;
	}

	/**
	 * Fuer den gegebenen GeneratedDokumentTyp gibt die Methode den entsprechenden Dateinamen zurueck.
	 */
	@Nonnull
	public static String getFileNameForGeneratedDokumentTyp(final GeneratedDokumentTyp typ, final String identificationNumber) {
		//Liste in server-messages.properties erganzen.
		switch (typ) {
		case BEGLEITSCHREIBEN:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.BEGLEITSCHREIBEN, identificationNumber);
		case FINANZIELLE_SITUATION:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.FINANZIELLE_SITUATION, identificationNumber);
		case VERFUEGUNG:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.VERFUEGUNG, identificationNumber);
		case MAHNUNG:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.MAHNUNG, identificationNumber);
		case NICHTEINTRETEN:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.NICHTEINTRETEN, identificationNumber);
		case FREIGABEQUITTUNG:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.FREIGABEQUITTUNG, identificationNumber);
		case PAIN001:
			return ServerMessageUtil.translateEnumValue(GeneratedDokumentTyp.PAIN001, identificationNumber);
		default:
			return "file.pdf";
		}
	}
}
