/*
 * Copyright © 2017 DV Bern AG, Switzerland
 *
 * Das vorliegende Dokument, einschliesslich aller seiner Teile, ist urheberrechtlich
 * geschützt. Jede Verwertung ist ohne Zustimmung der DV Bern AG unzulässig. Dies gilt
 * insbesondere für Vervielfältigungen, die Einspeicherung und Verarbeitung in
 * elektronischer Form. Wird das Dokument einem Kunden im Rahmen der Projektarbeit zur
 * Ansicht übergeben, ist jede weitere Verteilung durch den Kunden an Dritte untersagt.
 */

package ch.dvbern.ebegu.enums.reporting;

public enum BatchJobStatus {
	/**
	 * Soll von einem Worker abgearbeitet werden
	 */
	REQUESTED,
	/**
	 * Laeuft
	 */
	RUNNING,
	/**
	 * Fertig/abgebrochen
	 */
	FINISHED
}
