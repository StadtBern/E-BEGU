package ch.dvbern.ebegu.tests;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import ch.dvbern.ebegu.entities.FinanzielleSituation;
import ch.dvbern.ebegu.entities.FinanzielleSituationContainer;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.GesuchstellerContainer;
import ch.dvbern.ebegu.services.FinanzielleSituationService;
import ch.dvbern.ebegu.tets.TestDataUtil;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests fuer die Klasse FinanzielleSituationService
 */
@RunWith(Arquillian.class)
@UsingDataSet("datasets/empty.xml")
@Transactional(TransactionMode.DISABLED)
public class FinanzielleSituationServiceBeanTest extends AbstractEbeguLoginTest {

	@Inject
	private FinanzielleSituationService finanzielleSituationService;

	@Inject
	private Persistence persistence;



	@Test
	public void createFinanzielleSituation() {
		Assert.assertNotNull(finanzielleSituationService);

		FinanzielleSituation finanzielleSituation = TestDataUtil.createDefaultFinanzielleSituation();
		final Gesuch gesuch = TestDataUtil.createAndPersistGesuch(persistence);
		GesuchstellerContainer gesuchsteller = TestDataUtil.createDefaultGesuchstellerContainer(gesuch);
		gesuchsteller = persistence.persist(gesuchsteller);

		FinanzielleSituationContainer container = TestDataUtil.createFinanzielleSituationContainer();
		container.setFinanzielleSituationGS(finanzielleSituation);
		container.setGesuchsteller(gesuchsteller);

		finanzielleSituationService.saveFinanzielleSituation(container, null);
		Collection<FinanzielleSituationContainer> allFinanzielleSituationen = finanzielleSituationService.getAllFinanzielleSituationen();
		Assert.assertEquals(1, allFinanzielleSituationen.size());
		FinanzielleSituationContainer nextFinanzielleSituation = allFinanzielleSituationen.iterator().next();
		Assert.assertEquals(100000L, nextFinanzielleSituation.getFinanzielleSituationGS().getNettolohn().longValue());
	}

	@Test
	public void updateFinanzielleSituationTest() {
		Assert.assertNotNull(finanzielleSituationService);
		FinanzielleSituationContainer insertedFinanzielleSituations = insertNewEntity();
		Optional<FinanzielleSituationContainer> finanzielleSituationOptional = finanzielleSituationService.findFinanzielleSituation(insertedFinanzielleSituations.getId());
		Assert.assertTrue(finanzielleSituationOptional.isPresent());
		FinanzielleSituationContainer finanzielleSituation = finanzielleSituationOptional.get();
		finanzielleSituation.setFinanzielleSituationGS(TestDataUtil.createDefaultFinanzielleSituation());
		FinanzielleSituationContainer updatedCont = finanzielleSituationService.saveFinanzielleSituation(finanzielleSituation, null);
		Assert.assertEquals(100000L, updatedCont.getFinanzielleSituationGS().getNettolohn().longValue());

		updatedCont.getFinanzielleSituationGS().setNettolohn(new BigDecimal(200000));
		FinanzielleSituationContainer contUpdTwice = finanzielleSituationService.saveFinanzielleSituation(updatedCont, null);
		Assert.assertEquals(200000L, contUpdTwice.getFinanzielleSituationGS().getNettolohn().longValue());
	}

	private FinanzielleSituationContainer insertNewEntity() {
		final Gesuch gesuch = TestDataUtil.createAndPersistGesuch(persistence);
		GesuchstellerContainer gesuchsteller = TestDataUtil.createDefaultGesuchstellerContainer(gesuch);
		FinanzielleSituationContainer container = TestDataUtil.createFinanzielleSituationContainer();
		gesuchsteller.setFinanzielleSituationContainer(container);
		gesuchsteller = persistence.persist(gesuchsteller);
		return gesuchsteller.getFinanzielleSituationContainer();
	}
}
