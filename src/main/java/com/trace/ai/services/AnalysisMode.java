package com.trace.ai.services;

/**
 * Analysis mode selector for initial failure analysis.
 * 
 * <p>This enum defines the two analysis modes available for initial failure analysis:</p>
 * <ul>
 *   <li><strong>OVERVIEW:</strong> Routes to a concise summary prompt for quick triage</li>
 *   <li><strong>FULL:</strong> Uses the detailed prompt for comprehensive analysis</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public enum AnalysisMode {
    /**
     * Concise summary mode for quick triage and initial assessment.
     */
    OVERVIEW,
    
    /**
     * Detailed analysis mode for comprehensive investigation.
     */
    FULL
}

