package ch.dvbern.ebegu.api.resource;

import ch.dvbern.ebegu.api.dtos.JaxAdresse;
import ch.dvbern.ebegu.services.SearchIndexService;
import ch.dvbern.ebegu.util.Constants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

@Path("admin/reindex")
@Stateless
@Api(description = "Resource zum Reindizieren des Lucene-Suchindexes")
public class ReindexResource {


	@Inject
	private SearchIndexService searchIndexService;

	@ApiOperation(value = "Erstellt den Suchindex neu")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response reindex(@Context HttpServletRequest request) {
		searchIndexService.rebuildSearchIndex();

		String time = LocalDateTime.now().format(Constants.DATE_FORMATTER);
		return Response.ok(time + " Reindex started...").build();
	}
}
