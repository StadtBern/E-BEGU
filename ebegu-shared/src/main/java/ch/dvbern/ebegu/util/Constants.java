/*
 * Copyright (c) 2013 DV Bern AG, Switzerland
 *
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschuetzt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulaessig. Dies gilt
 * insbesondere fuer Vervielfaeltigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht uebergeben ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 */

package ch.dvbern.ebegu.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ch.dvbern.ebegu.types.DateRange;

/**
 * Interface fuer Konstanten.
 */
public interface Constants {

	int DB_DEFAULT_MAX_LENGTH = 255;
	int DB_TEXTAREA_LENGTH = 4000;
	int DB_DEFAULT_SHORT_LENGTH = 100;

	int UUID_LENGTH = 36;
	int PLZ_LENGTH = 4;

	int LOGIN_TIMEOUT_SECONDS = 60 * 60; //aktuell 1h


	int ABWESENHEIT_DAYS_LIMIT = 30;

	Locale DEFAULT_LOCALE = new Locale("de", "CH");


	String DATA = "Data";
	String REGEX_EMAIL = "[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}";
	String REGEX_TELEFON = "(0|\\+41|0041)[ ]*[\\d]{2}[ ]*[\\d]{3}[ ]*[\\d]{2}[ ]*[\\d]{2}";
	String REGEX_TELEFON_MOBILE = "(0|\\+41|0041)[ ]*(74|75|76|77|78|79)[ ]*[\\d]{3}[ ]*[\\d]{2}[ ]*[\\d]{2}";
	String PATTERN_DATE = "dd.MM.yyyy";
	String PATTERN_FILENAME_DATE_TIME = "dd.MM.yyyy_HH.mm.ss";
	DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(Constants.PATTERN_DATE);
	DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(Constants.PATTERN_FILENAME_DATE_TIME);
	DateTimeFormatter SQL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	DateTimeFormatter SQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	String SERVER_MESSAGE_BUNDLE_NAME = "ch.dvbern.ebegu.i18n.server-messages";
	LocalDate END_OF_TIME = LocalDate.of(9999, 12, 31);
	LocalDate START_OF_TIME = LocalDate.of(1000, 1, 1);

	LocalDateTime START_OF_DATETIME = LocalDateTime.of(1000, 1, 1, 0, 0, 0);

	DateRange DEFAULT_GUELTIGKEIT = new DateRange(Constants.START_OF_TIME, Constants.END_OF_TIME);

	long MAX_TEMP_DOWNLOAD_AGE_MINUTES = 3L;
	int FALLNUMMER_LENGTH = 6;
	long MAX_LUCENE_QUERY_RUNTIME = 500L;

	int MAX_LUCENE_QUICKSEARCH_RESULTS = 25; // hier gibt es ein Problem, wenn wir fuer keines der Resultate berechtigt sind wird unser resultset leer sein auf client

	String DEFAULT_MANDANT_ID = "e3736eb8-6eef-40ef-9e52-96ab48d8f220";
	String AUTH_TOKEN_SUFFIX_FOR_NO_TOKEN_REFRESH_REQUESTS = "NO_REFRESH";
	String PATH_DESIGNATOR_NO_TOKEN_REFRESH = "notokenrefresh";
}
