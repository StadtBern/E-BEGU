/*
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.dvbern.ebegu.tests;

import ch.dvbern.ebegu.entities.ApplicationProperty;
import ch.dvbern.ebegu.enums.ApplicationPropertyKey;
import ch.dvbern.ebegu.services.ApplicationPropertyService;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;


/**
 * Dies ist ein Beispiel einer Arquillian Test Klasse. Es wird vor jedem Test die Datenbank mit dem leeren Dataset
 * initialisiert.
 */
@RunWith(Arquillian.class)
@UsingDataSet("datasets/empty.xml")
@Transactional(TransactionMode.DISABLED)
public class ApplicationPropertyServiceTest extends AbstractEbeguLoginTest {

	@Inject
	private ApplicationPropertyService applicationPropertyService;

	@Inject
	private Persistence persistence;




	@Test
	public void saveOrUpdateApplicationPropertyTest() {
		Assert.assertNotNull(applicationPropertyService);
		applicationPropertyService.saveOrUpdateApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED, "testValue");
		Assert.assertEquals(1, applicationPropertyService.getAllApplicationProperties().size());
		Assert.assertEquals("testValue", applicationPropertyService.readApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED).get().getValue());

	}

	@Test
	public void removeApplicationPropertyTest() {
		insertNewEntity();
		applicationPropertyService.removeApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED);
		Assert.assertEquals(0, applicationPropertyService.getAllApplicationProperties().size());

	}
	@Test
	public void updateApplicationPropertyTest() {
		insertNewEntity();
		applicationPropertyService.saveOrUpdateApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED,"changed");
		Assert.assertEquals("changed", applicationPropertyService.readApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED).get().getValue());

	}

	// Help Methods

	private void insertNewEntity() {
		persistence.persist(new ApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED, "testValue"));
		Assert.assertEquals(1, applicationPropertyService.getAllApplicationProperties().size());
		Assert.assertNotNull(applicationPropertyService.readApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED));
		Assert.assertEquals("testValue", applicationPropertyService.readApplicationProperty(ApplicationPropertyKey.EVALUATOR_DEBUG_ENABLED).get().getValue());
	}

}
