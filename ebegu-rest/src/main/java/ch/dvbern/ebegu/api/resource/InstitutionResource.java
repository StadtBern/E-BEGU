package ch.dvbern.ebegu.api.resource;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.Validate;

import ch.dvbern.ebegu.api.client.JaxOpenIdmResponse;
import ch.dvbern.ebegu.api.client.JaxOpenIdmResult;
import ch.dvbern.ebegu.api.client.OpenIdmRestService;
import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.dtos.JaxInstitution;
import ch.dvbern.ebegu.api.util.OpenIDMUtil;
import ch.dvbern.ebegu.entities.Institution;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.InstitutionService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * REST Resource fuer Institution
 */
@Path("institutionen")
@Stateless
@Api(description = "Resource für Institutionen (Anbieter eines Betreuungsangebotes)")
public class InstitutionResource {

	@Inject
	private InstitutionService institutionService;

	@Inject
	private JaxBConverter converter;

	@Inject
	private OpenIdmRestService openIdmRestService;

	@ApiOperation(value = "Creates a new Institution in the database.", response = JaxInstitution.class)
	@Nullable
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createInstitution(
		@Nonnull @NotNull JaxInstitution institutionJAXP,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Institution convertedInstitution = converter.institutionToEntity(institutionJAXP, new Institution());
		Institution persistedInstitution = this.institutionService.createInstitution(convertedInstitution);

		URI uri = uriInfo.getBaseUriBuilder()
			.path(InstitutionResource.class)
			.path("/" + persistedInstitution.getId())
			.build();

		JaxInstitution jaxInstitution = converter.institutionToJAX(persistedInstitution);

		final Optional<JaxOpenIdmResult> openIdmRestClientInstitution = openIdmRestService.createInstitution(persistedInstitution);
		if (openIdmRestClientInstitution.isPresent()) {
			jaxInstitution.setSynchronizedWithOpenIdm(true);
		}

		return Response.created(uri).entity(jaxInstitution).build();
	}

	@ApiOperation(value = "Update a Institution in the database.", response = JaxInstitution.class)
	@Nullable
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxInstitution updateInstitution(
		@Nonnull @NotNull @Valid JaxInstitution institutionJAXP,
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		Validate.notNull(institutionJAXP.getId());
		Optional<Institution> optInstitution = institutionService.findInstitution(institutionJAXP.getId());
		Institution institutionFromDB = optInstitution.orElseThrow(() -> new EbeguEntityNotFoundException("update", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND, institutionJAXP.getId()));

		Institution institutionToMerge = converter.institutionToEntity(institutionJAXP, institutionFromDB);
		Institution modifiedInstitution = this.institutionService.updateInstitution(institutionToMerge);

		final Optional<JaxOpenIdmResult> openIdmRestClientInstitution = openIdmRestService.updateInstitution(modifiedInstitution);

		final JaxInstitution jaxInstitution = converter.institutionToJAX(modifiedInstitution);
		if (openIdmRestClientInstitution.isPresent()) {
			jaxInstitution.setSynchronizedWithOpenIdm(true);
		}

		return jaxInstitution;
	}

	@ApiOperation(value = "Find and return an Institution by his institution id as parameter",
		response = JaxInstitution.class)
	@Nullable
	@GET
	@Path("/id/{institutionId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public JaxInstitution findInstitution(
		@Nonnull @NotNull @PathParam("institutionId") JaxId institutionJAXPId){

		Validate.notNull(institutionJAXPId.getId());
		String institutionID = converter.toEntityId(institutionJAXPId);
		Optional<Institution> optional = institutionService.findInstitution(institutionID);

		if (!optional.isPresent()) {
			return null;
		}
		return converter.institutionToJAX(optional.get());
	}

	@ApiOperation(value = "Remove an Institution logically by his institution-id as parameter", response = Void.class)
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	@Nullable
	@DELETE
	@Path("/{institutionId}")
	@Consumes(MediaType.WILDCARD)
	public Response removeInstitution(
		@Nonnull @NotNull @PathParam("institutionId") JaxId institutionJAXPId,
		@Context HttpServletResponse response) {

		Validate.notNull(institutionJAXPId.getId());
		institutionService.setInstitutionInactive(converter.toEntityId(institutionJAXPId));

		openIdmRestService.deleteInstitution(institutionJAXPId.getId());

		return Response.ok().build();
	}

	@ApiOperation(value = "Find and return a list of Institution by the Traegerschaft as parameter",
		responseContainer = "List", response = JaxInstitution.class)
	@Nonnull
	@GET
	@Path("/traegerschaft/{traegerschaftId}")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public List<JaxInstitution> getAllInstitutionenFromTraegerschaft(
		@Nonnull @NotNull @PathParam("traegerschaftId") JaxId traegerschaftJAXPId) {

		Validate.notNull(traegerschaftJAXPId.getId());
		String traegerschaftId = converter.toEntityId(traegerschaftJAXPId);
		return institutionService.getAllInstitutionenFromTraegerschaft(traegerschaftId).stream()
			.map(institution -> converter.institutionToJAX(institution))
			.collect(Collectors.toList());
	}


	@ApiOperation(value = "Find and return a list of all Institutionen",
		responseContainer = "List", response = JaxInstitution.class)
	@Nonnull
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public List<JaxInstitution> getAllInstitutionen() {
		return institutionService.getAllInstitutionen().stream()
			.map(inst -> converter.institutionToJAX(inst))
			.collect(Collectors.toList());
	}

	@ApiOperation(value = "Find and return a list of all active Institutionen. An active Institution is a Institution " +
		"where the active flag is true", responseContainer = "List", response = JaxInstitution.class)
	@Nonnull
	@GET
	@Path("/active")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public List<JaxInstitution> getAllActiveInstitutionen() {
		return institutionService.getAllActiveInstitutionen().stream()
			.map(inst -> converter.institutionToJAX(inst))
			.collect(Collectors.toList());
	}

	@ApiOperation(value = "Find and return a list of all Institutionen of the currently logged in Benutzer. Retruns " +
		"all for admins", responseContainer = "List", response = JaxInstitution.class)
	@Nonnull
	@GET
	@Path("/currentuser")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public List<JaxInstitution> getAllowedInstitutionenForCurrentBenutzer() {
		return institutionService.getAllowedInstitutionenForCurrentBenutzer().stream()
			.map(inst -> converter.institutionToJAX(inst))
			.collect(Collectors.toList());
	}

	@ApiOperation(value = "Synchronize DB Institutions with OpenIdm Institutions.")
	@Nullable
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/synchronizeWithOpenIdm")
	public Response synchronizeWithOpenIdm(
		@Context UriInfo uriInfo,
		@Context HttpServletResponse response) throws EbeguException {

		final StringBuilder stringBuilder = synchronizeInstitutions(true);

		URI uri = uriInfo.getBaseUriBuilder()
			.path(InstitutionResource.class)
			.build();

		return Response.created(uri).entity(stringBuilder).build();
	}

	public StringBuilder synchronizeInstitutions(boolean deleteOrphan) {
		final Optional<JaxOpenIdmResponse> optAllInstitutions = openIdmRestService.getAll();
		final Collection<Institution> allActiveInstitutionen = institutionService.getAllActiveInstitutionen();
		StringBuilder responseString = new StringBuilder("");

		if (optAllInstitutions.isPresent()) {
			final JaxOpenIdmResponse allInstitutions = optAllInstitutions.get();
			Objects.requireNonNull(allInstitutions);
			Objects.requireNonNull(allActiveInstitutionen);

			// Create in OpenIDM those Institutions that currently exist in EBEGU but not in OpenIDM
			allActiveInstitutionen.forEach(ebeguInstitution -> {
				if (allInstitutions.getResult().stream()
					.noneMatch(jaxOpenIdmResult -> OpenIDMUtil.convertToEBEGUID(jaxOpenIdmResult.get_id()).equals(ebeguInstitution.getId())
						&& jaxOpenIdmResult.getType().equals(OpenIdmRestService.INSTITUTION))) {
					// if none match -> create
					final Optional<JaxOpenIdmResult> institutionCreated = openIdmRestService.createInstitution(ebeguInstitution);
					openIdmRestService.generateResponseString(responseString, ebeguInstitution.getId(), ebeguInstitution.getName(), institutionCreated.isPresent(), "Created");
				}
			});

			if (deleteOrphan && allInstitutions.getResult() != null) {
				// Delete in OpenIDM those Institutions that exist in OpenIdm but not in EBEGU
				allInstitutions.getResult().forEach(openIdmInstitution -> {
					if (openIdmInstitution.getType().equals(OpenIdmRestService.INSTITUTION) && allActiveInstitutionen.stream().noneMatch(
						ebeguInstitution -> ebeguInstitution.getId().equals(OpenIDMUtil.convertToEBEGUID(openIdmInstitution.get_id())))) {
						// if none match -> delete
						final boolean sucess = openIdmRestService.deleteInstitution(OpenIDMUtil.convertToEBEGUID(openIdmInstitution.get_id()));
						openIdmRestService.generateResponseString(responseString, openIdmInstitution.get_id(), openIdmInstitution.getName(), sucess, "Deleted");
					}
				});
			}
		} else {
			responseString.append("Error: Can't communicate with OpenIdm server");
		}
		if (responseString.length() == 0) {
			responseString.append("No differences between OpenIdm and Ebegu found. Nothing to do!");
		}
		return responseString;
	}
}
