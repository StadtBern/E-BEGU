/*
 * Copyright (c) 2013 DV Bern AG, Switzerland
 *
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 */

package ch.dvbern.ebegu.config;

import org.apache.commons.configuration.SystemConfiguration;

import javax.enterprise.context.Dependent;
import java.io.Serializable;

/**
 * Konfiguration von Kurstool. Liest system Properties aus
 */
@Dependent
public class EbeguConfigurationImpl extends SystemConfiguration implements EbeguConfiguration, Serializable {


	private static final long serialVersionUID = 463057263479503486L;
	private static final String EBEGU_DEVELOPMENT_MODE = "ebegu.development.mode";
	private static final String EBEGU_DOCUMENT_FILE_PATH = "ebegu.document.file.path";
	private static final String EBEGU_FEDLET_CONFIG_PATH = "ebegu.fedlet.config.path";
	private static final String EBEGU_CLIENT_USING_HTTPS = "ebegu.client.using.https";
	private static final String EBEGU_OPENIDM_URL = "ebegu.openidm.url";
	private static final String EBEGU_OPENIDM_USER = "ebegu.openidm.user";
	private static final String EBEGU_OPENIDM_PASSWD = "ebegu.openidm.passwd";
	private static final String EBEGU_OPENIDM_ENABLED = "ebegu.openidm.enabled";

	public EbeguConfigurationImpl() {

	}

	@Override
	public boolean getIsDevmode() {
		return getBoolean(EBEGU_DEVELOPMENT_MODE, true);
	}

	@Override
	public String getDocumentFilePath() {
		return getString(EBEGU_DOCUMENT_FILE_PATH, getString("jboss.server.data.dir"));
	}

	@Override
	public String getFedletConfigPath() {
		return getString(EBEGU_FEDLET_CONFIG_PATH, "fedletConfig/http_app_ebegu_ch");
	}

	@Override
	public boolean isClientUsingHTTPS() {
		return getBoolean(EBEGU_CLIENT_USING_HTTPS, false);
	}

	@Override
	public String getOpenIdmURL() {
		return getString(EBEGU_OPENIDM_URL, "https://eaccount-test.bern.ch");
	}

	@Override
	public String getOpenIdmUser() {
		return getString(EBEGU_OPENIDM_USER, "SRVC_eBEGU");
	}

	@Override
	public String getOpenIdmPassword() {
		return getString(EBEGU_OPENIDM_PASSWD, "EBEGUADMINTZZ0");
	}

	@Override
	public boolean getOpenIdmEnabled() {
		return getBoolean(EBEGU_OPENIDM_ENABLED, false);
	}

}
