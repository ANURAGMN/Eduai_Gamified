package com.ncert7.aitutorandlab.domain.examplan

/**
 * Knowledge-bite goal for chapter-trial whiteboard nodes.
 *
 * A bite is one tutor teaching turn the learner received:
 * - opening AI turn after session start
 * - each later AI turn after a user message
 *
 * Opening and immediately leaving without turns must not complete the node.
 */
object WhiteboardTrialThresholds {
    const val DEFAULT_GOAL = 5
}
