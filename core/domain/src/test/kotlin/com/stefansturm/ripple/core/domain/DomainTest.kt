package com.stefansturm.ripple.core.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DomainTest {
    @Test
    fun unitConversionUsesDeterministicIntegerMilliliters() {
        assertEquals(250, UnitConverter.toMl(250, VolumeUnit.MILLILITERS).value)
        assertEquals(8, UnitConverter.toDisplay(Milliliters(250), VolumeUnit.FLUID_OUNCES))
        assertEquals(237, UnitConverter.toMl(8, VolumeUnit.FLUID_OUNCES).value)
    }

    @Test
    fun uiAmountValidationEnforcesTenMilliliterSteps() {
        RippleValidation.validateUiAmount(Milliliters(50))
        RippleValidation.validateUiAmount(Milliliters(2000))
        assertThrows(IllegalArgumentException::class.java) {
            RippleValidation.validateUiAmount(Milliliters(55))
        }
    }

    @Test
    fun calculatedGoalUsesMassRoundingActivityAndWorkoutBonus() {
        val profile = UserProfile(bodyMassKg = 70, activityLevel = ActivityLevel.MODERATE)
        val goal = GoalSettings(mode = GoalMode.CALCULATED)
        assertEquals(2650, GoalCalculator.calculate(profile, goal, workoutBonusMl = 0).value)
    }

    @Test
    fun manualGoalWinsOverCalculatedInputs() {
        val profile = UserProfile(bodyMassKg = 90, activityLevel = ActivityLevel.HIGH)
        val goal = GoalSettings(mode = GoalMode.MANUAL, manualGoalMl = Milliliters(1800))
        assertEquals(1800, GoalCalculator.calculate(profile, goal, workoutBonusMl = 700).value)
    }

    @Test
    fun containerNamesAreTrimmedAndBounded() {
        assertEquals("Glass", RippleValidation.validateContainerName(" Glass "))
        assertThrows(IllegalArgumentException::class.java) {
            RippleValidation.validateContainerName(" ")
        }
    }

    @Test
    fun clockTimeValidatesBounds() {
        ClockTime(7, 30)
        assertThrows(IllegalArgumentException::class.java) { ClockTime(24, 0) }
    }

    @Test
    fun defaultInstantIsAStableValueTypeBoundary() {
        val now = Instant.now()
        val command = LogIntakeCommand(Milliliters(250), IntakeSource.APP, now)
        assertEquals(now, command.date)
    }
}
