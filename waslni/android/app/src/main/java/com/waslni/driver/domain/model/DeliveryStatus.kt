package com.waslni.driver.domain.model

/**
 * Delivery lifecycle states.
 *
 * State machine (see PRD §5.3):
 *
 *   PENDING ──→ ASSIGNED ──→ ON_THE_WAY ──→ ARRIVED ──→ DELIVERED
 *      │            │             │              │
 *      └────────────┴─────────────┴──────────────┘
 *                          ↓
 *                      CANCELLED
 *
 * Terminal states: DELIVERED, CANCELLED.
 *
 * The driver app in practice skips PENDING and ASSIGNED — when a driver
 * taps "Start Delivery" we go straight to ON_THE_WAY. The full state machine
 * exists because the backend (and later the admin panel) needs PENDING/
 * ASSIGNED for the assignment flow.
 */
enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    ON_THE_WAY,
    ARRIVED,
    DELIVERED,
    CANCELLED;

    /**
     * True if no further transitions are allowed from this state.
     */
    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED

    /**
     * True if the delivery is "in flight" — affects whether the map shows
     * an active-delivery marker for the related customer.
     */
    val isActive: Boolean
        get() = this == ON_THE_WAY || this == ARRIVED

    /**
     * Returns the set of statuses that can legally follow this one.
     */
    fun allowedNextStates(): Set<DeliveryStatus> = when (this) {
        PENDING    -> setOf(ASSIGNED, CANCELLED)
        ASSIGNED   -> setOf(ON_THE_WAY, CANCELLED)
        ON_THE_WAY -> setOf(ARRIVED, CANCELLED)
        ARRIVED    -> setOf(DELIVERED, CANCELLED)
        DELIVERED  -> emptySet()
        CANCELLED  -> emptySet()
    }

    /**
     * True if transitioning from this status to [target] is legal.
     */
    fun canTransitionTo(target: DeliveryStatus): Boolean =
        target in allowedNextStates()

    companion object {
        fun fromString(value: String): DeliveryStatus =
            valueOf(value.uppercase())
    }
}
