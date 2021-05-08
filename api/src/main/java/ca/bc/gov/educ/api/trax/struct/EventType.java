package ca.bc.gov.educ.api.trax.struct;

/**
 * The enum Event type.
 */
public enum EventType {
  /**
   * Create student event type.
   */
  CREATE_STUDENT,
  /**
   * Update student event type.
   */
  UPDATE_STUDENT,
  /**
   * Add possible match event type.
   */
  ADD_POSSIBLE_MATCH,
  /**
   * Delete possible match event type.
   */
  DELETE_POSSIBLE_MATCH,
  /**
   * Create merge event type.
   */
  CREATE_MERGE,

  /**
   * Delete Merge Data.
   */
  DELETE_MERGE,
  GET_STUDENTS
}
