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

package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.Verfuegung;
import ch.dvbern.ebegu.entities.VerfuegungZeitabschnitt;
import ch.dvbern.ebegu.enums.Betreuungsstatus;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service zum berechnen und speichern der Verfuegung
 */
public interface VerfuegungService {

	/**
	 * Speichert die Verfuegung neu in der DB falls der Key noch nicht existiert.
	 * Die Betreuung erhaelt den Status VERFUEGT
	 *
	 * @param verfuegung Die Verfuegung als DTO
	 * @param betreuungId Id der Betreuung auf die die verfuegung gespeichet werden soll
	 * @param ignorieren true wenn die ausbezahlten Zeitabschnitte nicht neu berechnet werden muessen
	 */
	@Nonnull
	Verfuegung verfuegen(@Nonnull Verfuegung verfuegung, @Nonnull String betreuungId, boolean ignorieren);

	/**
	 * Generiert das Verfuegungsdokument.
	 *
	 * @param betreuung Betreuung, fuer die das Dokument generiert werden soll.
	 */
	void generateVerfuegungDokument(@Nonnull Betreuung betreuung);

	/**
	 * Aendert den Status der Zahlung auf NEU oder IGNORIEREND fuer alle Zahlungen wo etwas korrigiert wurde.
	 * Wird auf NEU gesetzt wenn ignorieren==false, sonst wird es auf IGNORIEREND gesetzt.
	 */
	@SuppressWarnings("LocalVariableNamingConvention")
	void setZahlungsstatus(Verfuegung verfuegung, @Nonnull String betreuungId, boolean ignorieren);

	/**
	 * Speichert die Verfuegung neu in der DB falls der Key noch nicht existiert.
	 * Die Betreuung erhaelt den Status NICHT_EINGETRETEN
	 *
	 * @param verfuegung Die Verfuegung als DTO
	 * @param betreuungId Id der Betreuung auf die die verfuegung gespeichet werden soll
	 */
	@Nonnull
	Verfuegung nichtEintreten(@Nonnull Verfuegung verfuegung, @Nonnull String betreuungId);

	/**
	 * Speichert die Verfuegung und setzt die Betreuung in den uebergebenen Status
	 */
	@Nonnull
	Verfuegung persistVerfuegung(@Nonnull Verfuegung verfuegung, @Nonnull String betreuungId, @Nonnull Betreuungsstatus betreuungsstatus);

	/**
	 * @param id PK (id) der Verfuegung
	 * @return Verfuegung mit dem gegebenen key oder null falls nicht vorhanden
	 */
	@Nonnull
	Optional<Verfuegung> findVerfuegung(@Nonnull String id);

	/**
	 * @return Liste aller Verfuegung aus der DB
	 */
	@Nonnull
	Collection<Verfuegung> getAllVerfuegungen();

	/**
	 * entfernt eine Verfuegung aus der Databse
	 *
	 * @param verfuegung Verfuegung zu entfernen
	 */
	void removeVerfuegung(@Nonnull Verfuegung verfuegung);

	/**
	 * Berechnet die Verfuegung fuer ein Gesuch
	 *
	 * @return gibt die Betreuung mit der berechneten angehangten Verfuegung zurueck
	 */
	@Nonnull
	Gesuch calculateVerfuegung(@Nonnull Gesuch gesuch);

	Verfuegung getEvaluateFamiliensituationVerfuegung(@Nonnull Gesuch gesuch);

	/**
	 * gibt die Verfuegung der vorherigen verfuegten Betreuung zurueck.
	 * kann null sein
	 *
	 * @return Verfuegung oder null falls nicht vorhanden
	 */
	@Nonnull
	Optional<Verfuegung> findVorgaengerVerfuegung(@Nonnull Betreuung betreuung);

	/**
	 * genau wie findVorgaengerVerfuegung gibt aber nur deren TimestampErstellt zurueck wenn vorhanden
	 */
	Optional<LocalDate> findVorgaengerVerfuegungDate(@Nonnull Betreuung betreuung);

	/**
	 * Sucht den Zeitabschnitt / die Zeitabschnitte mit demselben Zeitraum auf der Vorgängerverfügung,
	 * und die verrechnet oder ignoriert sind. Rekursive Methode, die die gegebene Liste mit den richtigen Objekten ausfuellt
	 */
	void findVerrechnetenZeitabschnittOnVorgaengerVerfuegung(@Nonnull VerfuegungZeitabschnitt zeitabschnittNeu,
		@Nonnull Betreuung betreuungNeu, @Nonnull List<VerfuegungZeitabschnitt> vorgaengerZeitabschnitte);
}
