package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxEbeguVorlage;
import ch.dvbern.ebegu.api.dtos.JaxId;
import ch.dvbern.ebegu.api.dtos.JaxVorlage;
import ch.dvbern.ebegu.api.util.RestUtil;
import ch.dvbern.ebegu.entities.EbeguVorlage;
import ch.dvbern.ebegu.entities.Gesuchsperiode;
import ch.dvbern.ebegu.enums.EbeguVorlageKey;
import ch.dvbern.ebegu.errors.EbeguException;
import ch.dvbern.ebegu.services.EbeguVorlageService;
import ch.dvbern.ebegu.services.FileSaverService;
import ch.dvbern.ebegu.services.GesuchsperiodeService;
import ch.dvbern.ebegu.util.UploadFileInfo;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeTypeParseException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Resource fuer Dokumente
 */
@Path("ebeguVorlage")
@Stateless
@Api
public class EbeguVorlageResource {

	private static final String PART_FILE = "file";
	private static final String FILENAME_HEADER = "x-filename";
	private static final String VORLAGE_KEY_HEADER = "x-vorlagekey";
	private static final String GESUCHSPERIODE_HEADER = "x-gesuchsperiode";


	private static final Logger LOG = LoggerFactory.getLogger(EbeguVorlageResource.class);

	@SuppressWarnings("CdiInjectionPointsInspection")
	@Inject
	private JaxBConverter converter;

	@Inject
	private EbeguVorlageService ebeguVorlageService;

	@Inject
	private FileSaverService fileSaverService;

	@Inject
	private GesuchsperiodeService gesuchsperiodeService;


	@Nullable
	@GET
	@Path("/gesuchsperiode/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<JaxEbeguVorlage> getEbeguVorlagenByGesuchsperiode(
		@Nonnull @NotNull @PathParam("id") JaxId id) {

		Validate.notNull(id.getId());
		String gesuchsperiodeId = converter.toEntityId(id);
		Optional<Gesuchsperiode> gesuchsperiode = gesuchsperiodeService.findGesuchsperiode(gesuchsperiodeId);
		if (gesuchsperiode.isPresent()) {

			List<EbeguVorlage> persistedEbeguVorlagen = ebeguVorlageService.getALLEbeguVorlageByGesuchsperiode(gesuchsperiode.get());

			if (persistedEbeguVorlagen.isEmpty()) {
				ebeguVorlageService.copyEbeguVorlageListToNewGesuchsperiode(gesuchsperiode.get());
				persistedEbeguVorlagen = ebeguVorlageService.getALLEbeguVorlageByGesuchsperiode(gesuchsperiode.get());
			}

			Collections.sort(persistedEbeguVorlagen);

			return persistedEbeguVorlagen.stream()
				.map(ebeguVorlage -> converter.ebeguVorlageToJax(ebeguVorlage))
				.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response save(@Context HttpServletRequest request, @Context UriInfo uriInfo, MultipartFormDataInput input)
		throws IOException, ServletException, MimeTypeParseException, SQLException, EbeguException {

		request.setAttribute(InputPart.DEFAULT_CONTENT_TYPE_PROPERTY, "*/*; charset=UTF-8");

		String filename = request.getHeader(FILENAME_HEADER);

		// check if filename available
		if (filename == null || filename.isEmpty()) {
			final String problemString = "filename must be given";
			LOG.error(problemString);
			return Response.serverError().entity(problemString).build();
		}

		EbeguVorlageKey ebeguVorlageKey;
		try {
			ebeguVorlageKey = EbeguVorlageKey.valueOf(request.getHeader(VORLAGE_KEY_HEADER));
		} catch (IllegalArgumentException e) {
			final String problemString = "ebeguVorlageKey must be given";
			LOG.error(problemString);
			return Response.serverError().entity(problemString).build();
		}

		String gesuchsperiodeId = request.getHeader(GESUCHSPERIODE_HEADER);

		// check if filename available
		if (gesuchsperiodeId == null || gesuchsperiodeId.isEmpty()) {
			final String problemString = "gesuchsperiodeId must be given";
			LOG.error(problemString);
			return Response.serverError().entity(problemString).build();
		}
		Optional<Gesuchsperiode> gesuchsperiode = gesuchsperiodeService.findGesuchsperiode(gesuchsperiodeId);
		if (!gesuchsperiode.isPresent()) {
			final String problemString = "gesuchsperiode not found on server";
			LOG.error(problemString);
			return Response.serverError().entity(problemString).build();
		}


		List<InputPart> inputParts = input.getFormDataMap().get(PART_FILE);
		if (inputParts == null || !inputParts.stream().findAny().isPresent()) {
			return Response.serverError().entity("form-parameter 'file' not found").build();
		}

		UploadFileInfo fileInfo = RestUtil.parseUploadFile(inputParts.stream().findAny().get());

		// evil workaround, (Umlaute werden sonst nicht richtig übertragen!)
		fileInfo.setFilename(filename);

		try (InputStream file = input.getFormDataPart(PART_FILE, InputStream.class, null)) {
			fileInfo.setBytes(IOUtils.toByteArray(file));
		}

		// safe File to Filesystem
		fileSaverService.save(fileInfo, "vorlagen");

		JaxEbeguVorlage jaxEbeguVorlage = new JaxEbeguVorlage();
		jaxEbeguVorlage.setName(ebeguVorlageKey);
		jaxEbeguVorlage.setGueltigAb(gesuchsperiode.get().getGueltigkeit().getGueltigAb());
		jaxEbeguVorlage.setGueltigBis(gesuchsperiode.get().getGueltigkeit().getGueltigBis());
		jaxEbeguVorlage.setVorlage(new JaxVorlage());
		jaxEbeguVorlage.getVorlage().setFilename(fileInfo.getFilename());
		jaxEbeguVorlage.getVorlage().setFilepfad(fileInfo.getPath());
		jaxEbeguVorlage.getVorlage().setFilesize(fileInfo.getSizeString());


		final Optional<EbeguVorlage> ebeguVorlageOptional = ebeguVorlageService.getEbeguVorlageByDatesAndKey(jaxEbeguVorlage.getGueltigAb(),
			jaxEbeguVorlage.getGueltigBis(), jaxEbeguVorlage.getName());
		EbeguVorlage ebeguVorlageToMerge = ebeguVorlageOptional.orElse(new EbeguVorlage());


		EbeguVorlage ebeguVorlageConverted = converter.ebeguVorlageToEntity(jaxEbeguVorlage, ebeguVorlageToMerge);

		// save modified EbeguVorlage to DB
		EbeguVorlage persistedEbeguVorlage = ebeguVorlageService.updateEbeguVorlage(ebeguVorlageConverted);

		final JaxEbeguVorlage jaxEbeguVorlageToReturn = converter.ebeguVorlageToJax(persistedEbeguVorlage);


		URI uri = uriInfo.getBaseUriBuilder()
			.path(EbeguVorlageResource.class)
			.path("/" + persistedEbeguVorlage.getId())
			.build();

		return Response.created(uri).entity(jaxEbeguVorlageToReturn).build();
	}

	@Nullable
	@DELETE
	@Path("/{ebeguVorlageId}")
	@Consumes(MediaType.WILDCARD)
	public Response removeInstitutionStammdaten(
		@Nonnull @NotNull @PathParam("ebeguVorlageId") JaxId ebeguVorlageId,
		@Context HttpServletResponse response) {

		Validate.notNull(ebeguVorlageId.getId());
		ebeguVorlageService.removeVorlage(converter.toEntityId(ebeguVorlageId));
		return Response.ok().build();
	}


}
