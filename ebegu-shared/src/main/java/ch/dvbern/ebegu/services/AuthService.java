package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.authentication.AuthAccessElement;
import ch.dvbern.ebegu.authentication.AuthLoginElement;
import ch.dvbern.ebegu.entities.AuthorisierterBenutzer;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Service fuer die Authentifizierung eines Benutzers
 */
public interface AuthService {

	/**
	 * Verifiziert die User Credentials, Wenn erfolgreich wird das login als authorisierter_benutzer persisstiert
	 *
	 * @param loginElement beinhaltet die User Credentials
	 * @return Authentication Response, falls das Login erfolgriech war, sonst NULL
	 */
	@Nonnull
	Optional<AuthAccessElement> login(@Nonnull AuthLoginElement loginElement);

	/**
	 * @param authToken Authentifizierungs Token Identifikation
	 * @return TRUE falls das Logout erfolgreich war, sonst FALSE
	 */
	boolean logout(@Nonnull final String authToken);


	@Nonnull
	AuthAccessElement createLoginFromIAM(AuthorisierterBenutzer authorisierterBenutzer);

	/**
	 * gets the logged in user based on the login token
	 * if doRefresh is true, the login cookie gets refreshed.
	 */
	Optional<AuthorisierterBenutzer> validateAndRefreshLoginToken(String token, boolean doRefreshToken);
}
