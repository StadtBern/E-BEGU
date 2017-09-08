package ch.dvbern.ebegu.api.resource.authentication;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.dvbern.ebegu.api.AuthConstants;
import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxAuthAccessElementCookieData;
import ch.dvbern.ebegu.api.dtos.JaxAuthLoginElement;
import ch.dvbern.ebegu.authentication.AuthAccessElement;
import ch.dvbern.ebegu.authentication.AuthLoginElement;
import ch.dvbern.ebegu.config.EbeguConfiguration;
import ch.dvbern.ebegu.entities.AuthorisierterBenutzer;
import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.enums.UserRole;
import ch.dvbern.ebegu.services.AuthService;
import ch.dvbern.ebegu.services.BenutzerService;

/**
 * This resource has functions to login or logout
 */
@Stateless
@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

	private static final Logger LOG = LoggerFactory.getLogger(AuthResource.class);

	@Inject // @EJB
	private AuthService authService;

	@Context
	private HttpServletRequest request;
	@Inject
	private BenutzerService benutzerService;

	@Inject
	private JaxBConverter converter;

	@Inject
	private FedletURLInitializer fedletURLInitializer;

	@Inject
	private UsernameRoleChecker usernameRoleChecker;

	@Inject
	private EbeguConfiguration configuration;

	@Inject
	private LoginProviderInfoRestService loginProviderInfoRestService;


	@Path("/singleSignOn")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	@PermitAll
	public Response initSSOLogin(@Nullable @QueryParam("relayPath") String relayPath) {

		String url = this.loginProviderInfoRestService.getSSOLoginInitURL(relayPath);
		LOG.warn("Received URL to init single sign on login '{}'", url);
		return Response.ok(url).build();

//		try {
//
//			URIBuilder builder = new URIBuilder(fedletURLInitializer.getSSOInitURI());
//			//prueft ob er relay state param in der Liste der relayStateUrlList in der config entahlten ist
//			if (isRelayStateURLValid(relayPath)) {
//				builder.addParameter("RelayState", relayPath);
//			} else {
//				LOG.warn("RelayState Param '{}' seems to be invalid, ignoring", relayPath);
//			}
//			String initSSOURI = request.getContextPath() + builder.build();
//			return Response.ok(initSSOURI).build();
//
//			//wahrscheinlich muss man redirect url hier als string zurueckgeben da der aufruf ja nicht async sein sollte
//		} catch (URISyntaxException e) {
//			throw new EbeguRuntimeException("initSSOLogin", "Could not appendRelay Path to  SSOURL for Saml Login", e);
//		}

	}

//	private boolean isRelayStateURLValid(String relayPath) {
//		return relayPath != null && !relayPath.isEmpty()
//			&& SAML2Utils.isRelayStateURLValid(fedletURLInitializer.getSpMetaAlias(), relayPath, SAML2Constants.SP_ROLE)
//			&& ESAPI.validator().isValidInput("RelayState", relayPath, "URL", 2000, true);
//	}

	@Path("/singleLogout")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	@PermitAll
	public Response initSingleLogout(@Nullable @QueryParam("relayPath") String relayPath,
		@CookieParam(AuthConstants.COOKIE_AUTH_TOKEN) Cookie authTokenCookie) {

		if (authTokenCookie != null && authTokenCookie.getValue() != null) {
			Optional<AuthorisierterBenutzer> currentAuthOpt = authService.validateAndRefreshLoginToken(authTokenCookie.getValue(), false);
			if (currentAuthOpt.isPresent()) {
				String nameID = currentAuthOpt.get().getSamlNameId();
				String sessionID = currentAuthOpt.get().getSessionIndex();
				if (sessionID != null) {
					String logoutUrl = loginProviderInfoRestService.getSingleLogoutURL(relayPath, nameID, sessionID);

					return Response.ok(logoutUrl).build();
				}
			}
		}

		return Response.ok("").build(); //dummy
	}

	/**
	 * extrahiert die Daten aus dem DTO und versucht einzuloggen. Fuer das einloggen
	 * Fuer das Login schreiben wir selber eine Logik die direkt ohne Loginmodul ueber den Service einloggt.
	 * Dabei checken wir unser property File und suchen die gegebene Username/PW kombination
	 *
	 * @param loginElement Benutzer Identifikation (Benutzername/Passwort)
	 * @return im Erfolgsfall eine HTTP Response mit Cookies
	 */
	@Nullable
	@POST
	@Path("/login")
	@PermitAll
	public Response login(@Nonnull JaxAuthLoginElement loginElement) {
		if (configuration.isDummyLoginEnabled()) {

			// zuerst im Container einloggen, sonst schlaegt in den Entities die Mandanten-Validierung fehl
			if (!usernameRoleChecker.checkLogin(loginElement.getUsername(), loginElement.getPassword())) {
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
			//wir machen kein rollenmapping sondern versuchen direkt in enum zu transformieren
			String roleString = usernameRoleChecker.getSingleRole(loginElement.getUsername());
			UserRole validRole = UserRole.valueOf(roleString);

			AuthLoginElement login = new AuthLoginElement(loginElement.getUsername(), loginElement.getPassword(),
				loginElement.getNachname(), loginElement.getVorname(), loginElement.getEmail(), validRole);

			// Der Benutzer wird gesucht. Wenn er noch nicht existiert wird er erstellt und wenn ja dann aktualisiert
			Benutzer benutzer = new Benutzer();
			Optional<Benutzer> optBenutzer = benutzerService.findBenutzer(loginElement.getUsername());
			if (optBenutzer.isPresent()) {
				benutzer = optBenutzer.get();
			}
			benutzerService.saveBenutzer(converter.authLoginElementToBenutzer(loginElement, benutzer));

			Optional<AuthAccessElement> accessElement = authService.login(login);
			if (!accessElement.isPresent()) {
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
			AuthAccessElement access = accessElement.get();
			JaxAuthAccessElementCookieData element = convertToJaxAuthAccessElement(access);
			boolean cookieSecure = isCookieSecure();

			// Cookie to store auth_token, HTTP-Only Cookie --> Protection from XSS
			NewCookie authCookie = new NewCookie(AuthConstants.COOKIE_AUTH_TOKEN, access.getAuthToken(),
				AuthConstants.COOKIE_PATH, AuthConstants.COOKIE_DOMAIN, "authentication", AuthConstants.COOKIE_TIMEOUT_SECONDS, cookieSecure, true);
			// Readable Cookie for XSRF Protection (the Cookie can only be read from our Domain)
			NewCookie xsrfCookie = new NewCookie(AuthConstants.COOKIE_XSRF_TOKEN, access.getXsrfToken(),
				AuthConstants.COOKIE_PATH, AuthConstants.COOKIE_DOMAIN, "XSRF", AuthConstants.COOKIE_TIMEOUT_SECONDS, cookieSecure, false);
			// Readable Cookie storing user data
			NewCookie principalCookie = new NewCookie(AuthConstants.COOKIE_PRINCIPAL, encodeAuthAccessElement(element),
				AuthConstants.COOKIE_PATH, AuthConstants.COOKIE_DOMAIN, "principal", AuthConstants.COOKIE_TIMEOUT_SECONDS, cookieSecure, false);

			return Response.ok().cookie(authCookie, xsrfCookie, principalCookie).build();
		} else {
			LOG.warn("Dummy Login is disabled, returning 410");
			return Response.status(Response.Status.GONE).build();
		}

	}

	private JaxAuthAccessElementCookieData convertToJaxAuthAccessElement(AuthAccessElement access) {
		return new JaxAuthAccessElementCookieData(
			access.getAuthId(),
			String.valueOf(access.getNachname()),
			String.valueOf(access.getVorname()),
			String.valueOf(access.getEmail()),
			access.getRole() != null ? String.valueOf(access.getRole()) : null);
	}


	private boolean isCookieSecure() {
		return request.isSecure();
	}

	@POST
	@Path("/logout")
	@PermitAll
	public Response logout(@CookieParam(AuthConstants.COOKIE_AUTH_TOKEN) Cookie authTokenCookie) {
		try {
			if (authTokenCookie != null) {
				String authToken = Objects.requireNonNull(authTokenCookie.getValue());
				boolean cookieSecure = isCookieSecure();

				if (authService.logout(authToken)) {
					// Respond with expired cookies
					NewCookie authCookie = expireCookie(AuthConstants.COOKIE_AUTH_TOKEN, cookieSecure, true);
					NewCookie xsrfCookie = expireCookie(AuthConstants.COOKIE_XSRF_TOKEN, cookieSecure, false);
					NewCookie principalCookie = expireCookie(AuthConstants.COOKIE_PRINCIPAL, cookieSecure, false);
					return Response.ok().cookie(authCookie, xsrfCookie, principalCookie).build();
				}
			}
			return Response.ok().build();
		} catch (NoSuchElementException e) {
			LOG.info("Token Decoding from Cookies failed", e);
			return Response.ok().build();
		}
	}

	@Nonnull
	private NewCookie expireCookie(@Nonnull String name, boolean secure, boolean httpOnly) {
		return new NewCookie(name, "", AuthConstants.COOKIE_PATH, AuthConstants.COOKIE_DOMAIN, "", 0, secure, httpOnly);
	}

	/**
	 * @param element zu codirendes AuthAccessElement
	 * @return Base64 encoded JSON representation
	 */
	private String encodeAuthAccessElement(JaxAuthAccessElementCookieData element) {
		Gson gson = new Gson();
		return Base64.getEncoder().encodeToString(gson.toJson(element).getBytes(Charset.forName("UTF-8")));
	}
}
