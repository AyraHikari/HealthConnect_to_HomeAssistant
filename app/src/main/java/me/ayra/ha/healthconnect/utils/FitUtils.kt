package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BADMINTON
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BASEBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BASKETBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BIKING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BIKING_STATIONARY
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BOOT_CAMP
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BOXING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_CALISTHENICS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_CRICKET
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_DANCING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ELLIPTICAL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_EXERCISE_CLASS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_FENCING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_FOOTBALL_AMERICAN
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_FRISBEE_DISC
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_GOLF
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_GUIDED_BREATHING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_GYMNASTICS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_HANDBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_HIKING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ICE_HOCKEY
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ICE_SKATING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_MARTIAL_ARTS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_OTHER_WORKOUT
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_PADDLING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_PARAGLIDING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_PILATES
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RACQUETBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ROCK_CLIMBING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ROLLER_HOCKEY
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ROWING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_ROWING_MACHINE
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUGBY
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING_TREADMILL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SAILING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SCUBA_DIVING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SKATING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SKIING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SNOWBOARDING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SNOWSHOEING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SOCCER
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SOFTBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SQUASH
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STAIR_CLIMBING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRENGTH_TRAINING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRETCHING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SURFING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SWIMMING_OPEN_WATER
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SWIMMING_POOL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_TABLE_TENNIS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_TENNIS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_VOLLEYBALL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_WALKING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_WATER_POLO
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_WEIGHTLIFTING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_WHEELCHAIR
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_YOGA
import kotlin.math.pow
import kotlin.math.sqrt

object FitUtils {
    fun calculateHeartRateScore(heartRateData: List<Long>): Double {
        if (heartRateData.isEmpty()) return 0.0

        // Calculate Resting Heart Rate (RHR)
        val restingHeartRate = heartRateData.minOrNull() ?: return 0.0

        // Calculate Average Heart Rate
        val averageHeartRate = heartRateData.average()

        // Calculate Heart Rate Variability (HRV Approximation) using Standard Deviation
        val sumOfSquaredDifferences = heartRateData.sumOf { (it - averageHeartRate).pow(2) }
        val hrv = sqrt(sumOfSquaredDifferences / heartRateData.size)

        // Calculate Score
        val restingHeartRateScore = when {
            restingHeartRate < 60 -> 40.0  // Excellent
            restingHeartRate in 60..70 -> 30.0  // Good
            restingHeartRate in 71..80 -> 20.0  // Average
            else -> 10.0  // Poor
        }

        val averageHeartRateScore = when {
            averageHeartRate < 70 -> 30.0  // Excellent
            averageHeartRate in 70.0..80.0 -> 20.0  // Good
            else -> 10.0  // Poor
        }

        val hrvScore = when {
            hrv > 15 -> 30.0  // Excellent
            hrv in 10.0..15.0 -> 20.0  // Average
            else -> 10.0  // Poor
        }

        return restingHeartRateScore + averageHeartRateScore + hrvScore
    }

    fun calculateEnergyScore(sleepQualityScore: Double, heartRateData: List<Long>): Double {
        // Normalize Sleep Quality Score (Assuming it's already between 0 and 100)
        val normalizedSleepQualityScore = sleepQualityScore.coerceIn(0.0, 100.0)
        val restingHeartRate = heartRateData.minOrNull() ?: return 0.0
        val averageHeartRate = heartRateData.average()
        val sumOfSquaredDifferences = heartRateData.sumOf { (it - averageHeartRate).pow(2) }
        val hrv = sqrt(sumOfSquaredDifferences / heartRateData.size)

        // Normalize Resting Heart Rate Score
        // Lower RHR is better. For example:
        val rhrScore = when {
            restingHeartRate < 60 -> 100.0  // Excellent
            restingHeartRate in 60..70 -> 80.0  // Good
            restingHeartRate in 71..80 -> 60.0  // Average
            else -> 40.0  // Poor
        }

        // Normalize HRV Score
        // Higher HRV is better. For example:
        val hrvScore = when {
            hrv > 15 -> 100.0  // Excellent
            hrv in 10.0..15.0 -> 80.0  // Average
            else -> 60.0  // Below average
        }

        // Assign weights to each component
        val sleepWeight = 0.5
        val rhrWeight = 0.25
        val hrvWeight = 0.25

        // Calculate Energy Score
        val energyScore = (sleepWeight * normalizedSleepQualityScore) +
                (rhrWeight * rhrScore) +
                (hrvWeight * hrvScore)

        return energyScore
    }

    fun calculateCalories(steps: Long, weightKg: Double): Double {
        val caloriesPerStep = 0.0005 * weightKg
        return steps * caloriesPerStep
    }

    fun calculateDistanceInKm(steps: Long, heightCm: Double, isMale: Boolean): Double {
        val strideLength = if (isMale) {
            heightCm * 0.415
        } else {
            heightCm * 0.413
        }
        val distanceMeters = steps * strideLength / 100.0
        return distanceMeters / 1000.0
    }

    fun getSleepQualityDescription(score: Double): String {
        return when {
            score < 10 -> """
            Your sleep quality was extremely poor last night. You may be experiencing extreme fatigue, lack of focus, and a significant decrease in motivation. It's important to prioritize sleep as your body didn’t get the rest it needed to recover properly. Consider adjusting your sleep environment or routine to improve your rest. If poor sleep persists, it might be worth discussing with a healthcare professional.
        """.trimIndent()

            score in 10.0..19.99 -> """
            Your sleep quality was very low. You may feel exhausted, mentally foggy, and irritable today. This level of sleep might impact your ability to concentrate and handle tasks effectively. It’s crucial to address the underlying factors affecting your sleep, such as stress or inconsistent sleep patterns, to prevent long-term effects on your health and well-being.
        """.trimIndent()

            score in 20.0..29.99 -> """
            Your sleep quality was below average, which means you probably didn't get enough restorative rest. You may find it difficult to be productive, and your mood may be affected as well. Low energy levels are likely, so it might help to take short breaks and avoid pushing yourself too hard. Aim for a consistent bedtime and consider reducing screen time before bed to improve future sleep quality.
        """.trimIndent()

            score in 30.0..39.99 -> """
            Your sleep quality was somewhat poor. Although you may be able to function, you could still feel a bit groggy, and your energy might not last throughout the day. This type of sleep often lacks sufficient REM and deep sleep, which are crucial for recovery. It's a good idea to evaluate your sleep habits and make necessary adjustments to create a more restful environment.
        """.trimIndent()

            score in 40.0..49.99 -> """
            Your sleep quality was mediocre, indicating that you had a fair amount of rest, but it may not have been very restorative. You might be able to manage your tasks, but your focus and energy could wane as the day progresses. Paying attention to small changes, such as room temperature or reducing caffeine late in the day, might help improve your sleep quality.
        """.trimIndent()

            score in 50.0..59.99 -> """
            Your sleep quality was average. You may feel reasonably okay, though you might not feel fully refreshed. While you can get through your day, there’s a chance of experiencing some fatigue or lower motivation. Ensuring consistent sleep and wake times can help improve the overall quality of your rest and help you feel more energized in the mornings.
        """.trimIndent()

            score in 60.0..69.99 -> """
            Your sleep quality was decent. You should have enough energy to get through your day without much difficulty, but you might notice that your energy isn’t optimal. To feel even better, try focusing on a relaxing pre-sleep routine, such as light stretching, meditation, or reading, to help your body fully prepare for a restorative rest.
        """.trimIndent()

            score in 70.0..79.99 -> """
            Your sleep quality was good, and you’re likely feeling fairly well-rested. This level of sleep should support both physical and mental well-being, helping you maintain focus and productivity throughout the day. To maintain or further enhance your sleep, continue practicing good sleep hygiene, such as limiting late-night snacks or establishing a calming bedtime routine.
        """.trimIndent()

            score in 80.0..89.99 -> """
            Your sleep quality was very good, and you should be feeling refreshed and full of energy. This quality of sleep contributes positively to your health and helps maintain a good mood, strong concentration, and resilience to stress. Keeping up with your healthy sleep habits will ensure you continue to feel and perform at your best during the day.
        """.trimIndent()

            score in 90.0..100.0 -> """
            Your sleep quality was excellent. You are well-rested and ready for a productive and active day. This kind of sleep ensures your body and mind are fully restored, boosting your mood, energy, and cognitive abilities. Continue with your effective sleep strategies, as they’re clearly benefiting you, and consider sharing your success with others to help them improve their sleep too.
        """.trimIndent()

            else -> """
            Invalid score. Please provide a valid sleep quality score between 0 and 100.
        """.trimIndent()
        }
    }

    fun getRhrScoreDescription(heartRateData: List<Long>): String {
        val restingHeartRate = heartRateData.minOrNull() ?: return "No heart rate data"
        val rhrScore = when {
            restingHeartRate < 60 -> 100.0  // Excellent
            restingHeartRate in 60..70 -> 80.0  // Good
            restingHeartRate in 71..80 -> 60.0  // Average
            else -> 40.0  // Poor
        }
        return when {
            rhrScore < 60 -> """
            Your resting heart rate is excellent, falling below 60 beats per minute. This typically indicates strong cardiovascular health and that your body is in great shape, especially if you maintain regular physical activity. Athletes often have lower resting heart rates due to the efficiency of their heart. Make sure to maintain your active lifestyle, as this low RHR is a strong indicator of good overall fitness and relaxation. Remember, a low RHR also means your heart doesn't need to work as hard while you're at rest, which is a very positive sign for long-term health.
        """.trimIndent()

            rhrScore in 60.0..70.0 -> """
            Your resting heart rate is good, ranging between 60 and 70 beats per minute. This suggests that your cardiovascular system is functioning well, and your heart is fairly efficient at delivering blood throughout your body. A heart rate in this range is typical for people who engage in moderate physical activity. To improve further, consider incorporating a mix of aerobic exercises and strength training. Keeping stress levels in check and maintaining a healthy diet will also help keep your heart rate in this favorable range.
        """.trimIndent()

            rhrScore in 71.0..80.0 -> """
            Your resting heart rate is average, between 71 and 80 beats per minute. While this is generally acceptable, it suggests that there might be some room for improvement in terms of cardiovascular fitness. You might benefit from more regular physical activity, especially exercises that improve heart health, such as brisk walking, jogging, or swimming. Additionally, managing stress through relaxation techniques like meditation or yoga could help reduce your RHR over time, benefiting both your physical and mental health.
        """.trimIndent()

            else -> """
            Your resting heart rate is above 80 beats per minute, which may indicate that your cardiovascular system is under some strain. Factors such as lack of physical activity, stress, diet, or even dehydration can contribute to a higher resting heart rate. It’s a good idea to evaluate your lifestyle and identify areas that could be contributing to this elevated rate. Engaging in regular aerobic exercise, improving sleep quality, and managing stress are key strategies to help lower your RHR and enhance overall cardiovascular health. If this high resting heart rate persists, consider consulting with a healthcare professional for further advice.
        """.trimIndent()
        }
    }

    fun getEnergyScoreDescription(score: Double): String {
        return when {
            score < 10 -> """
            Your energy levels are extremely low today. This could be due to very poor sleep quality, high resting heart rate, or low heart rate variability. You may feel exhausted, unable to focus, and even physically drained. It’s important to prioritize rest and recovery today, perhaps by taking short naps, avoiding heavy tasks, and ensuring you’re well-hydrated and nourished. Consider adjusting your sleep environment or consulting a healthcare professional if such low energy levels persist.
        """.trimIndent()

            score in 10.0..19.99 -> """
            Your energy is quite low, and you might feel fatigued or overwhelmed. Poor sleep and high resting heart rate can contribute to feeling this way. It's likely you’ll struggle with focus, productivity, and managing stress today. Try to take frequent breaks, avoid high-stress activities, and practice relaxation techniques such as deep breathing. It might be helpful to get a bit of gentle exercise to support your energy and mood.
        """.trimIndent()

            score in 20.0..29.99 -> """
            Your energy levels are below average. You may find it difficult to maintain productivity or stay motivated throughout the day. You’re at risk of feeling tired and experiencing mood swings. Improving your energy might require focusing on a better sleep schedule, reducing caffeine in the evening, and incorporating light physical activity to enhance your overall heart health. Don't hesitate to take some extra rest breaks today if needed.
        """.trimIndent()

            score in 30.0..39.99 -> """
            Your energy level is somewhat low, and although you may be able to push through your day, you may feel more tired or irritable than usual. Both sleep quality and heart rate data suggest there is room for improvement in your routine. Focus on getting to bed earlier, creating a relaxing bedtime routine, and incorporating physical activities such as walking or yoga to improve both your sleep and heart health.
        """.trimIndent()

            score in 40.0..49.99 -> """
            Your energy today is average to below average. You might feel capable of completing basic tasks, but fatigue will likely slow you down. Your sleep quality and heart rate indicate that some positive adjustments can help boost your energy levels. Consider improving your diet with more fruits and vegetables, staying hydrated, and making your bedroom environment more conducive to restful sleep.
        """.trimIndent()

            score in 50.0..59.99 -> """
            Your energy levels are moderate today, though not optimal. You may feel capable of managing day-to-day activities, but might not be able to fully tap into your productivity potential. Regular exercise, consistent sleep, and effective stress management can all contribute to gradually raising your energy score. Incorporate some outdoor activity, as fresh air and sunlight can boost mood and energy.
        """.trimIndent()

            score in 60.0..69.99 -> """
            Your energy level is decent, and you should be able to handle most of the tasks you have planned for the day. You may have a little dip in energy in the afternoon but should generally feel okay. To continue improving, focus on maintaining a consistent sleep schedule, reducing unnecessary stress, and keeping up with physical activity that you enjoy, such as walking, cycling, or light sports.
        """.trimIndent()

            score in 70.0..79.99 -> """
            Your energy levels are good today, and you should be feeling reasonably well-rested and capable of tackling most of your planned activities. You’re likely to maintain focus and productivity throughout much of the day. Keep up with healthy habits, such as maintaining good sleep hygiene, exercising regularly, and eating balanced meals, to keep your energy high.
        """.trimIndent()

            score in 80.0..89.99 -> """
            Your energy level is very good today. You are likely feeling refreshed, motivated, and ready to accomplish your tasks with minimal fatigue. Your sleep and heart health are contributing positively to your well-being. Stay consistent with your healthy habits—proper sleep, regular physical activity, and stress management—to maintain and continue to boost your energy and overall health.
        """.trimIndent()

            score in 90.0..100.0 -> """
            Your energy level is excellent! You are feeling well-rested, physically capable, and mentally sharp today. This energy level is the result of excellent sleep quality, a healthy resting heart rate, and good heart rate variability. Keep doing what you’re doing—regular physical activity, stress management, a balanced diet, and good sleep hygiene are all key contributors to feeling this great. Share your tips with others who may need guidance!
        """.trimIndent()

            else -> """
            Invalid score. Please provide a valid energy score between 0 and 100.
        """.trimIndent()
        }
    }

    fun getEnergyScoreSummaryTitle(score: Double): String {
        return when {
            score < 10 -> "Prioritize Rest and Recovery"
            score in 10.0..19.99 -> "Focus on Rest and Stress Management"
            score in 20.0..29.99 -> "Improve Sleep and Physical Activity"
            score in 30.0..39.99 -> "Enhance Sleep Routine and Exercise"
            score in 40.0..49.99 -> "Better Sleep and Diet Adjustments"
            score in 50.0..59.99 -> "Improve Sleep and Exercise"
            score in 60.0..69.99 -> "Maintain Sleep Schedule and Stay Active"
            score in 70.0..79.99 -> "Continue Healthy Habits"
            score in 80.0..89.99 -> "Keep Consistent with Healthy Lifestyle"
            score in 90.0..100.0 -> "Maintain Excellent Habits"
            else -> "Invalid Score"
        }
    }

    fun getScoreInfo(score: Double): String {
        return when {
            score < 20 -> "Very Low"
            score in 20.0..39.99 -> "Needs Attention"
            score in 40.0..59.99 -> "Fair"
            score in 60.0..79.99 -> "Good"
            score in 80.0..89.99 -> "Very Good"
            score in 90.0..100.0 -> "Excellent"
            else -> "Invalid Score"
        }
    }

    fun getScoreColor(score: Double): String {
        return when {
            score >= 90.0 -> "#5E7893" // Lightened version of #2D4664 (Excellent)
            score in 80.0..89.99 -> "#A05556" // Lightened version of #642D2E (Very Good)
            score in 60.0..79.99 -> "#7A6BA3" // Lightened version of #3B2D64 (Good)
            score in 40.0..59.99 -> "#BFA65A" // Lightened version of #64592D (Fair)
            score in 20.0..39.99 -> "#5CA28F" // Lightened version of #2D6457 (Needs Attention)
            score < 20 -> "#6D9B65" // Lightened version of #2D642E (Very Low)
            else -> "#B0B0B0" // Invalid Score (Light Gray)
        }
    }

    fun Int.toExerciseName(): String {
        return when (this) {
            EXERCISE_TYPE_BADMINTON -> "Badminton"
            EXERCISE_TYPE_BASEBALL -> "Baseball"
            EXERCISE_TYPE_BASKETBALL -> "Basketball"
            EXERCISE_TYPE_BIKING -> "Biking"
            EXERCISE_TYPE_BIKING_STATIONARY -> "Stationary Biking"
            EXERCISE_TYPE_BOOT_CAMP -> "Boot Camp"
            EXERCISE_TYPE_BOXING -> "Boxing"
            EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
            EXERCISE_TYPE_CRICKET -> "Cricket"
            EXERCISE_TYPE_DANCING -> "Dancing"
            EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
            EXERCISE_TYPE_EXERCISE_CLASS -> "Exercise Class"
            EXERCISE_TYPE_FENCING -> "Fencing"
            EXERCISE_TYPE_FOOTBALL_AMERICAN -> "American Football"
            EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "Australian Football"
            EXERCISE_TYPE_FRISBEE_DISC -> "Frisbee/Disc"
            EXERCISE_TYPE_GOLF -> "Golf"
            EXERCISE_TYPE_GUIDED_BREATHING -> "Guided Breathing"
            EXERCISE_TYPE_GYMNASTICS -> "Gymnastics"
            EXERCISE_TYPE_HANDBALL -> "Handball"
            EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            EXERCISE_TYPE_HIKING -> "Hiking"
            EXERCISE_TYPE_ICE_HOCKEY -> "Ice Hockey"
            EXERCISE_TYPE_ICE_SKATING -> "Ice Skating"
            EXERCISE_TYPE_MARTIAL_ARTS -> "Martial Arts"
            EXERCISE_TYPE_OTHER_WORKOUT -> "Other Workout"
            EXERCISE_TYPE_PADDLING -> "Paddling"
            EXERCISE_TYPE_PARAGLIDING -> "Paragliding"
            EXERCISE_TYPE_PILATES -> "Pilates"
            EXERCISE_TYPE_RACQUETBALL -> "Racquetball"
            EXERCISE_TYPE_ROCK_CLIMBING -> "Rock Climbing"
            EXERCISE_TYPE_ROLLER_HOCKEY -> "Roller Hockey"
            EXERCISE_TYPE_ROWING -> "Rowing"
            EXERCISE_TYPE_ROWING_MACHINE -> "Rowing Machine"
            EXERCISE_TYPE_RUGBY -> "Rugby"
            EXERCISE_TYPE_RUNNING -> "Running"
            EXERCISE_TYPE_RUNNING_TREADMILL -> "Treadmill Running"
            EXERCISE_TYPE_SAILING -> "Sailing"
            EXERCISE_TYPE_SCUBA_DIVING -> "Scuba Diving"
            EXERCISE_TYPE_SKATING -> "Skating"
            EXERCISE_TYPE_SKIING -> "Skiing"
            EXERCISE_TYPE_SNOWBOARDING -> "Snowboarding"
            EXERCISE_TYPE_SNOWSHOEING -> "Snowshoeing"
            EXERCISE_TYPE_SOCCER -> "Soccer"
            EXERCISE_TYPE_SOFTBALL -> "Softball"
            EXERCISE_TYPE_SQUASH -> "Squash"
            EXERCISE_TYPE_STAIR_CLIMBING -> "Stair Climbing"
            EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "Stair Climbing Machine"
            EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
            EXERCISE_TYPE_STRETCHING -> "Stretching"
            EXERCISE_TYPE_SURFING -> "Surfing"
            EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Open Water Swimming"
            EXERCISE_TYPE_SWIMMING_POOL -> "Pool Swimming"
            EXERCISE_TYPE_TABLE_TENNIS -> "Table Tennis"
            EXERCISE_TYPE_TENNIS -> "Tennis"
            EXERCISE_TYPE_VOLLEYBALL -> "Volleyball"
            EXERCISE_TYPE_WALKING -> "Walking"
            EXERCISE_TYPE_WATER_POLO -> "Water Polo"
            EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
            EXERCISE_TYPE_WHEELCHAIR -> "Wheelchair"
            EXERCISE_TYPE_YOGA -> "Yoga"
            else -> "Unknown"
        }
    }
}