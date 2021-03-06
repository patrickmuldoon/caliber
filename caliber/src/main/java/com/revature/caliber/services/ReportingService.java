package com.revature.caliber.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.revature.caliber.beans.Assessment;
import com.revature.caliber.beans.AssessmentType;
import com.revature.caliber.beans.Batch;
import com.revature.caliber.beans.Category;
import com.revature.caliber.beans.Grade;
import com.revature.caliber.beans.Note;
import com.revature.caliber.beans.QCStatus;
import com.revature.caliber.beans.Trainee;
import com.revature.caliber.data.AssessmentDAO;
import com.revature.caliber.data.BatchDAO;
import com.revature.caliber.data.GradeDAO;
import com.revature.caliber.data.NoteDAO;
import com.revature.caliber.data.TraineeDAO;

/**
 * Exclusively used to generate data for charts
 * 
 * Provides logic concerning grade and aggregated data sets. Application logic
 * has no business being in a DAO nor in a Controller. This is the ideal place
 * for calculations
 * 
 * Team !Uncharted
 * 
 * @author Pier Yos
 * @author Hossain Yahya
 * @author Yanilda Peralta
 * @author Igor Gluskin
 * @author Ateeb Khawaja
 *
 */
@Service
public class ReportingService {

	private static final Logger log = Logger.getLogger(ReportingService.class);
	private static final String ALL = "(All)";

	private GradeDAO gradeDAO;
	private BatchDAO batchDAO;
	private TraineeDAO traineeDAO;
	private NoteDAO noteDAO;
	private AssessmentDAO assessmentDAO;

	@Autowired
	public void setGradeDAO(GradeDAO gradeDAO) {
		this.gradeDAO = gradeDAO;
	}

	@Autowired
	public void setBatchDAO(BatchDAO batchDAO) {
		this.batchDAO = batchDAO;
	}

	@Autowired
	public void setTraineeDAO(TraineeDAO traineeDAO) {
		this.traineeDAO = traineeDAO;
	}

	@Autowired
	public void setNoteDAO(NoteDAO noteDAO) {
		this.noteDAO = noteDAO;
	}

	@Autowired
	public void seAssessmentDAO(AssessmentDAO assessmentDAO) {
		this.assessmentDAO = assessmentDAO;
	}
	/*
	 *******************************************************
	 * Doughnut / Pie Charts
	 *******************************************************
	 */

	/**
	 * Pie chart displaying number of trainees that received red, yellow, green,
	 * x-Axis: QC Statuses y-Axis: Number of Trainees with
	 * 
	 * @param batchId
	 * @param weekNumber
	 * @return Map <The QC Status, Number of Trainees>
	 */
	public Map<QCStatus, Integer> getBatchWeekPieChart(Integer batchId, Integer weekNumber) {
		Map<QCStatus, Integer> results = new LinkedHashMap<>();
		for (QCStatus s : QCStatus.values()) {
			results.put(s, 0);
		}
		List<Note> notes = noteDAO.findAllQCTraineeNotes(batchId, weekNumber);
		for (Note n : notes) {
			if(n != null && results.get(n.getQcStatus()) != null)
				results.put(n.getQcStatus(), results.get(n.getQcStatus()) + 1);
		}
		return results;
	}

	public Map<QCStatus, Integer> pieChartCurrentWeekQCStatus(Integer batchId) {
		List<Batch> batch = batchDAO.findAllCurrentWithNotesAndTrainees();
		Batch currentOne = batch.stream().filter(e -> e.getBatchId() == batchId).findFirst().get();
		Map<Integer, Map<QCStatus, Integer>> batchWeekQCStats = utilSeparateQCTraineeNotesByWeek(currentOne);
		for (Integer i = batchWeekQCStats.size(); i > 0; i--) {
			Map<QCStatus, Integer> temp = batchWeekQCStats.get(i);
			if (temp.values().stream().mapToInt(Number::intValue).sum() != 0) {
				return temp;
			}
		}
		return new HashMap<>();
	}

	/*
	 *******************************************************
	 * Stacked Bar Chart   batchOverAllData
	 *******************************************************
	 */
	public Map<String, Map<QCStatus, Integer>> getAllBatchesCurrentWeekQCStackedBarChart() {
		Map<String, Map<QCStatus, Integer>> results = new ConcurrentHashMap<>();
		List<Batch> currentBatches = batchDAO.findAllCurrentWithNotes();  // changed to Notes
		currentBatches.parallelStream().forEach(b -> {
			Map<Integer, Map<QCStatus, Integer>> batchWeekQCStats = utilSeparateQCTraineeNotesByWeek(b);
			for (Integer i = batchWeekQCStats.size(); i > 0; i--) {
				Map<QCStatus, Integer> temp = batchWeekQCStats.get(i);
				if (temp.values().stream().mapToInt(Number::intValue).sum() != 0) {
					results.put(b.getTrainer().getName().substring(0,b.getTrainer().getName().indexOf(' '))+" - "+ // Trainer first name
								b.getTrainingName().substring(0,b.getTrainingName().indexOf(' ')), temp);   // Batch ID
					break;
				}
			}
		});
		return results;
	}

	public Map<Integer, Map<QCStatus, Integer>> utilSeparateQCTraineeNotesByWeek(Batch batch) {
		Map<Integer, Map<QCStatus, Integer>> results = new HashMap<>();
		Map<QCStatus, Integer> qcStatsMapTemplate = new LinkedHashMap<>();
		for (QCStatus q : QCStatus.values()) {
			qcStatsMapTemplate.put(q, 0);
		}
		for (Integer i = 1; i <= batch.getWeeks(); i++) {
			results.put(i, new HashMap<>(qcStatsMapTemplate));
		}
		for (Trainee t : batch.getTrainees()) {
			for (Note n : t.getNotes()) {
				if (n.getQcStatus() != null) {
					Map<QCStatus, Integer> temp = results.get((int) n.getWeek());
					Integer count = temp.get(n.getQcStatus()) + 1;
					temp.put(n.getQcStatus(), count);
					results.put((int) n.getWeek(), temp);
				}
			}
		}
		return results;
	}
	/*
	 *******************************************************
	 * Bar Charts
	 *******************************************************
	 */

	/**
	 * x-Axis: Assessment Names y-Axis: Average Batch Scores
	 * 
	 * @param batchId
	 * @param week
	 * @return
	 */
	public Map<String, Double[]> getBatchWeekAvgBarChart(int batchId, int week) {
		List<Trainee> trainees = traineeDAO.findAllByBatch(batchId);
		Map<String, Double[]> results = new ConcurrentHashMap<>();
		Arrays.stream(AssessmentType.values()).parallel().forEach(a -> {
			Map<Trainee, Double[]> temp = utilAvgBatchWeek(trainees, week, a);
			if (temp.size() > 0) {
				Double batchAvg = 0d;
				double batchRaw = temp.entrySet().iterator().next().getValue()[1];
				for (Entry<Trainee, Double[]> e : temp.entrySet()) {
					batchAvg += e.getValue()[0];
				}
				batchAvg = batchAvg / temp.size();
				if (!temp.entrySet().iterator().next().getValue()[0].isNaN()) {
					results.put(a.name(), new Double[] { batchAvg, batchRaw });
				}
			}
		});
		return results;
	}

	/**
	 * x-Axis: Trainee Names y-Axis: Average Trainee Score
	 * 
	 * @param batchId
	 * @param week
	 * @return Map<Trainee's name, Double Average Score>
	 */
	public Map<String, Double> getBatchWeekSortedBarChart(int batchId, int week) {
		List<Trainee> trainees = traineeDAO.findAllByBatch(batchId);
		Map<Trainee, Double> avgBatchWeek = utilAvgBatchWeek(trainees, week);
		Map<String, Double> result = new HashMap<>();
		for (Entry<Trainee, Double> t : avgBatchWeek.entrySet()) {
			result.put(t.getKey().getName(), t.getValue());
		}
		return result;
	}

	/**
	 * x-Axis: Assessment Name, (Weight as well) y-Axis: Average Scores
	 * 
	 * @param traineeId
	 * @param week
	 * @return Map<AssessmentType Name, [0: Trainee Overall Average, 1: Batch
	 *         Overall Average, 2: Score Weight]>
	 */
	public Map<String, Double[]> getBatchOverallTraineeBarChart(Integer batchId, Integer traineeId) {
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		Set<Grade> grades = trainees.stream().filter(e -> e.getTraineeId() == traineeId).findFirst().get().getGrades();
		Map<String, Double[]> results = new ConcurrentHashMap<>();
		Arrays.stream(AssessmentType.values()).parallel().forEach(a -> {
			int[] counts = { 0, 0 };
			Map<Integer, Double[]> avgTraineeWeek = utilAvgTraineeOverall(grades, a, batch.getWeeks());
			Map<Integer, Double[]> avgBatchWeek = utilAvgBatchOverall(trainees, a, batch.getWeeks());
			Double batchAvg = 0d;
			for (Entry<Integer, Double[]> e : avgBatchWeek.entrySet()) {
				counts[1] += e.getValue()[2];
				batchAvg += e.getValue()[0] * e.getValue()[2];
			}
			if (counts[1] > 0) {
				batchAvg = batchAvg / counts[1];
				Double traineeAvg = 0d;
				for (Entry<Integer, Double[]> e : avgTraineeWeek.entrySet()) {
					counts[0] += e.getValue()[2];
					traineeAvg += e.getValue()[0] * e.getValue()[2];
				}
				traineeAvg = traineeAvg / counts[0];
				results.put(a.name(), new Double[] { traineeAvg, batchAvg });
			}
		});
		return results;
	}

	/**
	 * x-Axis: Trainee y-Axis: Average score
	 * 
	 * @param batchId
	 * @return
	 */
	public Map<String, Double> getBatchOverallBarChart(Integer batchId) {
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		Map<String, Double> results = new ConcurrentHashMap<>();
		int weeks = batch.getWeeks();
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		trainees.parallelStream().forEach(trainee -> {
			Double avg = 0.d;
			int weeksWithGrades = 0;
			for (Integer i = 0; i < weeks; i++) {
				Double tempAvg = utilAvgTraineeWeek(trainee.getGrades(), i);
				if (tempAvg > 0) {
					weeksWithGrades++;
					avg += tempAvg;
				}
			}
			if (avg > 0.0 && weeksWithGrades > 0) {
				avg = avg / weeksWithGrades;
				results.put(trainee.getName(), avg);
			}
		});
		return results;
	}

	/**
	 * x-Axis: y-Axis:
	 * 
	 * @param traineeId
	 * @param week
	 * @return map<'assessmentType', {traineeAvg, batchAvg, weight Percentage}
	 */
	public Map<String, Double[]> getBatchWeekTraineeBarChart(Integer batchId, Integer traineeId, Integer week) {
		Map<String, Double[]> results = new ConcurrentHashMap<>();
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		Set<Grade> grades = trainees.stream().filter(e -> e.getTraineeId() == traineeId).findFirst().get().getGrades();
		Arrays.stream(AssessmentType.values()).parallel().forEach(a -> {
			Double[] avgTraineeWeek = utilAvgTraineeWeek(week, a, grades);
			if (avgTraineeWeek[0] > 0.0) {
				Map<Trainee, Double[]> avgBatchWeek = utilAvgBatchWeek(trainees, week, a);
				Double batchAvg = 0d;
				for (Map.Entry<Trainee, Double[]> e : avgBatchWeek.entrySet()) {
					batchAvg += e.getValue()[0];
				}
				batchAvg = batchAvg / avgBatchWeek.size();
				results.put(a.name(), new Double[] { avgTraineeWeek[0], batchAvg, avgTraineeWeek[1] });
			}
		});
		return results;
	}

	/*
	 *******************************************************
	 * Line Charts
	 *******************************************************
	 */

	/**
	 * x-Axis: y-Axis:
	 * 
	 * @param batchId
	 * @param week
	 * @param traineeId
	 * @return Map<'week', 'avgScore'>
	 */

	public Map<Integer, Double[]> getTraineeUpToWeekLineChart(int batchId, int week, int traineeId) {
		Map<Integer, Double[]> results = new HashMap<>();
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		Set<Grade> grades = trainees.stream().filter(e -> e.getTraineeId() == traineeId).findFirst().get().getGrades();
		for (int w = 1; w <= week; w++) {
			Double[] temp = { utilAvgTraineeWeek(grades, w), utilAvgBatchWeekValue(trainees, w) };
			if (temp[1] > 0.0) {
				results.put(w, temp);
			}
		}
		return results;
	}

	/**
	 * x-Axis: trainee avg, batch avg y-Axis: week
	 * 
	 * @param batchId,
	 * @param traineeId
	 * @return Map<Total Weeks, Double[0:Trainee Overall Average 1: Batch
	 *         Overall Average]>
	 * 
	 */
	public Map<Integer, Double[]> getTraineeOverallLineChart(Integer batchId, Integer traineeId) {
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		Set<Grade> grades = trainees.stream().filter(e -> e.getTraineeId() == traineeId).findFirst().get().getGrades();
		Map<Integer, Double> traineeAvgOverall = utilAvgTraineeOverall(grades, batch.getWeeks());
		Map<Integer, Double> batchAvgOverall = utilAvgBatchOverall(trainees, batch.getWeeks());
		Map<Integer, Double[]> results = new HashMap<>();
		int totalWeeks = batch.getWeeks();
		for (int i = 1; i <= totalWeeks; i++) {
			Double batchWeekScore = batchAvgOverall.get(i);
			Double traineeWeekScore = traineeAvgOverall.get(i);
			if (traineeWeekScore > 0) {
				results.put(i, new Double[] { traineeWeekScore, batchWeekScore });
			}
		}
		return results;
	}

	/**
	 * x-Axis: y-Axis: Method for Controller to fetch Week number Batch Average
	 * Score
	 * 
	 * @param batchId
	 * @return Map<Week #, Double Average Score>
	 */
	public Map<Integer, Double> getBatchOverallLineChart(int batchId) {
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
		return utilAvgBatchOverall(trainees, batch.getWeeks());
	}


	public Map<String, Map<Integer, Double>> getAllCurrentBatchesLineChart() {
		Map<String, Map<Integer, Double>> results = new ConcurrentHashMap<>();
		List<Batch> batches = batchDAO.findAllCurrentWithTrainees();  // changed to Trainees
		batches.parallelStream().forEach(batch -> {
			List<Trainee> trainees = new ArrayList<>(batch.getTrainees());
			results.put(batch.getTrainer().getName().substring(0,batch.getTrainer().getName().indexOf(' '))+" - "+ //Trainer First name
					batch.getTrainingName().substring(0,batch.getTrainingName().indexOf(' ')),   // Batch ID
					utilAvgBatchOverall(trainees, batch.getWeeks()));
		});
		return results;
	}

	public Map<String, Map<Integer, Double>> getAllCurrentBatchesLineChartConcurrent() {
		Map<String, Map<Integer, Double>> results = new ConcurrentHashMap<>();
		List<Batch> batches = batchDAO.findAllCurrentWithNotesAndTrainees();
		batches.parallelStream().forEach(b -> {
			List<Trainee> trainees = new ArrayList<>(b.getTrainees());
			results.put(b.getTrainingName(), utilAvgBatchOverall(trainees, b.getWeeks()));
		});
		return results;
	}
	/*
	 *******************************************************
	 * Radar Charts
	 *******************************************************
	 */

	/**
	 * label-Axis: value-Axis:
	 * 
	 * @param traineeId
	 * @param week
	 * @return
	 */
	public Map<String, Double> getTraineeUpToWeekRadarChart(Integer traineeId, Integer week) {
		List<Grade> grades = gradeDAO.findByTrainee(traineeId);
		List<Grade> weekgrades = grades.stream().filter(g -> g.getAssessment().getWeek() <= week)
				.collect(Collectors.toList());
		Map<Category, Double[]> skills = utilAvgSkills(weekgrades);
		return utilReplaceCategoryWithSkillName(skills);
	}

	/**
	 * label-Axis: value-Axis:
	 * 
	 * @param traineeId
	 * @return
	 */
	public Map<String, Double> getTraineeOverallRadarChart(Integer traineeId) {
		List<Grade> grades = gradeDAO.findByTrainee(traineeId);
		Map<Category, Double[]> skills = utilAvgSkills(grades);
		return utilReplaceCategoryWithSkillName(skills);
	}

	/**
	 * label-Axis: value-Axis:
	 * 
	 * @param batchId
	 * @return
	 */
	public Map<String, Double> getBatchOverallRadarChart(Integer batchId) {
		List<Grade> grades = gradeDAO.findByBatch(batchId);
		Map<Category, Double[]> skills = utilAvgSkills(grades);
		log.info("getBatchOverallRadarChart : "+utilReplaceCategoryWithSkillName(skills));
		return utilReplaceCategoryWithSkillName(skills);
	}

	/**
	 * Return the Category Scores for each trainee in a batch
	 * 
	 * @param batchId
	 * @return Map<Trainee Name, Map<Category Name, Category Score>>
	 */
	public Map<String, Map<String, Double>> getBatchAllTraineesOverallRadarChart(Integer batchId) {
		Map<String, Map<String, Double>> results = new ConcurrentHashMap<>();
		Batch batch = batchDAO.findOneWithTraineesAndGrades(batchId);
		batch.getTrainees().parallelStream().forEach(t -> {
			Map<Category, Double[]> skills = utilAvgSkills(new ArrayList<>(t.getGrades()));
			results.put(t.getName(), utilReplaceCategoryWithSkillName(skills));
		});
		return results;
	}
	/*
	 *******************************************************
	 * Misc Reports
	 *******************************************************
	 */

	public Double getAvgBatchWeekValue(Integer batchId, Integer week) {
		List<Trainee> trainees = traineeDAO.findAllByBatch(batchId);
		return utilAvgBatchWeekValue(trainees, week);
	}

	public Set<String> getTechnologiesForTheWeek(Integer batchId, Integer week) {
		List<Assessment> assessments = assessmentDAO.findByWeek(batchId, week);
		Set<String> results = new TreeSet<>();
		assessments.forEach(a -> results.add(a.getCategory().getSkillCategory()));
		return results;
	}

	/*
	 *******************************************************
	 * Batch Comparison Data
	 *******************************************************
	 */
	public Double getBatchComparisonAvg(String skill, String training, Date startDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		List<Batch> allBatches = batchDAO.findAllAfterDate(cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.YEAR));
		List<Batch> filteredBatches;
		if (skill.equalsIgnoreCase(ALL)) {
			if (training.equalsIgnoreCase(ALL)) {
				filteredBatches = allBatches;
			} else {
				filteredBatches = allBatches.parallelStream().filter(b -> b.getTrainingType().name().equals(training))
						.collect(Collectors.toList());
			}

		} else {
			if (training.equalsIgnoreCase(ALL)) {
				filteredBatches = allBatches.parallelStream().filter(b -> b.getTrainingType().name().equals(skill))
						.collect(Collectors.toList());
			} else {
				filteredBatches = allBatches.parallelStream().filter(b -> b.getTrainingType().name().equals(skill))
						.filter(b -> b.getTrainingType().name().equals(training)).collect(Collectors.toList());
			}
		}

		Object lock = new Object();
		List<Double> results = new ArrayList<>();
		filteredBatches.parallelStream().forEach(batch -> {
			Double temp = utilAvgBatch(new ArrayList<Trainee>(batch.getTrainees()), batch.getWeeks());
			synchronized (lock) {
				results.add(temp);
			}
		});
		Double result = results.parallelStream().mapToDouble(Double::doubleValue).sum();
		result = result / filteredBatches.size();

		return result;
	}
	/*
	 *******************************************************
	 * Utility Methods
	 *******************************************************
	 */

	public Double utilAvgBatch(List<Trainee> trainees, int weeks) {
		Double result = 0.0;
		for (Trainee trainee : trainees) {
			Double avg = 0.d;
			int weeksWithGrades = 0;
			for (Integer i = 0; i < weeks; i++) {
				Double tempAvg = utilAvgTraineeWeek(trainee.getGrades(), i);
				if (tempAvg > 0) {
					weeksWithGrades++;
					avg += tempAvg;
				}
			}
			if (avg > 0.0 && weeksWithGrades > 0) {
				avg = avg / weeksWithGrades;
				result += avg;
			}
		}
		result = result / trainees.size();
		return result;
	}

	/**
	 * Gets the average for a given Trainee ID for the entire week for one
	 * particular assessment. One Week -> One Trainee -> Average Score -> One
	 * Assessment Type
	 * 
	 * @param traineeId
	 * @param week
	 * @param assessmentType
	 * @return Double [0: Average Trainee score for the week for AssessmentType,
	 *         1: Weight of that assessment type, 2: Number of Assessments of
	 *         Each Type}
	 */
	public Double[] utilAvgTraineeWeek(Integer week, AssessmentType assessmentType, Set<Grade> allGrade) {
		List<Grade> gradesForTheWeek = allGrade.stream().filter(el -> el.getAssessment().getWeek() == week)
				.collect(Collectors.toList());
		Double totalRawScore = gradesForTheWeek.stream().mapToDouble(el -> el.getAssessment().getRawScore()).sum();
		List<Grade> gradesForAssessment = gradesForTheWeek.stream()
				.filter(e -> e.getAssessment().getType().name().equalsIgnoreCase(assessmentType.name()))
				.collect(Collectors.toList());
		Double[] result = { 0d, 0d, 0d };
		if (gradesForAssessment == null || gradesForAssessment.isEmpty()) {
			return result;
		} else {
			result[2] = (double) gradesForAssessment.size();
		}
		for (Grade grade : gradesForAssessment) {
			result[0] += (grade.getScore() * grade.getAssessment().getRawScore() / totalRawScore);
			result[1] += grade.getAssessment().getRawScore();
		}
		result[1] = result[1] / totalRawScore * 100;
		result[0] = result[0] / result[1] * 100;
		return result;
	}

	/**
	 * Gets the average for a given Batch ID for the entire week for one
	 * particular assessment. One Week -> All Trainees in Batch -> Average Score
	 * -> One Assessment Type
	 * 
	 * @param batchId
	 * @param week
	 * @param assessmentType
	 * @return Map<'Trainee in Batch, Double[0: Average score for all trainees
	 *         for the week for assessmentType, weight for assessment}
	 */
	public Map<Trainee, Double[]> utilAvgBatchWeek(List<Trainee> trainees, Integer week,
			AssessmentType assessmentType) {
		Map<Trainee, Double[]> results = new HashMap<>();
		for (Trainee trainee : trainees) {
			Set<Grade> gradeList = new HashSet<>(trainee.getGrades());
			Double[] averageTraineeWeeklyScores = utilAvgTraineeWeek(week, assessmentType, gradeList);
			if (averageTraineeWeeklyScores[0] > 0.0) {
				results.put(trainee, averageTraineeWeeklyScores);
			}
		}
		return results;
	}

	/**
	 * Get Weighted Average for a single assessment type for a given Trainee ID,
	 * returning weeks as keys in the map and the corresponding values of
	 * weight(%) and scores. All Weeks -> One Trainee -> Average Score -> One
	 * Assessment Type
	 * 
	 * @param traineeId
	 * @param assessmentType
	 * @return Map<'week', {'score', 'weight'}>
	 */
	public Map<Integer, Double[]> utilAvgTraineeOverall(Set<Grade> grades, AssessmentType assessmentType, int weeks) {
		Map<Integer, Double[]> results = new HashMap<>();
		for (Integer i = 1; i <= weeks; i++) {
			Double[] avg = utilAvgTraineeWeek(i, assessmentType, grades);
			if (avg[0] > 0.0) {
				results.put(i, avg);
			}
		}
		return results;
	}

	/**
	 * Get Weighted Average for a the whole batch for all the weeks per an
	 * assessment All Weeks -> All Trainees In Batch -> Average Score -> One
	 * Assessment Type
	 * 
	 * @param batchId
	 * @param assessmentType
	 * @return Map<Week #s, Double[]: 0: Score, 1: Weight>
	 */
	public Map<Integer, Double[]> utilAvgBatchOverall(List<Trainee> trainees, AssessmentType assessmentType,
			int weeks) {
		Map<Integer, Double[]> results = new HashMap<>();
		for (Integer i = 1; i <= weeks; i++) {
			Map<Trainee, Double[]> temp = utilAvgBatchWeek(trainees, i, assessmentType);
			if (temp.size() > 0) {
				Double[] avg = { 0d, 0d, 0d };
				avg[1] = temp.values().iterator().next()[1];
				avg[2] = temp.values().iterator().next()[2];
				for (Entry<Trainee, Double[]> t : temp.entrySet()) {
					avg[0] += t.getValue()[0];
				}
				avg[0] = avg[0] / temp.size();
				results.put(i, avg);
			}
		}
		return results;
	}

	/**
	 * Gets the average score of a trainee over the week for all assessments.
	 * 
	 * @param traineeId
	 * @param week
	 * @return Double average score over the week for all assessments
	 */
	public Double utilAvgTraineeWeek(Set<Grade> allGrades, int week) {
		List<Grade> gradesForTheWeek = allGrades.stream().filter(el -> el.getAssessment().getWeek() == week)
				.collect(Collectors.toList());
		Double totalRawScore = gradesForTheWeek.stream().mapToDouble(el -> el.getAssessment().getRawScore()).sum();
		Double result = 0d;
		for (Grade grade : gradesForTheWeek) {
			result += (grade.getScore() * grade.getAssessment().getRawScore() / totalRawScore);
		}
		return result;
	}

	/**
	 * Takes a batchId and Week number, and returns the average score per
	 * trainee for that week.
	 * 
	 * @param batchId
	 * @param week
	 * @return Map<Trainee in Batch, Average Total Assessment Score>
	 */
	public Map<Trainee, Double> utilAvgBatchWeek(List<Trainee> trainees, Integer week) {
		Map<Trainee, Double> results = new HashMap<>();
		for (Trainee trainee : trainees) {
			Set<Grade> grades = new HashSet<>(trainee.getGrades());
			Double traineeWeeklyGradeAverage = utilAvgTraineeWeek(grades, week);
			if (traineeWeeklyGradeAverage > 0.0) {
				results.put(trainee, utilAvgTraineeWeek(grades, week));
			}
		}
		return results;
	}

	/**
	 * Gets the Week numbers and the average scores for all assessments for the
	 * given trainee id for all weeks.
	 * 
	 * @param traineeId
	 * @return Map<Week Number, Average Total Assessment Score Per Week>
	 */
	public Map<Integer, Double> utilAvgTraineeOverall(Set<Grade> grades, int weeks) {
		Map<Integer, Double> results = new HashMap<>();
		for (Integer i = 1; i <= weeks; i++) {
			results.put(i, utilAvgTraineeWeek(grades, i));
		}
		return results;
	}

	/**
	 * Takes in batch ID, and returns a Map with the Week and the average Grades
	 * for that week for the batch.
	 * 
	 * @param batchId
	 * @return Map<Week Number, Double Average Score for All Assessments For the
	 *         Week>
	 */
	public Map<Integer, Double> utilAvgBatchOverall(List<Trainee> trainees, Integer weeks) {
		Map<Integer, Double> results = new HashMap<>();
		for (Integer i = 1; i <= weeks; i++) {
			Map<Trainee, Double> temp = utilAvgBatchWeek(trainees, i);
			Double avg = 0d;
			for (Map.Entry<Trainee, Double> t : temp.entrySet()) {
				avg += t.getValue();
			}
			if (avg > 0.0 && temp.size() > 0) {
				avg = avg / temp.size();
				results.put(i, avg);
			}
		}
		return results;
	}

	/**
	 * Takes in a List of Grades and Returns an Map with Categories and Averages
	 * for Each Categories
	 * 
	 * @param traineeId
	 * @param weekNumber
	 * @return Map<Category, Double[0: Average, 1: Number of assessments for
	 *         that skill]>
	 */
	public Map<Category, Double[]> utilAvgSkills(List<Grade> grades) {
		Map<Category, Double[]> results = new HashMap<>();
		for (Grade g : grades) {
			Category skill = g.getAssessment().getCategory();
			if (results.get(skill) == null) {
				Double[] temp = { g.getScore(), 1.0 };
				results.put(skill, temp);
			} else {
				Double[] temp = results.get(skill);
				temp[0] = ((temp[0] * temp[1]) + g.getScore()) / (temp[1] + 1);
				temp[1]++;
				results.put(skill, temp);
			}
		}
		return results;
	}

	/**
	 * Takes a Map of Category, Double and converts it to Category Skill Names
	 * and Double
	 * 
	 * @param skills
	 * @return Map<Category Skill Name, Double Same as Input>
	 */
	public Map<String, Double> utilReplaceCategoryWithSkillName(Map<Category, Double[]> skills) {
		Map<String, Double> skillsWithLabels = new TreeMap<>();
		for (Entry<Category, Double[]> entry : skills.entrySet()) {
			skillsWithLabels.put(entry.getKey().getSkillCategory(), entry.getValue()[0]);
		}
		return skillsWithLabels;
	}

	/**
	 * Takes the weighted sum of scores by assessment type, averages that with
	 * all other assessments for the week, sums the value for all trainees overa
	 * wekk, and finally divides by number of trainees
	 * 
	 * @param batchId
	 * @param week
	 * @return Double [Average Value of Trainee Scores Weighted by Category
	 *         Types]
	 */
	public Double utilAvgBatchWeekValue(List<Trainee> trainees, Integer week) {
		Map<Trainee, Double> traineeAverageGrades = utilAvgBatchWeek(trainees, week);
		// weeklyBatchAverage
		return traineeAverageGrades.entrySet().stream().mapToDouble(e -> e.getValue()).sum()
				/ traineeAverageGrades.size();
	}
}
