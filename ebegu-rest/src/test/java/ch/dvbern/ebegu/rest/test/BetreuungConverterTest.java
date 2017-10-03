package ch.dvbern.ebegu.rest.test;

import javax.inject.Inject;

import ch.dvbern.ebegu.api.converter.JaxBConverter;
import ch.dvbern.ebegu.api.dtos.JaxBetreuung;
import ch.dvbern.ebegu.api.dtos.JaxPensumFachstelle;
import ch.dvbern.ebegu.entities.Betreuung;
import ch.dvbern.ebegu.entities.Fachstelle;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.entities.InstitutionStammdaten;
import ch.dvbern.ebegu.entities.KindContainer;
import ch.dvbern.ebegu.entities.PensumFachstelle;
import ch.dvbern.ebegu.tets.TestDataUtil;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests der die Konvertierung von Betreuungen prueft
 */
@RunWith(Arquillian.class)
@Transactional(TransactionMode.DISABLED)
public class BetreuungConverterTest extends AbstractEbeguRestLoginTest {

	@Inject
	private Persistence persistence;

	@Inject
	private JaxBConverter converter;

	/**
	 * transformiert einen gespeichertes Betreuungen nach jax und wieder zurueck. wir erwarten das Daten gleich bleiben
	 */
	@Test
	public void convertPersistedTestEntityToJax() {
		Betreuung betreuung = insertNewEntity();
		JaxBetreuung jaxBetr = this.converter.betreuungToJAX(betreuung);
		Betreuung betrToEntity = this.converter.betreuungToEntity(jaxBetr, new Betreuung());
		Assert.assertTrue(betreuung.isSame(betrToEntity, true, true));

	}

	@Test
	public void convertFachstelleTest() {
		Betreuung betreuung = insertNewEntity();
		KindContainer kind = betreuung.getKind();
		Assert.assertEquals(Constants.END_OF_TIME, kind.getKindJA().getPensumFachstelle().getGueltigkeit().getGueltigBis());

		JaxPensumFachstelle jaxPenFachstelle = converter.pensumFachstelleToJax(kind.getKindJA().getPensumFachstelle());
		Assert.assertNull("Gueltig bis wird nicht transformiert", jaxPenFachstelle.getGueltigBis());

		PensumFachstelle reconvertedPensum = converter.pensumFachstelleToEntity(jaxPenFachstelle, new PensumFachstelle());
		Assert.assertEquals(Constants.END_OF_TIME, reconvertedPensum.getGueltigkeit().getGueltigBis());
	}

	private Betreuung insertNewEntity() {
		Betreuung betreuung = TestDataUtil.createDefaultBetreuung();
		KindContainer kind = TestDataUtil.createDefaultKindContainer();
		Fachstelle fachstelle = persistence.persist(TestDataUtil.createDefaultFachstelle());

		PensumFachstelle pensumFachstelle = TestDataUtil.createDefaultPensumFachstelle();
		pensumFachstelle.setFachstelle(fachstelle);

		PensumFachstelle pensumFachstelle2 = TestDataUtil.createDefaultPensumFachstelle();
		pensumFachstelle2.setPensum(5);
		pensumFachstelle2.setFachstelle(fachstelle);

		InstitutionStammdaten instStammdaten = TestDataUtil.createDefaultInstitutionStammdaten();
		persistence.persist(instStammdaten.getInstitution().getMandant());
		persistence.persist(instStammdaten.getInstitution().getTraegerschaft());
		persistence.persist(instStammdaten.getInstitution());
		persistence.persist(instStammdaten);

		kind.getKindGS().setPensumFachstelle(pensumFachstelle);
		kind.getKindJA().setPensumFachstelle(pensumFachstelle2);
		Gesuch gesuch = TestDataUtil.createDefaultGesuch();
		gesuch.setGesuchsperiode(persistence.persist(gesuch.getGesuchsperiode()));
		gesuch.setFall(persistence.persist(gesuch.getFall()));
		kind.setGesuch(persistence.persist(gesuch));
		betreuung.setKind(persistence.persist(kind));
		betreuung.setInstitutionStammdaten(instStammdaten);
		return persistence.persist(betreuung);
	}

}
