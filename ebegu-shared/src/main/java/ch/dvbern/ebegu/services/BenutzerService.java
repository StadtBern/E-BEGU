package ch.dvbern.ebegu.services;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;

import ch.dvbern.ebegu.entities.Benutzer;

/**
 * Service fuer die Verwaltung von Benutzern
 */
public interface BenutzerService {

	/**
	 * Aktualisiert den Benutzer in der DB or erstellt ihn wenn er noch nicht existiert
	 *
	 * @param benutzer die Benutzer als DTO
	 * @return Die aktualisierte Benutzer
	 */
	@Nonnull
	Benutzer saveBenutzer(@Nonnull Benutzer benutzer);

	/**
	 * @param username PK (id) des Benutzers
	 * @return Benutzer mit dem gegebenen key oder null falls nicht vorhanden
	 */
	@Nonnull
	Optional<Benutzer> findBenutzer(@Nonnull String username);

	/**
	 * Gibt alle existierenden Benutzer zurueck.
	 *
	 * @return Liste aller Benutzern aus der DB
	 */
	@Nonnull
	Collection<Benutzer> getAllBenutzer();

	/**
	 * Gibt alle existierenden Benutzer mit Rolle Sachbearbeiter_JA oder Admin zurueck.
	 *
	 * @return Liste aller Benutzern mit entsprechender Rolle aus der DB
	 */
	@Nonnull
	Collection<Benutzer> getBenutzerJAorAdmin();

	/**
	 * @return Liste saemtlicher Gesuchsteller aus der DB
	 */
	@Nonnull
	Collection<Benutzer> getGesuchsteller();

	/**
	 * entfernt die Benutzer aus der Database
	 *
	 * @param username die Benutzer als DTO
	 */
	void removeBenutzer(@Nonnull String username);

	/**
	 * Gibt den aktuell eingeloggten Benutzer zurueck
	 */
	@Nonnull
	Optional<Benutzer> getCurrentBenutzer();

	/**
	 * inserts a user received from iam or updates it if it alreday exists
	 */
	Benutzer updateOrStoreUserFromIAM(Benutzer benutzer);
}
