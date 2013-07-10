package ua.dp.ardas.radiator.jobs.report.builder;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.String.format;
import static ua.dp.ardas.radiator.utils.DataTimeUtils.calculateMondayDate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ua.dp.ardas.radiator.dao.BuildStateDAO;
import ua.dp.ardas.radiator.dao.SpiraTestStatisticDAO;
import ua.dp.ardas.radiator.dao.ThucydidesTestStatisticDAO;
import ua.dp.ardas.radiator.jobs.buils.state.BuildState;
import ua.dp.ardas.radiator.utils.JsonUtils;

@Component
public class ReportBuilder {
	private static final String REPOST_IN_JSON = "repostInJson";

	private static Logger LOG = Logger.getLogger(ReportBuilder.class.getName());
	
	@Autowired
	private BuildStateDAO buildStateDAO;
	@Autowired
	private ThucydidesTestStatisticDAO thucydidesTestStaisticDAO;
	@Autowired
	private SpiraTestStatisticDAO spiraTestStatisticDAO;
	@Value("${report.template.path}")
	private String pathToTemplate;
	@Value("${report.path}")
	private String pathToReport;

	
	public void build() {
		HashMap<String, String> reportParameters = reportToParameterMap(agregateReportObject());

		FileReader templateReader = newTemplateReader();
		FileWriter reportWriter = newReportWriter();
		
		Velocity.evaluate(new VelocityContext(reportParameters),reportWriter, "ReportFileGenerator", templateReader);
		
		closeQuietly(templateReader);
		closeQuietly(reportWriter);
	}

	private HashMap<String, String> reportToParameterMap(Report report) {
		HashMap<String, String> reportParameters = newHashMap();
		reportParameters.put(REPOST_IN_JSON, JsonUtils.toJSON(report));
		return reportParameters;
	}

	private Report agregateReportObject() {
		Report report = new Report();

		agrefateBuildStates(report);
		agrefateThucydidesTestStais(report);
		agrefateSpiraTestStatistic(report);
		agrefateSpiraTestOnStartWeekStatistic(report);

		if (LOG.isInfoEnabled()) {
			LOG.info(format("Agregated params %s", report));
		}

		return report;
	}

	private void agrefateBuildStates(Report report) {
		List<BuildState> buildStates = buildStateDAO.findLastData();

		for (BuildState state : buildStates) {
			report.buildStates.put(state.instances, state);
		}
	}

	private void agrefateThucydidesTestStais(Report report) {
		report.thucydidesTestStaistic = thucydidesTestStaisticDAO.findLastData();
		
	}

	private void agrefateSpiraTestStatistic(Report report) {
		report.spiraTestStatistics = spiraTestStatisticDAO.findLastData();
	}

	private void agrefateSpiraTestOnStartWeekStatistic(Report report) {
		report.spiraTestOnStartWeekStatistics = spiraTestStatisticDAO.findByDate(calculateMondayDate());
	}
	

	private FileWriter newReportWriter() {
		try {
			return new FileWriter(new File(pathToReport));
		} catch (IOException e) {
			LOG.error(format("Unable write report file: %s", pathToReport));
			throw new RuntimeException(e);
		}
	}

	private FileReader newTemplateReader() {
		try {
			return new FileReader(new File(pathToTemplate));
		} catch (FileNotFoundException e) {
			LOG.error(format("Unable find template file: %s", pathToTemplate));
			throw new RuntimeException(e);
		}
	}
}
