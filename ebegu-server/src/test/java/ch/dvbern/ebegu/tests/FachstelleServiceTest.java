package ch.dvbern.ebegu.tests;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import ch.dvbern.ebegu.entities.Fachstelle;
import ch.dvbern.ebegu.services.FachstelleService;
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
 * Arquillian Tests fuer die Klasse FachstelleService
 */
@RunWith(Arquillian.class)
@UsingDataSet("datasets/empty.xml")
@Transactional(TransactionMode.DISABLED)
public class FachstelleServiceTest extends AbstractEbeguLoginTest {

	@Inject
	private FachstelleService fachstelleService;

	@Inject
	private Persistence persistence;



	@Test
	public void createFachstelle() {
		Assert.assertNotNull(fachstelleService);
		Fachstelle fachstelle = TestDataUtil.createDefaultFachstelle();
		fachstelleService.saveFachstelle(fachstelle);

		Collection<Fachstelle> allFachstellen = fachstelleService.getAllFachstellen();
		Assert.assertEquals(1, allFachstellen.size());
		Fachstelle nextFamsit = allFachstellen.iterator().next();
		Assert.assertEquals("Fachstelle1", nextFamsit.getName());
		Assert.assertEquals("Kinder Fachstelle", nextFamsit.getBeschreibung());
		Assert.assertTrue(nextFamsit.isBehinderungsbestaetigung());
	}

	@Test
	public void updateFamiliensituationTest() {
		Assert.assertNotNull(fachstelleService);
		Fachstelle insertedFachstelle = insertNewEntity();
		Optional<Fachstelle> fachstelle = fachstelleService.findFachstelle(insertedFachstelle.getId());
		Assert.assertEquals("Fachstelle1", fachstelle.get().getName());

		fachstelle.get().setName("Fachstelle2");
		Fachstelle updatedFachstelle = fachstelleService.saveFachstelle(fachstelle.get());
		Assert.assertEquals("Fachstelle2", updatedFachstelle.getName());
		Assert.assertEquals("Fachstelle2", fachstelleService.findFachstelle(updatedFachstelle.getId()).get().getName());
	}

	@Test
	public void removeFachstelleTest() {
		Assert.assertNotNull(fachstelleService);
		Fachstelle insertedFachstelle = insertNewEntity();
		Assert.assertEquals(1, fachstelleService.getAllFachstellen().size());

		fachstelleService.removeFachstelle(insertedFachstelle.getId());
		Assert.assertEquals(0, fachstelleService.getAllFachstellen().size());
	}

	// HELP METHODS

	@Nonnull
	private Fachstelle insertNewEntity() {
		Fachstelle fachstelle = TestDataUtil.createDefaultFachstelle();
		persistence.persist(fachstelle);
		return fachstelle;
	}

}
