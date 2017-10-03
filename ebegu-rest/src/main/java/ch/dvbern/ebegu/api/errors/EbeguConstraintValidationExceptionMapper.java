package ch.dvbern.ebegu.api.errors;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJBAccessException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import ch.dvbern.ebegu.api.util.RestUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by imanol on 01.03.16.
 * ExceptionMapper fuer EJBTransactionRolledbackException. Dient zum Beispiel dazu ConstraintViolationException abzufangen
 */
@Provider
public class EbeguConstraintValidationExceptionMapper extends AbstractEbeguExceptionMapper<EJBTransactionRolledbackException> {

	private final Logger LOG = LoggerFactory.getLogger(EbeguConstraintValidationExceptionMapper.class.getSimpleName());
	@Override
	protected Response buildViolationReportResponse(EJBTransactionRolledbackException exception, Status status) {
		return null;
	}

	@Override
	public Response toResponse(EJBTransactionRolledbackException exception) {
		Throwable rootCause = ExceptionUtils.getRootCause(exception);
		if (rootCause instanceof ConstraintViolationException) {
			LOG.warn("Constraint Violation occured ", exception);
			ConstraintViolationException constViolationEx = (ConstraintViolationException) rootCause;
			ResteasyViolationException resteasyViolationException = new ResteasyViolationException(constViolationEx.getConstraintViolations());
			List<MediaType> acceptedTypes = new ArrayList<>(resteasyViolationException.getAccept());
			acceptedTypes.add(MediaType.APPLICATION_JSON_TYPE);
			return ViolationReportCreator.
				buildViolationReportResponse(resteasyViolationException, Status.CONFLICT, getAcceptMediaType(acceptedTypes));
		}
		if (rootCause instanceof EJBAccessException) {
			return RestUtil.sendErrorNotAuthorized();    // nackte 403 status antwort
		}
		// wir bauen hier auch eine eigene response fuer EJBTransactionRolledbackException die wir nicht erwarten
		return buildResponse(unwrapException(exception), MediaType.TEXT_PLAIN, Status.INTERNAL_SERVER_ERROR);
	}
}

