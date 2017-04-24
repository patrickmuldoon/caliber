angular
		.module("qc")
		.controller(
				"qcAssessController",
				function($log, $scope, $timeout, $rootScope, chartsDelegate,
						caliberDelegate, qcFactory, allBatches) {
					$log.debug("Booted Trainer Assess Controller");

					$scope.batches = allBatches;
					$scope.bnote = null;
					$scope.faces = [];
					$scope.weeks = [];
					$scope.batchesByYear = [];

					// Note object
					function Note(noteId, content, status, week, batch,
							trainee, maxVisibility, type, qcFeedback) {
						this.noteId = noteId;
						this.content = content;
						this.week = week;
						this.batch = batch;
						this.trainee = trainee;
						this.maxVisibility = maxVisibility;
						this.type = type;
						this.qcFeedback = qcFeedback;
						this.qcStatus = status;
					}

					// Used to sort trainees in batch
					function compare(a, b) {
						if (a.name < b.name)
							return -1;
						if (a.name > b.name)
							return 1;
						return 0;
					}

					/**
					 * ***************************************** UI
					 * **********************************************
					 */
					// function to get notes
					$scope.getNotes = function() {
						// Check if there are not weeks
						if ($scope.currentWeek !== undefined) {
							// Get qc notes for selected batch
							caliberDelegate.qc
									.batchNote($scope.currentBatch.batchId,
											$scope.currentWeek)
									.then(
											function(notes) {
												// If no batch note found create
												// empty note
												if (notes === "") {
													$log.debug("EMPTY!");
													$scope.bnote = new Note(
															null,
															null,
															null,
															$scope.currentWeek,
															$scope.currentBatch,
															null, "ROLE_QC",
															"QC_BATCH", true);
												}
												// If note found set to note and
												// face
												else {
													$scope.bnote = notes;
													$scope.qcBatchAssess = notes.qcStatus;
												}
											});
							// Get qc notes for trainees in selected batch
							caliberDelegate.qc
									.traineeNote($scope.currentBatch.batchId,
											$scope.currentWeek)
									.then(
											function(notes) {
												$log.debug("NOTES RETRIEVED!");
												$log.debug(notes);
												for (var i = 0; i < $scope.currentBatch.trainees.length; i++) {
													var content = null;
													var status = null;
													var id = null;
													// Set content, status and
													// id to note if found
													for (var j = 0; j < notes.length; j++) {
														if ($scope.currentBatch.trainees[i].name === notes[j].trainee.name) {
															content = notes[j].content;
															status = notes[j].qcStatus;
															id = notes[j].noteId;
															break;
														}
													}
													// Push note object into
													// array, batch set to null
													// for trainee note always
													$scope.faces
															.push(new Note(
																	id,
																	content,
																	status,
																	$scope.currentWeek,
																	null,
																	$scope.currentBatch.trainees[i],
																	"ROLE_QC",
																	"QC_TRAINEE",
																	true));
												}
											});
							// If there are no weeks
						} else {
							$scope.bnote = null;
							for (var i = 0; i < $scope.currentBatch.trainees.length; i++) {
								$scope.faces.push(new Note(null, null, null,
										$scope.currentWeek,
										$scope.currentBatch,
										$scope.currentBatch.trainees[i], "ROLE_QC",
										"QC_TRAINEE", true));
							}
						}
					}

					// Used to pick face for batch
					$scope.pickOverallStatus = function(batch, pick) {
						$scope.qcBatchAssess = pick;
						$log.debug(batch.trainingName + " " + pick);
						$log.debug("bnote");
						$log.debug($scope.bnote);
						$scope.bnote.qcStatus = pick;
						$scope.saveQCNotes();
					};

					// starting scope vars
					$log.debug($scope.$parent.currentBatch);
					// If in reports get reports current batch
					if ($scope.$parent.currentBatch !== undefined) {
						$scope.currentBatch = $scope.$parent.currentBatch;
					} else {
						$scope.currentBatch = $scope.batches[0];
					}

					// create an array of numbers for number of weeks
					for (var i = 1; i <= $scope.currentBatch.weeks; i++) {
						$scope.weeks.push(i);
					}

					// Start function for reports to use
					function start() {
						$scope.trainingNameDate = $scope.batches[0].trainingName
								+ " " + $scope.batches[0].startDate;

						var curYear = new Date();
						$scope.selectedYear = curYear.getFullYear();
						batchYears();

						// Sort trainees alphabetically
						$scope.currentBatch.trainees.sort(compare);

						// Set current week to first week
						// If reports week is selected
						if ($scope.$parent.reportCurrentWeek !== undefined
								&& $scope.$parent.reportCurrentWeek !== "(All)") {
							$log.debug("Got report week");
							$scope.currentWeek = $scope.$parent.reportCurrentWeek;
						} else {
							$log.debug("No report week");
							$scope.currentWeek = $scope.weeks[0];
						}

						// get status types
						$scope.qcStatusTypes = [];
						caliberDelegate.all.enumQCStatus().then(
								function(types) {
									$log.debug(types);
									$scope.qcStatusTypes = types;
								});

						// ///////////////////////////////////////////////////////////////////////////////////////////
						// load note types
						caliberDelegate.all.enumNoteType().then(
								function(noteTypes) {
									$log.debug(noteTypes);
									// do something with note type
								});
						// Reset notes to empty array
						$scope.faces = [];
						// Get notes
						$scope.getNotes();
					}

					function traineeWeek() {

					}

					$scope.pickIndividualStatus = function(trainee, status,
							index) {
						$scope.faces[index].qcStatus = status;
						$scope.saveTraineeNote(index);
						$log.debug($scope.faces[index]);
					};

					// default -- view assessments table
					$scope.currentView = true;

					// back button
					$scope.back = function() {
						$scope.currentView = true;
					};

					/**
					 * Batch drop down select Select batches from current year
					 */
					$scope.selectCurrentBatch = function(index) {
						$log.debug("SELECTED DIFFERENT BATCH");
						if ($scope.$parent.currentBatch !== undefined) {
							$scope.currentBatch = $scope.$parent.currentBatch;
						} else {
							$scope.currentBatch = $scope.batchesByYear[index];
						}
						$scope.currentBatch.trainees.sort(compare);
						// Create week array for batch selected
						$scope.weeks = [];
						for (var i = 1; i <= $scope.currentBatch.weeks; i++) {
							$scope.weeks.push(i);
						}
						// Set current week to first week
						$scope.currentWeek = $scope.weeks[0];
						$scope.getNotes();
						wipeFaces();

						$scope.trainingNameDate = $scope.currentBatch.trainingName
								+ " " + $scope.currentBatch.startDate;
					};

					// Select week
					$scope.selectWeek = function(index) {
						// If reports week is selected
						if ($scope.$parent.reportCurrentWeek !== undefined
								&& $scope.$parent.reportCurrentWeek !== "(All)") {
							$scope.currentWeek = $scope.$parent.reportCurrentWeek;
						} else {
							$scope.currentWeek = $scope.weeks[index];
						}
						$scope.getNotes();
						wipeFaces();
					};

					// Show week
					$scope.showActiveWeek = function(index) {
						if ($scope.currentWeek === $scope.weeks[index])
							return "active";
					}

					$scope.createWeek = function() {
						caliberDelegate.trainer
								.createWeek($scope.currentBatch.batchId)
								.then(
										function(response) {
											$scope.currentBatch.weeks += 1;
											$scope.weeks
													.push($scope.currentBatch.weeks);
											$scope
													.showActiveWeek($scope.currentBatch.weeks);
											$scope
													.selectWeek($scope.currentBatch.weeks - 1); // the
											// new
											// index
											// of
											// the
											// week
											// selected
										});
					};

					// ///// wipe faces ;) and selections ///////
					function wipeFaces() {
						$scope.faces = [];
						$scope.qcBatchAssess = null;
						$scope.finalQCBatchNote = null;
					}

					/**
					 * ************************************************ GETTING
					 * NOTES ON TRAINEE
					 * *********************************************
					 */
					// Note for trainee
					$scope.noteOnTrainee = function(index) {
						return $scope.faces[index];
					};

					/**
					 * ******************************************* QCFeedBack
					 * **********************************************************
					 */
					var readMe = false;
					$scope.readOnlyPage = function() {
						readMe = !readMe;
						$log.debug(readMe);
						return readMe;
					}

					// Save trainee not for ng-blur
					$scope.saveTraineeNote = function(index) {
						$log.debug($scope.faces[index]);
						// Create if noteId is null so nothing in database
						if ($scope.faces[index].noteId === null || $scope.faces[index].noteId === undefined) {
							$log.debug("create");
							caliberDelegate.qc.createNote($scope.faces[index])
									.then(function(id) {
										$scope.faces[index].noteId = id;
									});
						}
						// Update if noteId is there
						else {
							$log.debug("update");
							caliberDelegate.qc.updateNote($scope.faces[index]);
						}
					};

					// Save batch note for ng-blur
					$scope.saveQCNotes = function() {
						// Create note
						if ($scope.bnote.noteId === null || $scope.bnote.noteId === undefined) {
							caliberDelegate.qc.createNote($scope.bnote).then(
							// Set id to created notes id
							function(id) {
								$scope.bnote.noteId = id;
							});
						}
						// Update existing note
						else {
							$log
									.debug(document
											.getElementById("qcBatchNotes").value);
							$log.debug(caliberDelegate.qc
									.updateNote($scope.bnote));
						}
					}

					/***********************************************************
					 * Save Button **
					 **********************************************************/

					$scope.showSaving = false;
					$scope.showCheck = false;
					$scope.showFloppy = true;
					$scope.saveQCandTrainee = function() {
						$scope.showFloppy = false
						$timeout(function() {
							$scope.showSaving = true;
						}, 480).then(function() {
							$timeout(function() {
								$scope.showSaving = false;
							}, 1000).then(function() {
								$scope.showCheck = true;
								$timeout(function() {
									$scope.showCheck = false;
								}, 2000).then(function() {
									$scope.showFloppy = true;
								});
							});
						});
					}
					// Call start function
					start();
					// Call start function when on reports page and batch and
					// week selected
					$rootScope.$on('qcBatchOverall', function() {
						$log.debug("QCBATCHOVERALL");
						start();
					});
					// Execute when on reports page and trainee and week
					// selected
					$rootScope.$on('qcTraineeWeek', function() {
						traineeWeek();
					});

					/**
					 * **************************************************
					 * Duplicate code from trainerManageController.js
					 * **************************************************
					 */

					/** Filter batches by year * */
					$scope.years = addYears();
					function addYears() {
						var currentYear = new Date().getFullYear();
						$scope.selectedYear = currentYear;

						var data = [];
						// List all years from (current year - 1) --> (current
						// year + 1)
						for (var y = currentYear + 1; y >= currentYear - 1; y--) {
							data.push(y)
						}
						return data;
					}

					$scope.selectYear = function(index) {
						$scope.selectedYear = $scope.years[index];
						sortByDate($scope.selectedYear);
						batchYears();
						$scope.currentBatch = $scope.batchesByYear[0];

						// Create week array for batch selected
						$scope.weeks = [];
						for (var i = 1; i <= $scope.currentBatch.weeks; i++) {
							$scope.weeks.push(i); 
						}

						if ($scope.batchesByYear.length === 0) {
							$scope.noBatches = true;
							$scope.noBatchesMessage = "No Batches were found for this year.";
						} else {
							$scope.noBatches = false;
							// createDefaultCharts();
							$scope.selectedYear = $scope.years[index];
							sortByDate($scope.selectedYear);

							if ($scope.batchesByYear.length > 0) {
								$scope.trainingNameDate = $scope.batchesByYear[0].trainingName
										+ " - "
										+ $scope.batchesByYear[0].startDate;
								$scope.thereAreBatches = true;
							} else {
								/**
								 * If no batches are available, display that
								 * there are no batches
								 */

								$scope.trainingNameDate = "No Batch Found";
								$scope.currentView = false;
								// $scope.thereAreBatches = false;
							}

							$log.debug($scope.batchesByYear);
						}
					};

					function sortByDate(currentYear) {
						$scope.selectedBatches = [];
						for (var i = 0; i < $scope.batches.length; i++) {
							var date = new Date($scope.batches[i].startDate);
							if (date.getFullYear() === currentYear) {
								$scope.selectedBatches.push($scope.batches[i]);
							}
						}
					}

					/**
					 * ********************************************************************
					 * Display Batch By Years
					 * ********************************************************************
					 */

					/**
					 * Store batch object(s) according to selected year into an
					 * array
					 */
					function batchYears() {
						$scope.batchesByYear = [];

						for (var i = 0; i < $scope.batches.length; i++) {
							if ($scope.selectedYear === parseInt($scope.batches[i].startDate
									.substring(0, 4))) {
								$scope.batchesByYear.push($scope.batches[i]);
							}
						}
					}
				});