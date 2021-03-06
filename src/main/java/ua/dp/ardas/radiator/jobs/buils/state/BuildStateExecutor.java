package ua.dp.ardas.radiator.jobs.buils.state;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static ua.dp.ardas.radiator.jobs.buils.state.BuildState.States.BUILD_FAILED;
import static ua.dp.ardas.radiator.jobs.buils.state.BuildState.States.CONFIGURATION_FAILED;
import static ua.dp.ardas.radiator.jobs.buils.state.BuildState.States.SUCCESS;
import static ua.dp.ardas.radiator.utils.BuildStateUtils.calculateFailedEmail;
import static ua.dp.ardas.radiator.utils.BuildStateUtils.calculateFailedName;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ua.dp.ardas.radiator.config.AppConfig;
import ua.dp.ardas.radiator.dto.hudson.api.BuildDetails;
import ua.dp.ardas.radiator.resr.client.BuildStatusRestClient;

@Component
public class BuildStateExecutor {
	private static Logger LOG = Logger.getLogger(BuildStateExecutor.class.getName());
	
	@Autowired
	private AppConfig config;
	@Autowired
	private BuildStatusRestClient restClient;
	@Value("${job.errorMessage:Build has been broken}")
	private String defaultErrorMessage;
	@Value("${faild.email.format:%s@ardas.dp.ua}")
	private String emailFormat;

	
	public BuildState loadState(BuildStateInstances instances) {
		String url = getUrl(instances);
		if (LOG.isDebugEnabled()) {
			LOG.debug(format("url ", url));
		}

		BuildState buildState = null;
		try {
			buildState = calculateState(instances, url);
		} catch (Exception exception) {
			LOG.error("Can not evaluate buildState");
			if (LOG.isDebugEnabled()) {
				LOG.debug("Error", exception);
			}
		}
		
		if (LOG.isInfoEnabled()) {
			LOG.info(format("BuildState for url %s : %s", url, buildState));
		}
		
		return buildState;
	}

	private BuildState calculateState(BuildStateInstances instances, String url) {
		int lastBuild = restClient.loadLastBuildNumber(url);
		int lastSuccessfulBuild = restClient.loadLastSuccessfulBuildNumber(url);
		int lastFailedBuild = restClient.loadLastFailedBuildNumber(url);
		
		if(lastSuccessfulBuild > lastFailedBuild) {
			return newSuccessState(instances);
		}
		
		if(lastFailedBuild == lastBuild && !instances.isConfigurationIssue()) {
			return newBuildFailedState(instances, url, lastFailedBuild);
		}
		
		return newConfigurationFailedState(instances);
	}

	private BuildState newConfigurationFailedState(BuildStateInstances instances) {
		return new BuildState(CONFIGURATION_FAILED, instances);
	}

	private BuildState newBuildFailedState(BuildStateInstances instances, String url, Integer lastFailedBuild) {
		BuildDetails buildDetails = restClient.loadBuildDetails(url, lastFailedBuild);

		BuildState buildState = new BuildState(BUILD_FAILED, instances);
		buildState.errorMessage = getErrorMessage(instances);
		buildState.failedEmail = calculateFailedEmail(buildDetails.culprits, emailFormat);
		buildState.failedName = calculateFailedName(buildDetails.culprits);
		return buildState;
	}
	

	private BuildState newSuccessState(BuildStateInstances instances) {
		return new BuildState(SUCCESS, instances);
	}

	private String getUrl(BuildStateInstances instances) {
		return config.stringProperty(format("job.%s.url", instances));
	}
	
	private String getErrorMessage(BuildStateInstances instances) {
		String customErrorMessage = config.stringProperty(format("job.%s.errorMessage", instances));
		
		return defaultIfEmpty(customErrorMessage, defaultErrorMessage);
	}
}
