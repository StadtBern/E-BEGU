package ch.dvbern.ebegu.validators;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Betreuungsmitteilung;
import ch.dvbern.ebegu.entities.BetreuungsmitteilungPensum;
import ch.dvbern.ebegu.enums.ErrorCodeEnum;
import ch.dvbern.ebegu.errors.EbeguEntityNotFoundException;
import ch.dvbern.ebegu.errors.EbeguRuntimeException;
import ch.dvbern.ebegu.services.EbeguParameterService;
import ch.dvbern.ebegu.util.BetreuungUtil;

/**
 * Validator for Betreuungspensen, checks that the entered betreuungspensum is bigger than the minimum
 * that is allowed for the Betreungstyp for a given date
 */
public class CheckBetreuungsmitteilungValidator implements ConstraintValidator<CheckBetreuungsmitteilung, Betreuungsmitteilung> {

	private EbeguParameterService ebeguParameterService;

	// We need to pass to EbeguParameterService a new EntityManager to avoid errors like ConcurrentModificatinoException. So we create it here
	// and pass it to the methods of EbeguParameterService we need to call.
	//http://stackoverflow.com/questions/18267269/correct-way-to-do-an-entitymanager-query-during-hibernate-validation
	@PersistenceUnit(unitName = "ebeguPersistenceUnit")
	private EntityManagerFactory entityManagerFactory;

	public CheckBetreuungsmitteilungValidator() {
	}

	@Override
	public void initialize(CheckBetreuungsmitteilung constraintAnnotation) {
		// nop
	}

	private EbeguParameterService getEbeguParameterService() {
		if (ebeguParameterService == null) {
			//FIXME: das ist nur ein Ugly Workaround, weil CDI-Injection in Wildfly 10 nicht funktioniert.
			//noinspection NonThreadSafeLazyInitialization
			ebeguParameterService = CDI.current().select(EbeguParameterService.class).get();
		}
		return ebeguParameterService;
	}

	private EntityManager createEntityManager() {
		if (entityManagerFactory != null) {
			return entityManagerFactory.createEntityManager(); // creates a new EntityManager
		} else {
			throw new EbeguRuntimeException("createEntitymanager", "could not create entitymanger for betreuung validation ", "Validierung konnte nicht durchgefuehrt werden");
		}
	}

	private void closeEntityManager(EntityManager em) {
		if (em != null) {
			em.close();
		}
	}

	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean isValid(Betreuungsmitteilung mitteilung, ConstraintValidatorContext context) {

		final EntityManager em = createEntityManager();
		getEbeguParameterService();
		final Betreuung betreuung = em.find(Betreuung.class, mitteilung.getBetreuung().getId());
		if (betreuung == null) {
			throw new EbeguEntityNotFoundException("CheckBetreuungsmitteilungValidator.isValid", ErrorCodeEnum.ERROR_ENTITY_NOT_FOUND,
				"Die Betreuung mit ID " + mitteilung.getBetreuung().getId() + " konnte nicht gefunden werden");
		}
		LocalDate gesuchsperiodeStart = betreuung.getKind().getGesuch().getGesuchsperiode().getGueltigkeit().getGueltigAb();
		int index = 0;
		for (BetreuungsmitteilungPensum betPen : mitteilung.getBetreuungspensen()) {
			LocalDate betreuungAb = betPen.getGueltigkeit().getGueltigAb();
			//Wir laden  die Parameter von Start-Gesuchsperiode falls Betreuung schon laenger als Gesuchsperiode besteht
			LocalDate stichtagParameter = betreuungAb.isAfter(gesuchsperiodeStart) ? betreuungAb : gesuchsperiodeStart;
			int betreuungsangebotTypMinValue = BetreuungUtil.getMinValueFromBetreuungsangebotTyp(
				stichtagParameter, mitteilung.getBetreuung().getBetreuungsangebotTyp(), ebeguParameterService, em);

			if (!validateBetreuungspensum(betPen, betreuungsangebotTypMinValue, index, context)) {
				closeEntityManager(em);
				return false;
			}
			index++;
		}
		closeEntityManager(em);
		return true;
	}

	/**
	 * With the given the pensumMin it checks if the introduced pensum is in the permitted range. Case not a ConstraintValidator will be created
	 * with a message and a path indicating which object threw the error. False will be returned in the explained case. In case the value for pensum
	 * is right, nothing will be done and true will be returned.
	 *
	 * @param betreuungspensum the betreuungspensum to check
	 * @param pensumMin the minimum permitted value for pensum
	 * @param index the index of the Betreuungspensum inside the betreuungspensum container
	 * @param context the context
	 * @return true if the value resides inside the permitted range. False otherwise
	 */
	private boolean validateBetreuungspensum(BetreuungsmitteilungPensum betreuungspensum, int pensumMin, int index, ConstraintValidatorContext context) {
		if (betreuungspensum != null && betreuungspensum.getPensum() < pensumMin) {
			ResourceBundle rb = ResourceBundle.getBundle("ValidationMessages");
			String message = rb.getString("invalid_betreuungspensum");
			message = MessageFormat.format(message, betreuungspensum.getPensum(), pensumMin);

			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode("betreuungsmitteilung[" + Integer.toString(index) + "].pensum")
				.addConstraintViolation();

			return false;
		}
		return true;
	}
}
