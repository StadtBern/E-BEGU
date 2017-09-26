package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxGesuch;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.dtos.JaxKindContainer;
import ch.dvbern.ebegu.api.dtos.JaxVerfuegung;
import ch.dvbern.ebegu.api.util.RestUtil;
import ch.dvbern.ebegu.entities.*;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.enums.UserRole;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.*;
import ch.dvbern.lib.cdipersistence.Persistence;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * REST Resource fuer Verfügungen
 */
@Path("verfuegung")
@Stateless
@Api(description = "Resource für Verfügungen, inkl. Berechnung der Vergünstigung")
public class VerfuegungResource {

	@Inject
	private VerfuegungService verfuegungService;

	@Inject
	private GesuchService gesuchService;

	@Inject
	private BetreuungService betreuungService;

	@Inject
	private InstitutionService institutionService;

	@Inject
	private BenutzerService benutzerService;

	@Inject
	private MailService mailService;

	@SuppressWarnings("CdiInjectionPointsInspection")
	@Inject
	private JaxBConverter converter;

	@Resource
	private EJBContext context;    //fuer rollback

	@Inject
	private Persistence persistence;

	private static final Logger LOG = LoggerFactory.getLogger(VerfuegungResource.class.getSimpleName());


	@ApiOperation(value = "Calculates the Verfuegung of the Gesuch with the given id, does nothing if the Gesuch " +
		"does not exists. Note: Nothing is stored in the Database",
		responseContainer = "Set", response = JaxKindContainer.class)
	@Nullable
	@GET
	@Path("/calculate/{gesuchId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response calculateVerfuegung(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchstellerId,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Optional<Gesuch> gesuchOptional = gesuchService.findGesuch(gesuchstellerId.getId());

		try {
			if (!gesuchOptional.isPresent()) {
				return null;
			}
			Gesuch gesuch = gesuchOptional.get();
			Gesuch gesuchWithCalcVerfuegung = verfuegungService.calculateVerfuegung(gesuch);

			// Wir verwenden das Gesuch nur zur Berechnung und wollen nicht speichern, darum das Gesuch detachen
			loadRelationsAndDetach(gesuchWithCalcVerfuegung);

			JaxGesuch gesuchJax = converter.gesuchToJAX(gesuchWithCalcVerfuegung);

			Set<JaxKindContainer> kindContainers = gesuchJax.getKindContainers();
			Optional<Benutzer> currentBenutzer = benutzerService.getCurrentBenutzer();
			if (currentBenutzer.isPresent()) {
				UserRole currentUserRole = currentBenutzer.get().getRole();
				// Es wird gecheckt ob der Benutzer zu einer Institution/Traegerschaft gehoert. Wenn ja, werden die Kinder gefilter
				// damit nur die relevanten Kinder geschickt werden
				if (UserRole.SACHBEARBEITER_TRAEGERSCHAFT.equals(currentUserRole) || UserRole.SACHBEARBEITER_INSTITUTION.equals(currentUserRole)) {
					Collection<Institution> instForCurrBenutzer = institutionService.getAllowedInstitutionenForCurrentBenutzer();
					RestUtil.purgeKinderAndBetreuungenOfInstitutionen(kindContainers, instForCurrBenutzer);
				}
			}
			return Response.ok(kindContainers).build();

		} finally {
			// Wir verwenden das Gesuch nur zur Berechnung und wollen nicht speichern, darum Transaktion rollbacken
			// Dies muss insbesondere auch im Fehlerfall geschehen!
			try {
				context.setRollbackOnly();
			} catch (IllegalStateException ise) {
				LOG.error("Could not rollback Transaction!", ise);
			}
		}
	}

	//vorschlag: hier koennten wir auch nur die Bemerkungen vom client mitgeben und die Verfuegung nochmal neu berechnen.
	// Das ware sicherer gegen client manipulationen.
	@ApiOperation(value = "Speichert eine Verfuegung in der Datenbank", response = JaxVerfuegung.class)
	@Nullable
	@PUT
	@Path("/{gesuchId}/{betreuungId}/{ignorieren}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxVerfuegung saveVerfuegung(
		@Nonnull @NotNull @PathParam ("gesuchId") JaxId gesuchId,
		@Nonnull @NotNull @PathParam ("betreuungId") JaxId betreuungId,
		@Nonnull @NotNull @PathParam ("ignorieren") Boolean ignorieren,
		@Nonnull @NotNull @Valid JaxVerfuegung verfuegungJAXP) throws EbeguException {

		Optional<Gesuch> gesuch = gesuchService.findGesuch(gesuchId.getId());
		if (gesuch.isPresent()) {
			Optional<Betreuung> betreuung = betreuungService.findBetreuung(betreuungId.getId());
			if (betreuung.isPresent()) {
				Verfuegung verfuegungToMerge = new Verfuegung();
				if (verfuegungJAXP.getId() != null) {
					Optional<Verfuegung> optional = verfuegungService.findVerfuegung(verfuegungJAXP.getId());
					verfuegungToMerge = optional.orElse(new Verfuegung());
				}
				Verfuegung convertedVerfuegung = converter.verfuegungToEntity(verfuegungJAXP, verfuegungToMerge);

				Verfuegung persistedVerfuegung = this.verfuegungService.verfuegen(convertedVerfuegung, betreuung.get().getId(), ignorieren);
				mailService.sendInfoBetreuungVerfuegt(betreuung.get());

				return converter.verfuegungToJax(persistedVerfuegung);
			}
			throw new EbeguEntityNotFoundException("saveVerfuegung", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "BetreuungID invalid: " + betreuungId.getId());
		}
		throw new EbeguEntityNotFoundException("saveVerfuegung", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "GesuchId invalid: " + gesuchId.getId());
	}

	@ApiOperation(value = "Schliesst eine Betreuung ab, ohne sie zu verfuegen", response = Void.class)
	@Nullable
	@POST
	@Path("/schliessenOhneVerfuegen/{betreuungId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response verfuegungSchliessenOhneVerfuegen(
		@Nonnull @NotNull @PathParam ("betreuungId") JaxId betreuungId) throws EbeguException {

		Optional<Betreuung> betreuung = betreuungService.findBetreuung(betreuungId.getId());
		if (betreuung.isPresent()) {
			betreuungService.schliessenOhneVerfuegen(betreuung.get());
			return Response.ok().build();
		}
		throw new EbeguEntityNotFoundException("verfuegungSchliessenOhneVerfuegen", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "BetreuungID invalid: " + betreuungId.getId());
	}

	@ApiOperation(value = "Erstellt eine Nichteintretens-Verfuegung", response = JaxVerfuegung.class)
	@Nullable
	@PUT
	@Path("/nichtEintreten/{betreuungId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxVerfuegung schliessenNichtEintreten(
		@Nonnull @NotNull @PathParam ("betreuungId") JaxId betreuungId,
		@Nonnull @NotNull @Valid JaxVerfuegung verfuegungJAXP) throws EbeguException {

		Optional<Betreuung> betreuung = betreuungService.findBetreuung(betreuungId.getId());
		if (betreuung.isPresent()) {
			Verfuegung verfuegungToMerge = new Verfuegung();
			if (verfuegungJAXP.getId() != null) {
				Optional<Verfuegung> optional = verfuegungService.findVerfuegung(verfuegungJAXP.getId());
				verfuegungToMerge = optional.orElse(new Verfuegung());
			}
			Verfuegung convertedVerfuegung = converter.verfuegungToEntity(verfuegungJAXP, verfuegungToMerge);
			Verfuegung persistedVerfuegung = this.verfuegungService.nichtEintreten(convertedVerfuegung, betreuung.get().getId());
			return converter.verfuegungToJax(persistedVerfuegung);
		}
		throw new EbeguEntityNotFoundException("nichtEintreten", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "BetreuungID invalid: " + betreuungId.getId());
	}

	/**
	 * Hack, welcher das Gesuch detached, damit es auf keinen Fall gespeichert wird. Vorher muessen die Lazy geloadeten
	 * BetreuungspensumContainers geladen werden, da danach keine Session mehr zur Verfuegung steht!
     */
	private void loadRelationsAndDetach(Gesuch gesuch) {
		for (Betreuung betreuung : gesuch.extractAllBetreuungen()) {
			betreuung.getBetreuungspensumContainers().size();
			betreuung.getAbwesenheitContainers().size();
		}
		if (gesuch.getGesuchsteller1() != null) {
			gesuch.getGesuchsteller1().getAdressen().size();
			gesuch.getGesuchsteller1().getErwerbspensenContainers().size();
		}
		if (gesuch.getGesuchsteller2() != null) {
			gesuch.getGesuchsteller2().getAdressen().size();
			gesuch.getGesuchsteller2().getErwerbspensenContainers().size();
		}
		persistence.getEntityManager().detach(gesuch);
	}
}

