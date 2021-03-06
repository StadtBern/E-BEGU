/*
 * Ki-Tax: System for the management of external childcare subsidies
 * Copyright (C) 2018 City of Bern Switzerland
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.dvbern.ebegu.batch.jobs.report;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.batch.api.AbstractBatchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import ch.dvbern.ebegu.config.EbeguConfiguration;
import ch.dvbern.ebegu.entities.DownloadFile;
import ch.dvbern.ebegu.entities.Workjob;
import ch.dvbern.ebegu.enums.TokenLifespan;
import ch.dvbern.ebegu.enums.WorkJobConstants;
import ch.dvbern.ebegu.errors.MailException;
import ch.dvbern.ebegu.services.DownloadFileService;
import ch.dvbern.ebegu.services.MailService;
import ch.dvbern.ebegu.services.WorkjobService;
import ch.dvbern.ebegu.util.UploadFileInfo;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("sendEmailBatchlet")
@Dependent
public class SendEmailBatchlet extends AbstractBatchlet {

	private static final Logger LOG = LoggerFactory.getLogger(SendEmailBatchlet.class);

	@Inject
	private WorkjobService workJobService;

	@Inject
	private DownloadFileService downloadFileService;

	@Inject
	private MailService mailService;

	@Inject
	private JobContext jobCtx;

	@Inject
	private JobDataContainer jobDataContainer;

	@Inject
	private EbeguConfiguration configuration;


	@Override
	public String process() {
		final String receiverEmail = getParameters().getProperty(WorkJobConstants.EMAIL_OF_USER);
		LOG.debug("Sending mail with file for user to {}", receiverEmail);
		Validate.notNull(receiverEmail, " Email muss gesetzt sein damit der Fertige Report an den Empfaenger geschickt werden kann");
		final Workjob workJob = readWorkjobFromDatabase();
		final UploadFileInfo fileMetadata = jobDataContainer.getResult();
		final DownloadFile downloadFile = createDownloadfile(workJob, fileMetadata);
		//	EBEGU-1663 Wildfly 10 hack, this can be removed as soon as WF11 runs, right now we create the download file right at the start and
		// can only change its content
//		workJobService.addResultToWorkjob(workJob.getId(), downloadFile.getAccessToken());
		try {

			if(configuration.isSendReportAsAttachement()){
				mailService.sendDocumentCreatedEmail(receiverEmail, downloadFile, createStatistikPageLink());
			} else{
				mailService.sendDocumentCreatedEmail(receiverEmail, null, createStatistikPageLink());
			}
			return BatchStatus.COMPLETED.toString();
		} catch (MailException e) {
			return BatchStatus.FAILED.toString();
		}
	}

	private String createStatistikPageLink() {
		return configuration.getHostname() + "/statistik";
	}
// Do not send direct downloadlink (because of security)
//	private String constructDownloadURI(DownloadFile downloadFile) {
//		//file sollte wohl besser mitgesendet werden
////		String resultData = workjob.getResultData();
//		return "http://localhost:8080/ebegu/api/v1/blobs/temp/blobdata/" + downloadFile.getAccessToken();
//	}

	@Nullable
	private DownloadFile createDownloadfile(@Nonnull Workjob workJob, @Nullable UploadFileInfo uploadFile) {
		if (uploadFile != null) {
			//copy data into pre exsiting dowonload-file
			//	EBEGU-1663 Wildfly 10 hack, this can be removed as soon as WF11 runs and download file can be generated when report is finsihed
			return downloadFileService.insertDirectly(workJob.getResultData(), uploadFile, TokenLifespan.LONG, workJob.getTriggeringIp());
		} else {
			LOG.error("UploadFileInfo muss uebergeben werden vom vorherigen Step");
			return null;
		}
	}

	@Nonnull
	private Workjob readWorkjobFromDatabase() {
		final Workjob workJob = workJobService.findWorkjobByExecutionId(jobCtx.getExecutionId());
		Validate.notNull(workJob, "Workjob mit dieser execution id muss existieren  " + jobCtx.getExecutionId());
		return workJob;
	}

	private Properties getParameters() {
		JobOperator operator = BatchRuntime.getJobOperator();
		return operator.getParameters(jobCtx.getExecutionId());
	}
}
