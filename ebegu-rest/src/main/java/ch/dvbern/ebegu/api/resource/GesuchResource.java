package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.converter.AntragStatusConverter;
import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxAntragDTO;
import ch.dvbern.ebegu.api.dtos.JaxAntragSearchresultDTO;
import ch.dvbern.ebegu.api.dtos.JaxGesuch;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.resource.wizard.WizardStepResource;
import ch.dvbern.ebegu.api.util.RestUtil;
import ch.dvbern.ebegu.dto.suchfilter.AntragTableFilterDTO;
import ch.dvbern.ebegu.dto.suchfilter.PaginationDTO;
import ch.dvbern.ebegu.entities.Benutzer;
import ch.dvbern.ebegu.entities.Fall;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.Institution;
import ch.dvbern.ebegu.enums.AntragStatusDTO;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.BenutzerService;
import ch.dvbern.ebegu.services.GesuchService;
import ch.dvbern.ebegu.services.InstitutionService;
import com.google.common.collect.ArrayListMultimap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.security.Principal;
import java.util.*;

/**
 * Resource fuer Gesuch
 */
@Path("gesuche")
@Stateless
@Api
public class GesuchResource {

	@Inject
	private GesuchService gesuchService;

	@Inject
	private InstitutionService institutionService;

	@Inject
	private BenutzerService benutzerService;

	@Inject
	private WizardStepResource wizardStepResource;

	@Inject
	private AntragStatusConverter antragStatusConverter;

	private final Logger LOG = LoggerFactory.getLogger(GesuchResource.class.getSimpleName());

	@Inject
	private Principal principal;

	@Inject
	private JaxBConverter converter;

	@ApiOperation(value = "Creates a new Gesuch in the database. The transfer object also has a relation to Familiensituation " +
		"which is stored in the database as well.")
	@Nullable
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(
		@Nonnull @NotNull JaxGesuch gesuchJAXP,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Gesuch convertedGesuch = converter.gesuchToEntity(gesuchJAXP, new Gesuch());
		Gesuch persistedGesuch = this.gesuchService.createGesuch(convertedGesuch);
		// Die WizsrdSteps werden direkt erstellt wenn das Gesuch erstellt wird. So vergewissern wir uns dass es kein Gesuch ohne WizardSteps gibt
		wizardStepResource.createWizardStepList(new JaxId(persistedGesuch.getId()));

		URI uri = uriInfo.getBaseUriBuilder()
			.path(GesuchResource.class)
			.path("/" + persistedGesuch.getId())
			.build();


		JaxGesuch jaxGesuch = converter.gesuchToJAX(persistedGesuch);
		return Response.created(uri).entity(jaxGesuch).build();
	}

	@Nullable
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxGesuch update(
		@Nonnull @NotNull JaxGesuch gesuchJAXP,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Validate.notNull(gesuchJAXP.getId());
		Optional<Gesuch> optGesuch = gesuchService.findGesuch(gesuchJAXP.getId());
		Gesuch gesuchFromDB = optGesuch.orElseThrow(() -> new EbeguEntityNotFoundException("update", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, gesuchJAXP.getId()));

		Gesuch gesuchToMerge = converter.gesuchToEntity(gesuchJAXP, gesuchFromDB);
		Gesuch modifiedGesuch = this.gesuchService.updateGesuch(gesuchToMerge);

		return converter.gesuchToJAX(modifiedGesuch);
	}

	@Nullable
	@GET
	@Path("/{gesuchId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxGesuch findGesuch(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchJAXPId) throws EbeguException {
		Validate.notNull(gesuchJAXPId.getId());
		String gesuchID = converter.toEntityId(gesuchJAXPId);
		Optional<Gesuch> gesuchOptional = gesuchService.findGesuch(gesuchID);

		if (!gesuchOptional.isPresent()) {
			return null;
		}
		Gesuch gesuchToReturn = gesuchOptional.get();
		return converter.gesuchToJAX(gesuchToReturn);
	}

	/**
	 * Methode findGesuch fuer Benutzer mit Rolle SACHBEARBEITER_INSTITUTION oder SACHBEARBEITER_TRAEGERSCHAFT. Das ganze Gesuch wird gefilter
	 * sodass nur die relevanten Daten zum Client geschickt werden.
	 *
	 * @param gesuchJAXPId ID des Gesuchs
	 * @return filtriertes Gesuch mit nur den relevanten Daten
	 * @throws EbeguException
	 */
	@Nullable
	@GET
	@Path("/institution/{gesuchId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxGesuch findGesuchForInstitution(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchJAXPId) throws EbeguException {

		final JaxGesuch completeGesuch = findGesuch(gesuchJAXPId);

		final Optional<Benutzer> optBenutzer = benutzerService.findBenutzer(this.principal.getName());
		if (optBenutzer.isPresent()) {
			Collection<Institution> instForCurrBenutzer = institutionService.getInstitutionenForCurrentBenutzer();
			return cleanGesuchForInstitutionTraegerschaft(completeGesuch, instForCurrBenutzer);
		}
		return null; // aus sicherheitsgruenden geben wir null zurueck wenn etwas nicht stimmmt
	}

	/**
	 * Nimmt das uebergebene Gesuch und entfernt alle Daten die fuer die Rollen SACHBEARBEITER_INSTITUTION oder SACHBEARBEITER_TRAEGERSCHAFT nicht
	 * relevant sind. Dieses Gesuch wird zurueckgeliefert
	 */
	private JaxGesuch cleanGesuchForInstitutionTraegerschaft(final JaxGesuch completeGesuch, final Collection<Institution> userInstitutionen) {
		//clean EKV
		completeGesuch.setEinkommensverschlechterungInfo(null);

		//clean GS -> FinSit
		if (completeGesuch.getGesuchsteller1() != null) {
			completeGesuch.getGesuchsteller1().setEinkommensverschlechterungContainer(null);
			completeGesuch.getGesuchsteller1().setErwerbspensenContainers(null);
			completeGesuch.getGesuchsteller1().setFinanzielleSituationContainer(null);
		}
		if (completeGesuch.getGesuchsteller2() != null) {
			completeGesuch.getGesuchsteller2().setEinkommensverschlechterungContainer(null);
			completeGesuch.getGesuchsteller2().setErwerbspensenContainers(null);
			completeGesuch.getGesuchsteller2().setFinanzielleSituationContainer(null);
		}

		RestUtil.purgeKinderAndBetreuungenOfInstitutionen(completeGesuch.getKindContainers(), userInstitutionen);

		return completeGesuch;
	}

	@Nullable
	@PUT
	@Path("/bemerkung/{gesuchId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateBemerkung(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchJAXPId,
		@Nonnull @NotNull String bemerkung,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Validate.notNull(gesuchJAXPId.getId());
		Optional<Gesuch> gesuchOptional = gesuchService.findGesuch(converter.toEntityId(gesuchJAXPId));

		if (gesuchOptional.isPresent()) {
			gesuchOptional.get().setBemerkungen(bemerkung);

			gesuchService.updateGesuch(gesuchOptional.get());

			return Response.ok().build();
		}
		throw new EbeguEntityNotFoundException("updateBemerkung", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "GesuchId invalid: " + gesuchJAXPId.getId());
	}

	@Nullable
	@PUT
	@Path("/status/{gesuchId}/{statusDTO}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateStatus(
		@Nonnull @NotNull @PathParam("gesuchId") JaxId gesuchJAXPId,
		@Nonnull @NotNull @PathParam("statusDTO") AntragStatusDTO statusDTO) throws EbeguException {

		Validate.notNull(gesuchJAXPId.getId());
		Validate.notNull(statusDTO);
		Optional<Gesuch> gesuchOptional = gesuchService.findGesuch(converter.toEntityId(gesuchJAXPId));

		if (gesuchOptional.isPresent()) {
			gesuchOptional.get().setStatus(antragStatusConverter.convertStatusToEntity(statusDTO));

			gesuchService.updateGesuch(gesuchOptional.get());

			return Response.ok().build();
		}
		throw new EbeguEntityNotFoundException("updateStatus", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, "GesuchId invalid: " + gesuchJAXPId.getId());
	}

	/**
	 * Methode findGesuchByFallAndPeriode fuer fallID und GesuchsperiodeID
	 *
	 * @param fallJAXPId          ID des Falles
	 * @param gesuchperiodeJAXPId ID der Gesuchsperiode
	 * @return filtriertes Gesuch mit nur den relevanten Daten
	 */
	@Nullable
	@GET
	@Path("/fallId/gesuchsperiodeId/{fallId}/{gesuchsperiodeId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxGesuch findGesuchByFallAndPeriode(
		@Nonnull @NotNull @PathParam("fallId") JaxId fallJAXPId,
		@Nonnull @NotNull @PathParam("gesuchsperiodeId") JaxId gesuchperiodeJAXPId) throws EbeguException {

		Validate.notNull(fallJAXPId.getId());
		String fallID = converter.toEntityId(fallJAXPId);

		Validate.notNull(gesuchperiodeJAXPId.getId());
		String gesuchsperiodeID = converter.toEntityId(gesuchperiodeJAXPId);

		Optional<Gesuch> gesuchOptional = gesuchService.findGesuchByFallAndGesuchsperiode(fallID, gesuchsperiodeID);

		if (!gesuchOptional.isPresent()) {
			return null;
		}
		Gesuch gesuchToReturn = gesuchOptional.get();
		return converter.gesuchToJAX(gesuchToReturn);
	}


	@Nonnull
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchAntraege(
		@Nonnull @NotNull AntragTableFilterDTO antragSearch,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) {

		Pair<Long, List<Gesuch>> searchResultPair = gesuchService.searchAntraege(antragSearch);
		List<Gesuch> foundAntraege = searchResultPair.getRight();
		//todo hier darf fuer jeden Fall nur der Antrag mit dem neusten datum drin sein, spaeter im query machen
		ArrayListMultimap<Fall, Gesuch> fallToAntragMultimap = ArrayListMultimap.create();
		for (Gesuch gesuch : foundAntraege) {
			fallToAntragMultimap.put(gesuch.getFall(), gesuch);
		}
		Set<Gesuch> gesuchSet = new LinkedHashSet<>();
		for (Gesuch gesuch : foundAntraege) {
			List<Gesuch> antraege = fallToAntragMultimap.get(gesuch.getFall());
			Collections.sort(antraege, (Comparator<Gesuch>) (o1, o2) -> o1.getEingangsdatum().compareTo(o2.getEingangsdatum()));
			gesuchSet.add(antraege.get(0)); //nur neusten zurueckgeben
		}

		List<JaxAntragDTO> antragDTOList = new ArrayList<>(gesuchSet.size());
		gesuchSet.stream().forEach(gesuch -> {
			JaxAntragDTO antragDTO = converter.gesuchToAntragDTO(gesuch);
			antragDTO.setFamilienName(gesuch.extractFamiliennamenString());
			antragDTOList.add(antragDTO);
		});
		//Es wird immer nur der neuste Antrag zurueckgegeben, das muss spaeter im query gemacht werden sonst stimmt die pagegroesse dann nicht mehr
		JaxAntragSearchresultDTO resultDTO = new JaxAntragSearchresultDTO();
		resultDTO.setAntragDTOs(antragDTOList);
		PaginationDTO pagination = antragSearch.getPagination();
		pagination.setTotalItemCount(searchResultPair.getLeft());
		resultDTO.setPaginationDTO(pagination);
		return Response.ok(resultDTO).build();
	}

}
