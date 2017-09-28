package ch.dvbern.ebegu.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import ch.dvbern.ebegu.entities.DownloadFile;
import ch.dvbern.ebegu.entities.DownloadFile_;
import ch.dvbern.ebegu.entities.FileMetadata;
import ch.dvbern.ebegu.persistence.CriteriaQueryHelper;
import ch.dvbern.ebegu.util.Constants;
import ch.dvbern.lib.cdipersistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service fuer den Download von Dokumenten
 */
@Stateless
@Local(DownloadFileService.class)
@PermitAll
public class DownloadFileServiceBean implements DownloadFileService {


	private static final Logger LOG = LoggerFactory.getLogger(DownloadFileServiceBean.class);

	@Inject
	private Persistence persistence;

	@Inject
	private CriteriaQueryHelper criteriaQueryHelper;


	@Override
	public DownloadFile create(@Nonnull FileMetadata fileMetadata, String ip) {
		Objects.requireNonNull(fileMetadata);
		Objects.requireNonNull(ip);

		return persistence.persist(new DownloadFile(fileMetadata, ip));
	}

	@Nullable
	@Override
	public DownloadFile getDownloadFileByAccessToken(@Nonnull String accessToken) {
		Objects.requireNonNull(accessToken);

		Optional<DownloadFile> tempDokumentOptional = criteriaQueryHelper.getEntityByUniqueAttribute(DownloadFile.class, accessToken, DownloadFile_.accessToken);

		if (!tempDokumentOptional.isPresent()) {
			return null;
		}
		DownloadFile downloadFile = tempDokumentOptional.get();

		if (isFileDownloadExpired(downloadFile)) {
			return null;
		}
		return downloadFile;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void cleanUp() {
		LocalDateTime deleteOlderThan = LocalDateTime.now().minus(Constants.MAX_TEMP_DOWNLOAD_AGE_MINUTES, ChronoUnit.MINUTES);
		LOG.debug("Deleting TempDocuments before {}", deleteOlderThan);

		try {
			Objects.requireNonNull(deleteOlderThan);
			criteriaQueryHelper.deleteAllBefore(DownloadFile.class, deleteOlderThan);
		} catch (RuntimeException rte) {
			// timer methods may not throw exceptions or the timer will get cancelled (as per spec)
			String msg = "Unexpected error while deleting old TempDocuments";
			LOG.error(msg, rte);
		}
	}

	/**
	 * Access Token fuer Download ist nur fuer eine bestimmte Zeitspanne (3Min) gueltig
	 */
	private boolean isFileDownloadExpired(@Nonnull DownloadFile tempBlob) {
		LocalDateTime timestampMutiert = checkNotNull(tempBlob.getTimestampMutiert());
		return timestampMutiert.isBefore(LocalDateTime.now().minus(Constants.MAX_TEMP_DOWNLOAD_AGE_MINUTES, ChronoUnit.MINUTES));
	}
}
