package com.revature.caliber.controllers;

import java.util.List;

import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.revature.caliber.beans.Assessment;
import com.revature.caliber.services.AssessmentService;

/**
 * Used for assessment CRUD operations. Includes both Trainer and QC assessments
 * 
 * @author Patrick Walsh
 *
 */
@RestController
public class AssessmentController {

	private static final Logger log = Logger.getLogger(AssessmentController.class);
	private AssessmentService assessmentService;

	@Autowired
	public void setAssessmentService(AssessmentService assessmentService) {
		this.assessmentService = assessmentService;
	}

	/*
	 *******************************************************
	 * TODO ASSESSMENT SERVICES
	 *
	 *******************************************************
	 */

	/**
	 * QC can no longer create assessment, trainer only function
	 * Create assessment response entity.
	 *
	 * @param assessment
	 *            the assessment
	 * @return the response entity
	 */
	@RequestMapping(value = "/trainer/assessment/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public ResponseEntity<Void> createAssessment(@Valid @RequestBody Assessment assessment) {
		log.info("Creating assessment: " + assessment);
		assessmentService.save(assessment);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	/**
	 * Delete assessment response entity.
	 *
	 * @param id
	 *            the id
	 * @return the response entity
	 */
	@RequestMapping(value = "/trainer/assessment/delete/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public ResponseEntity<Void> deleteAssessment(@PathVariable Long id) {
		log.info("Deleting assessment: " + id);
		Assessment assessment = new Assessment();
		assessment.setAssessmentId(id);
		assessmentService.delete(assessment);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	/**
	 * Update assessment response entity.
	 *
	 * @param assessment
	 *            the assessment
	 * @return the response entity
	 */
	@RequestMapping(value = "/trainer/assessment/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public ResponseEntity<Assessment> updateAssessment(@Valid @RequestBody Assessment assessment) {
		log.info("Updating assessment: " + assessment);
		assessmentService.update(assessment);
		return new ResponseEntity<>(assessment,HttpStatus.OK);
	}

	/**
	 * FIND ASSESSMENT BY WEEK
	 * 
	 * @param batch
	 * @param week
	 * @return
	 */
	@RequestMapping(value = "/trainer/assessment/{batchId}/{week}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public ResponseEntity<List<Assessment>> findAssessmentByWeek(@PathVariable Integer batchId,
			@PathVariable Integer week) {
		log.debug("Find assessment by week number " + week + " for batch " + batchId + " ");
		List<Assessment> assessments = assessmentService.findAssessmentByWeek(batchId, week);
		return new ResponseEntity<>(assessments, HttpStatus.OK);
	}
	
}

