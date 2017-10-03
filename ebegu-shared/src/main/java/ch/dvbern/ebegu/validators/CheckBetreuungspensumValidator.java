package ch.dvbern.ebegu.validators;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Betreuungspensum;
import ch.dvbern.ebegu.entities.BetreuungspensumContainer;
import ch.dvbern.ebegu.services.EbeguParameterService;
import ch.dvbern.ebegu.util.BetreuungUtil;

/**
 * Validator for Betreuungspensen, checks that the entered betreuungspensum is bigger than the minimum
 * that is allowed for the Betreungstyp for a given date
 */
public class CheckBetreuungspensumValidator implements ConstraintValidator<CheckBetreuungspensum, Betreuung> {

	@SuppressWarnings("CdiInjectionPointsInspection")
	@Inject
	private EbeguParameterService ebeguParameterService;

	// We need to pass to EbeguParameterService a new EntityManager to avoid errors like ConcurrentModificatinoException. So we create it here
	// and pass it to the methods of EbeguParameterService we need to call.
	//http://stackoverflow.com/questions/18267269/correct-way-to-do-an-entitymanager-query-during-hibernate-validation
	@PersistenceUnit(unitName = "ebeguPersistenceUnit")
	private EntityManagerFactory entityManagerFactory;


	public CheckBetreuungspensumValidator() {
	}

	/**
	 * Constructor fuer tests damit service reingegeben werden kann
	 * @param service service zum testen
	 */
	public CheckBetreuungspensumValidator(EbeguParameterService service, EntityManagerFactory entityManagerFactory){
		this.ebeguParameterService = service;
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void initialize(CheckBetreuungspensum constraintAnnotation) {
		// nop
	}

	@Override
	public boolean isValid(Betreuung betreuung, ConstraintValidatorContext context) {

		final EntityManager em = createEntityManager();
		int index = 0;
		for (BetreuungspensumContainer betPenContainer: betreuung.getBetreuungspensumContainers()) {
			LocalDate betreuungAb = betPenContainer.getBetreuungspensumJA().getGueltigkeit().getGueltigAb();
			LocalDate gesuchsperiodeStart = betPenContainer.extractGesuchsperiode().getGueltigkeit().getGueltigAb();
			//Wir laden  die Parameter von Start-Gesuchsperiode falls Betreuung schon laenger als Gesuchsperiode besteht
			LocalDate stichtagParameter = betreuungAb.isAfter(gesuchsperiodeStart) ? betreuungAb : gesuchsperiodeStart;
			int betreuungsangebotTypMinValue = BetreuungUtil.getMinValueFromBetreuungsangebotTyp(
				stichtagParameter, betreuung.getBetreuungsangebotTyp(), ebeguParameterService, em);

			if (!validateBetreuungspensum(betPenContainer.getBetreuungspensumGS(), betreuungsangebotTypMinValue, index, "GS", context)
				|| !validateBetreuungspensum(betPenContainer.getBetreuungspensumJA(), betreuungsangebotTypMinValue, index, "JA", context)) {

				closeEntityManager(em);
				return false;
			}
			index++;
		}
		closeEntityManager(em);
		return true;
	}

	private EntityManager createEntityManager() {
		if (entityManagerFactory != null) {
			return  entityManagerFactory.createEntityManager(); // creates a new EntityManager
		}
		return null;
	}

	private void closeEntityManager(EntityManager em) {
		if (em != null) {
			em.close();
		}
	}

	/**
	 * With the given the pensumMin it checks if the introduced pensum is in the permitted range. Case not a ConstraintValidator will be created
	 * with a message and a path indicating which object threw the error. False will be returned in the explained case. In case the value for pensum
	 * is right, nothing will be done and true will be returned.
	 * @param betreuungspensum the betreuungspensum to check
	 * @param pensumMin the minimum permitted value for pensum
	 * @param index the index of the Betreuungspensum inside the betreuungspensum container
	 * @param containerPostfix JA or GS
	 * @param context the context
	 * @return true if the value resides inside the permitted range. False otherwise
	 */
	private boolean validateBetreuungspensum(Betreuungspensum betreuungspensum, int pensumMin, int index, String containerPostfix, ConstraintValidatorContext context) {
		// Es waere moeglich, die Messages mit der Klasse HibernateConstraintValidatorContext zu erzeugen. Das waere aber Hibernate-abhaengig. wuerde es Sinn machen??
		if(betreuungspensum != null && !betreuungspensum.getNichtEingetreten() && betreuungspensum.getPensum() < pensumMin) {
			ResourceBundle rb = ResourceBundle.getBundle("ValidationMessages");
			String message = rb.getString("invalid_betreuungspensum");
			message = MessageFormat.format(message, betreuungspensum.getPensum(), pensumMin);

			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode("betreuungspensumContainers[" + Integer.toString(index) + "].betreuungspensum" + containerPostfix + ".pensum")
				.addConstraintViolation();

			return false;
		}
		return true;
	}
}
