package ch.dvbern.ebegu.services;

import ch.dvbern.ebegu.entities.Zahlungsauftrag;

/**
 * Service fuer Generierung des Zahlungsfile gemäss ISI200022
 */
public interface Pain001Service {

	String getPainFileContent(Zahlungsauftrag zahlungsauftrag);

}
