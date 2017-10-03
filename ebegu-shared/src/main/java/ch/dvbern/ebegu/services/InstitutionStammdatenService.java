package ch.dvbern.ebegu.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;

import ch.dvbern.ebegu.entities.InstitutionStammdaten;
import ch.dvbern.ebegu.enums.BetreuungsangebotTyp;

/**
 * Service zum Verwalten von InstitutionStammdaten
 */
public interface InstitutionStammdatenService {

	/**
	 * Erstellt eine InstitutionStammdaten in der DB. Wenn eine InstitutionStammdaten mit demselben ID bereits existiert
	 * wird diese dann aktualisiert.
	 *
	 * @param institutionStammdaten Die InstitutionStammdaten als DTO
	 */
	InstitutionStammdaten saveInstitutionStammdaten(InstitutionStammdaten institutionStammdaten);

	/**
	 * @param institutionStammdatenID PK (id) der InstitutionStammdaten
	 * @return InstitutionStammdaten mit dem gegebenen key oder null falls nicht vorhanden
	 */
	Optional<InstitutionStammdaten> findInstitutionStammdaten(String institutionStammdatenID);

	/**
	 * @return Aller InstitutionStammdaten aus der DB.
	 */
	Collection<InstitutionStammdaten> getAllInstitutionStammdaten();

	/**
	 * removes a InstitutionStammdaten from the Database.
	 *
	 * @param institutionStammdatenId PK (id) der InstitutionStammdaten
	 */
	void removeInstitutionStammdaten(@Nonnull String institutionStammdatenId);

	/**
	 * @param date Das Datum fuer welches die InstitutionStammdaten gesucht werden muessen
	 * @return Alle InstitutionStammdaten, bei denen das gegebene Datum zwischen datumVon und datumBis liegt
	 */
	Collection<InstitutionStammdaten> getAllInstitutionStammdatenByDate(LocalDate date);

	/**
	 * @param date Das Datum fuer welches die InstitutionStammdaten gesucht werden muessen
	 * @return Alle aktiven InstitutionStammdaten bei denen das gegebene Datum zwischen datumVon und datumBis liegt
	 */
	Collection<InstitutionStammdaten> getAllActiveInstitutionStammdatenByDate(LocalDate date);

	/**
	 * @param institutionId Die Institutions-id für welche alle Stammdaten gesucht werden sollen
	 * @return Alle InstitutionStammdaten, bei denen die Institution dem übergebenen id-Wert entspricht
	 */
	Collection<InstitutionStammdaten> getAllInstitutionStammdatenByInstitution(String institutionId);

	/**
	 * Gibt alle Betreuungsangebotstypen zurueck, welche die Institutionen anbieten, fuer welche der
	 * aktuell eingeloggte Benutzer berechtigt ist
	 */
	Collection<BetreuungsangebotTyp> getBetreuungsangeboteForInstitutionenOfCurrentBenutzer();
}
