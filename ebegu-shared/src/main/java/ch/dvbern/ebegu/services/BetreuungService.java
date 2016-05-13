package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Betreuung;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Service zum Verwalten von Betreuungen
 */
public interface BetreuungService {

	/**
	 * Speichert die Betreuung neu in der DB falls der Key noch nicht existiert. Sonst wird das existierende Kind aktualisiert
	 * @param betreuung Die Betreuung als DTO
	 */
	@Nonnull
	Betreuung saveBetreuung(@Nonnull Betreuung betreuung);

	/**

	 * @param key PK (id) der Betreuung
	 * @return Betreuung mit dem gegebenen key oder null falls nicht vorhanden
	 */
	@Nonnull
	Optional<Betreuung> findBetreuung(@Nonnull String key);
}
