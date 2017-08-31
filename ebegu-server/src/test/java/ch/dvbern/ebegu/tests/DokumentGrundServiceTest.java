package ch.dvbern.ebegu.tests;

import java.util.Optional;

import javax.inject.Inject;

import ch.dvbern.ebegu.entities.DokumentGrund;
import ch.dvbern.ebegu.entities.Gesuch;
import ch.dvbern.ebegu.services.DokumentGrundService;
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
 * Tests fuer die Klasse DokumentGrundService
 */
@RunWith(Arquillian.class)
@UsingDataSet("datasets/mandant-dataset.xml")
@Transactional(TransactionMode.DISABLED)
public class DokumentGrundServiceTest extends AbstractEbeguLoginTest {

	@Inject
	private DokumentGrundService dokumentGrundService;

	@Inject
	private Persistence persistence;



	@Test
	public void createDokumentGrund() {
		Assert.assertNotNull(dokumentGrundService);

		DokumentGrund dokumentGrund = TestDataUtil.createDefaultDokumentGrund();
		Gesuch gesuch = TestDataUtil.createAndPersistGesuch(persistence);
		dokumentGrund.setGesuch(gesuch);

		dokumentGrundService.saveDokumentGrund(dokumentGrund);
		Optional<DokumentGrund> dokumentGrundOpt = dokumentGrundService.findDokumentGrund(dokumentGrund.getId());
		Assert.assertTrue(dokumentGrundOpt.isPresent());
		Assert.assertEquals(dokumentGrund.getFullName(), dokumentGrundOpt.get().getFullName());
	}

}
