const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

setGlobalOptions({ maxInstances: 10 });

admin.initializeApp();
const db = admin.firestore();

function getManilaNow() {
  const manilaString = new Date().toLocaleString("en-US", {
    timeZone: "Asia/Manila",
  });
  return new Date(manilaString);
}

function getDateKeyFromDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function getManilaDateParts(date = new Date()) {
  const manilaString = date.toLocaleString("en-US", {
    timeZone: "Asia/Manila",
  });
  const manilaDate = new Date(manilaString);

  return {
    dateKey: getDateKeyFromDate(manilaDate),
    hour: manilaDate.getHours(),
    manilaDate,
  };
}

function shiftDateKey(dateKey, days) {
  const [year, month, day] = dateKey.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() + days);
  return getDateKeyFromDate(date);
}

function parseDateFromMessage(message, fallbackToday) {
  const lower = String(message || "").toLowerCase();

  if (lower.includes("kahapon") || lower.includes("yesterday")) {
    return shiftDateKey(fallbackToday, -1);
  }

  if (lower.includes("today") || lower.includes("ngayon")) {
    return fallbackToday;
  }

  const isoMatch = lower.match(/\b(20\d{2}-\d{2}-\d{2})\b/);
  if (isoMatch) return isoMatch[1];

  const monthMap = {
    january: "01",
    february: "02",
    march: "03",
    april: "04",
    may: "05",
    june: "06",
    july: "07",
    august: "08",
    september: "09",
    october: "10",
    november: "11",
    december: "12",
  };

  const longDateMatch = lower.match(
    /\b(january|february|march|april|may|june|july|august|september|october|november|december)\s+(\d{1,2})(?:,\s*(\d{4}))?\b/
  );

  if (longDateMatch) {
    const monthName = longDateMatch[1];
    const day = String(Number(longDateMatch[2])).padStart(2, "0");
    const year = longDateMatch[3] || fallbackToday.slice(0, 4);
    const month = monthMap[monthName];
    return `${year}-${month}-${day}`;
  }

  return null;
}

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function pickFirstString(obj = {}, keys = []) {
  for (const key of keys) {
    const value = obj[key];
    if (typeof value === "string" && value.trim()) return value.trim();
  }
  return null;
}

function tsToDateKey(timestamp) {
  const ms = toNumber(timestamp);
  if (!ms) return null;
  const d = new Date(ms);
  if (Number.isNaN(d.getTime())) return null;

  const manilaString = d.toLocaleString("en-US", { timeZone: "Asia/Manila" });
  const manilaDate = new Date(manilaString);
  return getDateKeyFromDate(manilaDate);
}

function averageFromTotalAndCount(total, count) {
  if (!count) return 0;
  return Number((total / count).toFixed(2));
}

function sumOf(records, key) {
  return Number(records.reduce((sum, item) => sum + toNumber(item[key]), 0).toFixed(2));
}

function maxRecordBy(records, key) {
  if (!records.length) return null;
  return records.reduce((max, item) => {
    if (!max) return item;
    return toNumber(item[key]) > toNumber(max[key]) ? item : max;
  }, null);
}

function minRecordBy(records, key) {
  if (!records.length) return null;
  return records.reduce((min, item) => {
    if (!min) return item;
    return toNumber(item[key]) < toNumber(min[key]) ? item : min;
  }, null);
}

function buildDateRange(lastDays, todayKey) {
  const dates = [];
  for (let i = lastDays - 1; i >= 0; i--) {
    dates.push(shiftDateKey(todayKey, -i));
  }
  return dates;
}

function computeStreakFromDateSet(dateSet) {
  const dates = [...dateSet].sort();
  if (!dates.length) return 0;

  let streak = 1;
  for (let i = dates.length - 1; i > 0; i--) {
    const current = dates[i];
    const prev = dates[i - 1];
    const expectedPrev = shiftDateKey(current, -1);
    if (prev === expectedPrev) {
      streak++;
    } else {
      break;
    }
  }
  return streak;
}

async function getFoodLogs(userId) {
  const snapshot = await db
    .collection("users")
    .doc(userId)
    .collection("foodLogs")
    .get();

  return snapshot.docs.map((doc) => {
    const data = doc.data() || {};
    return {
      id: doc.id,
      timestamp: toNumber(data.timestamp),
      date: tsToDateKey(data.timestamp),
      calories: toNumber(data.calories),
      protein: toNumber(data.protein),
      carbs: toNumber(data.carbs),
      fats: toNumber(data.fats),
      foodName: pickFirstString(data, ["foodName", "name", "title"]),
    };
  }).filter((item) => item.date);
}

async function getWorkouts(userId) {
  const snapshot = await db
    .collection("users")
    .doc(userId)
    .collection("workouts")
    .get();

  return snapshot.docs.map((doc) => {
    const data = doc.data() || {};
    return {
      id: doc.id,
      timestamp: toNumber(data.timestamp),
      date: tsToDateKey(data.timestamp),
      caloriesBurned: toNumber(data.caloriesBurned),
      minutes: toNumber(data.minutes),
      name: pickFirstString(data, ["name", "title", "workoutName"]),
    };
  }).filter((item) => item.date);
}

function groupFoodLogsByDate(foodLogs) {
  const grouped = {};

  for (const log of foodLogs) {
    if (!grouped[log.date]) {
      grouped[log.date] = {
        date: log.date,
        calories: 0,
        protein: 0,
        carbs: 0,
        fats: 0,
      };
    }

    grouped[log.date].calories += log.calories;
    grouped[log.date].protein += log.protein;
    grouped[log.date].carbs += log.carbs;
    grouped[log.date].fats += log.fats;
  }

  return Object.values(grouped)
    .map((item) => ({
      ...item,
      calories: Number(item.calories.toFixed(2)),
      protein: Number(item.protein.toFixed(2)),
      carbs: Number(item.carbs.toFixed(2)),
      fats: Number(item.fats.toFixed(2)),
    }))
    .sort((a, b) => a.date.localeCompare(b.date));
}

function groupWorkoutsByDate(workouts) {
  const grouped = {};

  for (const workout of workouts) {
    if (!grouped[workout.date]) {
      grouped[workout.date] = {
        date: workout.date,
        burned: 0,
        workoutMinutes: 0,
      };
    }

    grouped[workout.date].burned += workout.caloriesBurned;
    grouped[workout.date].workoutMinutes += workout.minutes;
  }

  return Object.values(grouped)
    .map((item) => ({
      ...item,
      burned: Number(item.burned.toFixed(2)),
      workoutMinutes: Number(item.workoutMinutes.toFixed(2)),
    }))
    .sort((a, b) => a.date.localeCompare(b.date));
}

function mergeDailyRecords(foodDaily, workoutDaily) {
  const map = {};

  for (const item of foodDaily) {
    map[item.date] = {
      date: item.date,
      calories: item.calories,
      protein: item.protein,
      carbs: item.carbs,
      fats: item.fats,
      burned: 0,
      workoutMinutes: 0,
    };
  }

  for (const item of workoutDaily) {
    if (!map[item.date]) {
      map[item.date] = {
        date: item.date,
        calories: 0,
        protein: 0,
        carbs: 0,
        fats: 0,
        burned: 0,
        workoutMinutes: 0,
      };
    }

    map[item.date].burned = item.burned;
    map[item.date].workoutMinutes = item.workoutMinutes;
  }

  return Object.values(map).sort((a, b) => a.date.localeCompare(b.date));
}

function computeWeeklySummary(dailyRecords, todayKey) {
  const dates = buildDateRange(7, todayKey);
  const weekRecords = dates
    .map((date) => dailyRecords.find((r) => r.date === date))
    .filter(Boolean);

  return {
    recordedDays: weekRecords.length,
    averageCalories: averageFromTotalAndCount(sumOf(weekRecords, "calories"), weekRecords.length),
    averageProtein: averageFromTotalAndCount(sumOf(weekRecords, "protein"), weekRecords.length),
    averageCarbs: averageFromTotalAndCount(sumOf(weekRecords, "carbs"), weekRecords.length),
    averageFats: averageFromTotalAndCount(sumOf(weekRecords, "fats"), weekRecords.length),
    averageBurned: averageFromTotalAndCount(sumOf(weekRecords, "burned"), weekRecords.length),
    averageWorkoutMinutes: averageFromTotalAndCount(sumOf(weekRecords, "workoutMinutes"), weekRecords.length),
    totalCalories: sumOf(weekRecords, "calories"),
    totalProtein: sumOf(weekRecords, "protein"),
    totalCarbs: sumOf(weekRecords, "carbs"),
    totalFats: sumOf(weekRecords, "fats"),
    totalBurned: sumOf(weekRecords, "burned"),
    totalWorkoutMinutes: sumOf(weekRecords, "workoutMinutes"),
  };
}

function computeCoachInsights(dailyRecords, todayRecord, profile) {
  const goal = String(profile?.goal || "").toLowerCase();
  const calorieGoal = toNumber(profile?.calorieGoal);
  const proteinGoal = toNumber(profile?.proteinGoal);
  const carbsGoal = toNumber(profile?.carbsGoal);
  const fatsGoal = toNumber(profile?.fatsGoal);

  const avgCalories = averageFromTotalAndCount(
    sumOf(dailyRecords, "calories"),
    dailyRecords.filter((r) => r.calories > 0).length || dailyRecords.length
  );

  const avgProtein = averageFromTotalAndCount(
    sumOf(dailyRecords, "protein"),
    dailyRecords.filter((r) => r.protein > 0 || r.calories > 0).length || dailyRecords.length
  );

  const avgCarbs = averageFromTotalAndCount(
    sumOf(dailyRecords, "carbs"),
    dailyRecords.filter((r) => r.carbs > 0 || r.calories > 0).length || dailyRecords.length
  );

  const avgFats = averageFromTotalAndCount(
    sumOf(dailyRecords, "fats"),
    dailyRecords.filter((r) => r.fats > 0 || r.calories > 0).length || dailyRecords.length
  );

  const avgWorkoutMinutes = averageFromTotalAndCount(
    sumOf(dailyRecords, "workoutMinutes"),
    dailyRecords.filter((r) => r.workoutMinutes > 0 || r.burned > 0).length || dailyRecords.length
  );

  let mainFocus = "overall consistency";
  let likelyOutcome =
    "Your results will mostly depend on how consistently you hit your nutrition and activity targets.";
  let coachPerspective =
    "You already have enough data to improve. The best next step is focusing on the biggest repeated gap, not chasing random changes.";

  if (proteinGoal > 0 && avgProtein < proteinGoal) {
    mainFocus = "protein consistency";
    likelyOutcome =
      "If protein stays below target most days, recovery, satiety, and muscle retention may stay weaker than they should be.";
    coachPerspective =
      "Your pattern shows that protein is one of your clearest weak spots. Improving that usually raises meal quality fast.";
  }

  if (goal.includes("weight loss") || goal.includes("lose")) {
    if (calorieGoal > 0 && avgCalories > calorieGoal) {
      mainFocus = "calorie control";
      likelyOutcome =
        "If your average calories stay above target, fat loss will likely slow down or stall.";
      coachPerspective =
        "For weight loss, calorie control still drives the outcome most. Protein matters too, but staying over target too often can block progress.";
    } else if (proteinGoal > 0 && avgProtein < proteinGoal) {
      mainFocus = "protein while maintaining calorie control";
      likelyOutcome =
        "You may still lose weight if calories are controlled, but low protein can make it harder to preserve muscle and stay full.";
      coachPerspective =
        "Your calorie side looks more workable than your protein side, so your next upgrade should be protein consistency.";
    }
  }

  const todayProteinGap = proteinGoal > 0 ? Math.max(0, proteinGoal - toNumber(todayRecord?.protein)) : 0;
  const todayCaloriesLeft = calorieGoal > 0 ? calorieGoal - toNumber(todayRecord?.calories) : 0;

  return {
    mainFocus,
    likelyOutcome,
    coachPerspective,
    patternSummary: `Recent pattern: around ${avgCalories} calories, ${avgProtein}g protein, ${avgCarbs}g carbs, ${avgFats}g fats, and ${avgWorkoutMinutes} workout minutes on average.`,
    targetGaps: {
      proteinGap: Number((proteinGoal > 0 ? proteinGoal - avgProtein : 0).toFixed(2)),
      calorieGap: Number((calorieGoal > 0 ? avgCalories - calorieGoal : 0).toFixed(2)),
      carbsGap: Number((carbsGoal > 0 ? carbsGoal - avgCarbs : 0).toFixed(2)),
      fatsGap: Number((fatsGoal > 0 ? fatsGoal - avgFats : 0).toFixed(2)),
      todayProteinGap: Number(todayProteinGap.toFixed(2)),
      todayCaloriesLeft: Number(todayCaloriesLeft.toFixed(2)),
    },
  };
}

function computeAnalytics(foodLogs, workouts, foodDaily, workoutDaily, dailyRecords, todayKey, todayRecord, profile) {
  const distinctFoodDates = new Set(foodLogs.map((x) => x.date));
  const distinctWorkoutDates = new Set(workouts.map((x) => x.date));
  const activeDates = new Set([...distinctFoodDates, ...distinctWorkoutDates]);

  const totalFoodCalories = sumOf(foodLogs, "calories");
  const totalFoodProtein = sumOf(foodLogs, "protein");
  const totalFoodCarbs = sumOf(foodLogs, "carbs");
  const totalFoodFats = sumOf(foodLogs, "fats");

  const totalBurned = sumOf(workouts.map((w) => ({ burned: w.caloriesBurned })), "burned");
  const totalWorkoutMinutes = sumOf(workouts.map((w) => ({ workoutMinutes: w.minutes })), "workoutMinutes");

  const foodDateCount = distinctFoodDates.size || 0;
  const workoutDateCount = distinctWorkoutDates.size || 0;

  const maxFoodLogProtein = maxRecordBy(foodLogs, "protein");
  const maxFoodLogCalories = maxRecordBy(foodLogs, "calories");
  const minFoodLogCalories = minRecordBy(foodLogs, "calories");
  const maxWorkoutBurn = maxRecordBy(
    workouts.map((w) => ({ ...w, burned: w.caloriesBurned })),
    "burned"
  );
  const minWorkoutBurn = minRecordBy(
    workouts.map((w) => ({ ...w, burned: w.caloriesBurned })),
    "burned"
  );
  const maxWorkoutMinutes = maxRecordBy(
    workouts.map((w) => ({ ...w, workoutMinutes: w.minutes })),
    "workoutMinutes"
  );
  const minWorkoutMinutes = minRecordBy(
    workouts.map((w) => ({ ...w, workoutMinutes: w.minutes })),
    "workoutMinutes"
  );

  return {
    totalRecordedDays: dailyRecords.length,
    totalFoodDays: foodDateCount,
    totalWorkoutDays: workoutDateCount,
    currentStreak: computeStreakFromDateSet(activeDates),

    averages: {
      calories: averageFromTotalAndCount(totalFoodCalories, foodDateCount),
      protein: averageFromTotalAndCount(totalFoodProtein, foodDateCount),
      carbs: averageFromTotalAndCount(totalFoodCarbs, foodDateCount),
      fats: averageFromTotalAndCount(totalFoodFats, foodDateCount),
      burned: averageFromTotalAndCount(totalBurned, workoutDateCount),
      workoutMinutes: averageFromTotalAndCount(totalWorkoutMinutes, workoutDateCount),
    },

    totals: {
      calories: totalFoodCalories,
      protein: totalFoodProtein,
      carbs: totalFoodCarbs,
      fats: totalFoodFats,
      burned: totalBurned,
      workoutMinutes: totalWorkoutMinutes,
    },

    highs: {
      protein: maxFoodLogProtein
        ? { value: maxFoodLogProtein.protein, date: maxFoodLogProtein.date }
        : null,
      calories: maxFoodLogCalories
        ? { value: maxFoodLogCalories.calories, date: maxFoodLogCalories.date }
        : null,
      burned: maxWorkoutBurn
        ? { value: maxWorkoutBurn.burned, date: maxWorkoutBurn.date }
        : null,
      workoutMinutes: maxWorkoutMinutes
        ? { value: maxWorkoutMinutes.workoutMinutes, date: maxWorkoutMinutes.date }
        : null,
    },

    lows: {
      calories: minFoodLogCalories
        ? { value: minFoodLogCalories.calories, date: minFoodLogCalories.date }
        : null,
      burned: minWorkoutBurn
        ? { value: minWorkoutBurn.burned, date: minWorkoutBurn.date }
        : null,
      workoutMinutes: minWorkoutMinutes
        ? { value: minWorkoutMinutes.workoutMinutes, date: minWorkoutMinutes.date }
        : null,
    },

    weekly: computeWeeklySummary(dailyRecords, todayKey),

    recent7Days: buildDateRange(7, todayKey).map((date) => {
      const record = dailyRecords.find((r) => r.date === date);
      return {
        date,
        calories: record?.calories ?? 0,
        protein: record?.protein ?? 0,
        carbs: record?.carbs ?? 0,
        fats: record?.fats ?? 0,
        burned: record?.burned ?? 0,
        workoutMinutes: record?.workoutMinutes ?? 0,
      };
    }),

    coach: computeCoachInsights(dailyRecords, todayRecord, profile),
  };
}

async function getUserContext(userId, message) {
  const { dateKey: todayKey } = getManilaDateParts(new Date());
  const yesterdayKey = shiftDateKey(todayKey, -1);
  const requestedDate = parseDateFromMessage(message, todayKey);

  const result = {
    profile: null,
    today: null,
    yesterday: null,
    requestedDate: requestedDate || null,
    requestedRecord: null,
    analytics: null,
    allRecordsCount: 0,
    dataSource: "foodLogs+workouts+plan",
  };

  const userSnap = await db.collection("users").doc(userId).get();
  const user = userSnap.exists ? userSnap.data() || {} : {};
  const plan = user.plan || {};

  result.profile = {
    name: pickFirstString(user, ["name", "fullName", "username", "displayName"]),
    gender: pickFirstString(user, ["gender", "sex"]),
    age: user.age ?? null,
    weight: toNumber(user.weight),
    height: toNumber(user.height),
    goal: pickFirstString(user, ["goal"]),
    activityLevel: pickFirstString(user, ["activityLevel"]),
    lifestyle: pickFirstString(user, ["lifestyle"]),
    calorieGoal: toNumber(plan.calories),
    proteinGoal: toNumber(plan.protein),
    carbsGoal: toNumber(plan.carbs),
    fatsGoal: toNumber(plan.fats),
    workoutMinutesGoal: toNumber(plan.workoutMinutes),
    workoutDoneGoal: toNumber(plan.workoutDone),
  };

  const [foodLogs, workouts] = await Promise.all([
    getFoodLogs(userId),
    getWorkouts(userId),
  ]);

  const foodDaily = groupFoodLogsByDate(foodLogs);
  const workoutDaily = groupWorkoutsByDate(workouts);
  const dailyRecords = mergeDailyRecords(foodDaily, workoutDaily);

  result.allRecordsCount = dailyRecords.length;
  result.today = dailyRecords.find((r) => r.date === todayKey) || null;
  result.yesterday = dailyRecords.find((r) => r.date === yesterdayKey) || null;
  result.requestedRecord = requestedDate
    ? dailyRecords.find((r) => r.date === requestedDate) || null
    : null;

  result.analytics = computeAnalytics(
    foodLogs,
    workouts,
    foodDaily,
    workoutDaily,
    dailyRecords,
    todayKey,
    result.today,
    result.profile
  );

  return result;
}

function buildCarabuffPrompt(message, context = {}) {
  const profile = context.profile || {};
  const today = context.today || {};
  const yesterday = context.yesterday || {};
  const requestedDate = context.requestedDate || null;
  const requestedRecord = context.requestedRecord || null;
  const analytics = context.analytics || {};
  const averages = analytics.averages || {};
  const totals = analytics.totals || {};
  const highs = analytics.highs || {};
  const lows = analytics.lows || {};
  const weekly = analytics.weekly || {};
  const recent7Days = analytics.recent7Days || [];
  const coach = analytics.coach || {};
  const targetGaps = coach.targetGaps || {};

  const requestedDateBlock = requestedDate
    ? `
Requested date:
- requestedDate: ${requestedDate}
- recordFound: ${requestedRecord ? "yes" : "no"}

Requested date summary:
- calories: ${requestedRecord?.calories ?? "no data"}
- protein: ${requestedRecord?.protein ?? "no data"}
- carbs: ${requestedRecord?.carbs ?? "no data"}
- fats: ${requestedRecord?.fats ?? "no data"}
- burned: ${requestedRecord?.burned ?? "no data"}
- workoutMinutes: ${requestedRecord?.workoutMinutes ?? "no data"}
`
    : "";

  const recent7DaysBlock = recent7Days.length
    ? recent7Days.map(
        (r) =>
          `- ${r.date}: calories=${r.calories}, protein=${r.protein}, carbs=${r.carbs}, fats=${r.fats}, burned=${r.burned}, workoutMinutes=${r.workoutMinutes}`
      ).join("\n")
    : "- no data";

  return `
You are Carabuff, a smart fitness coach inside a mobile fitness app.

You only answer questions related to:
- fitness
- workouts
- exercise
- food
- nutrition
- calories
- macros
- hydration
- sleep
- recovery
- healthy habits
- motivation
- app-related guidance for Carabuff

Style rules:
- Reply in simple English or light Taglish
- Sound like a real coach, not just a data reader
- Be supportive, clear, and insightful
- Use short paragraphs or bullet points when helpful
- Never cut off mid-sentence
- Address the user by name if available
- Use the actual data below only
- Never guess missing values
- If a date has no record, clearly say no record was found
- If the user asks about today, use Today's progress
- If the user asks about yesterday, use Yesterday progress
- If the user asks about a specific date, use Requested date summary
- If the user asks about dashboard, highest, average, streak, weekly, trend, or overall progress, use Analytics summary
- If the user asks where to focus, compare actual progress against goals and identify the biggest priority
- Explain the likely outcome if the current pattern continues when useful
- Give practical coaching, not just raw numbers
- Do not invent data, goals, or history
- Do not give dangerous, extreme, or medical diagnosis advice
- If the topic is outside fitness, nutrition, health, or Carabuff app help, politely redirect

System context:
- dataSourceUsed: ${context.dataSource ?? "unknown"}
- totalMergedDailyRecords: ${context.allRecordsCount ?? 0}

User profile:
- name: ${profile.name ?? "no data"}
- gender: ${profile.gender ?? "no data"}
- age: ${profile.age ?? "no data"}
- weight: ${profile.weight ?? "no data"}
- height: ${profile.height ?? "no data"}
- goal: ${profile.goal ?? "no data"}
- activityLevel: ${profile.activityLevel ?? "no data"}
- lifestyle: ${profile.lifestyle ?? "no data"}
- calorieGoal: ${profile.calorieGoal ?? 0}
- proteinGoal: ${profile.proteinGoal ?? 0}
- carbsGoal: ${profile.carbsGoal ?? 0}
- fatsGoal: ${profile.fatsGoal ?? 0}
- workoutMinutesGoal: ${profile.workoutMinutesGoal ?? 0}
- workoutDoneGoal: ${profile.workoutDoneGoal ?? 0}

Today's progress:
- caloriesToday: ${today.calories ?? 0}
- proteinToday: ${today.protein ?? 0}
- carbsToday: ${today.carbs ?? 0}
- fatsToday: ${today.fats ?? 0}
- burnedToday: ${today.burned ?? 0}
- workoutMinutesToday: ${today.workoutMinutes ?? 0}

Yesterday progress:
- calories: ${yesterday.calories ?? 0}
- protein: ${yesterday.protein ?? 0}
- carbs: ${yesterday.carbs ?? 0}
- fats: ${yesterday.fats ?? 0}
- burned: ${yesterday.burned ?? 0}
- workoutMinutes: ${yesterday.workoutMinutes ?? 0}

${requestedDateBlock}

Analytics summary:
- totalRecordedDays: ${analytics.totalRecordedDays ?? 0}
- totalFoodDays: ${analytics.totalFoodDays ?? 0}
- totalWorkoutDays: ${analytics.totalWorkoutDays ?? 0}
- currentStreak: ${analytics.currentStreak ?? 0}

Average dashboard values:
- averageCalories: ${averages.calories ?? 0}
- averageProtein: ${averages.protein ?? 0}
- averageCarbs: ${averages.carbs ?? 0}
- averageFats: ${averages.fats ?? 0}
- averageBurned: ${averages.burned ?? 0}
- averageWorkoutMinutes: ${averages.workoutMinutes ?? 0}

Totals:
- totalCalories: ${totals.calories ?? 0}
- totalProtein: ${totals.protein ?? 0}
- totalCarbs: ${totals.carbs ?? 0}
- totalFats: ${totals.fats ?? 0}
- totalBurned: ${totals.burned ?? 0}
- totalWorkoutMinutes: ${totals.workoutMinutes ?? 0}

Highest dashboard values:
- highestProtein: ${highs.protein?.value ?? "no data"}
- highestProteinDate: ${highs.protein?.date ?? "no data"}
- highestCalories: ${highs.calories?.value ?? "no data"}
- highestCaloriesDate: ${highs.calories?.date ?? "no data"}
- highestBurned: ${highs.burned?.value ?? "no data"}
- highestBurnedDate: ${highs.burned?.date ?? "no data"}
- highestWorkoutMinutes: ${highs.workoutMinutes?.value ?? "no data"}
- highestWorkoutMinutesDate: ${highs.workoutMinutes?.date ?? "no data"}

Lowest dashboard values:
- lowestCalories: ${lows.calories?.value ?? "no data"}
- lowestCaloriesDate: ${lows.calories?.date ?? "no data"}
- lowestBurned: ${lows.burned?.value ?? "no data"}
- lowestBurnedDate: ${lows.burned?.date ?? "no data"}
- lowestWorkoutMinutes: ${lows.workoutMinutes?.value ?? "no data"}
- lowestWorkoutMinutesDate: ${lows.workoutMinutes?.date ?? "no data"}

This week's summary:
- weeklyRecordedDays: ${weekly.recordedDays ?? 0}
- weeklyAverageCalories: ${weekly.averageCalories ?? 0}
- weeklyAverageProtein: ${weekly.averageProtein ?? 0}
- weeklyAverageCarbs: ${weekly.averageCarbs ?? 0}
- weeklyAverageFats: ${weekly.averageFats ?? 0}
- weeklyAverageBurned: ${weekly.averageBurned ?? 0}
- weeklyAverageWorkoutMinutes: ${weekly.averageWorkoutMinutes ?? 0}
- weeklyTotalCalories: ${weekly.totalCalories ?? 0}
- weeklyTotalProtein: ${weekly.totalProtein ?? 0}
- weeklyTotalCarbs: ${weekly.totalCarbs ?? 0}
- weeklyTotalFats: ${weekly.totalFats ?? 0}
- weeklyTotalBurned: ${weekly.totalBurned ?? 0}
- weeklyTotalWorkoutMinutes: ${weekly.totalWorkoutMinutes ?? 0}

Coach insights:
- mainFocus: ${coach.mainFocus ?? "no data"}
- likelyOutcome: ${coach.likelyOutcome ?? "no data"}
- coachPerspective: ${coach.coachPerspective ?? "no data"}
- patternSummary: ${coach.patternSummary ?? "no data"}
- averageProteinGapVsGoal: ${targetGaps.proteinGap ?? 0}
- averageCalorieGapVsGoal: ${targetGaps.calorieGap ?? 0}
- averageCarbsGapVsGoal: ${targetGaps.carbsGap ?? 0}
- averageFatsGapVsGoal: ${targetGaps.fatsGap ?? 0}
- todayProteinGapVsGoal: ${targetGaps.todayProteinGap ?? 0}
- todayCaloriesLeftVsGoal: ${targetGaps.todayCaloriesLeft ?? 0}

Recent 7-day records:
${recent7DaysBlock}

User message:
${message}
`.trim();
}

function extractGeminiReply(data) {
  const candidate = data?.candidates?.[0];
  if (!candidate) return null;

  const parts = candidate?.content?.parts || [];
  const text = parts.map((part) => part?.text || "").join("").trim();
  return text || null;
}

exports.askCarabuff = onRequest(
  {
    cors: true,
    region: "asia-southeast1",
  },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        return res.status(405).json({ error: "Method not allowed" });
      }

      const { message, userId } = req.body || {};

      if (!message || !String(message).trim()) {
        return res.status(400).json({ error: "Message is required" });
      }

      if (!userId || !String(userId).trim()) {
        return res.status(400).json({ error: "userId is required" });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(500).json({
          error: "Missing GEMINI_API_KEY in environment variables",
        });
      }

      const context = await getUserContext(
        String(userId).trim(),
        String(message).trim()
      );

      const input = buildCarabuffPrompt(String(message).trim(), context);

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [
              {
                role: "user",
                parts: [{ text: input }],
              },
            ],
            generationConfig: {
              temperature: 0.7,
              topP: 0.9,
              topK: 40,
              maxOutputTokens: 1400,
            },
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        console.error("Gemini error:", JSON.stringify(data, null, 2));
        return res.status(response.status).json({
          error: data?.error?.message || "Gemini request failed",
        });
      }

      const reply = extractGeminiReply(data);

      if (!reply) {
        console.error("Gemini empty reply:", JSON.stringify(data, null, 2));
        return res.status(200).json({
          reply: "Sorry, I couldn’t generate a complete reply right now. Please try again.",
        });
      }

      return res.status(200).json({ reply });
    } catch (error) {
      console.error("askCarabuff error:", error);
      return res.status(500).json({
        error: error.message || "Internal server error",
      });
    }
  }
);

exports.sendCarabuffReminders = onSchedule(
  {
    schedule: "every 1 hours",
    timeZone: "Asia/Manila",
    region: "asia-southeast1",
  },
  async () => {
    const users = await db.collection("users").get();
    const { dateKey: today, hour } = getManilaDateParts(new Date());

    for (const doc of users.docs) {
      const user = doc.data();
      const uid = doc.id;
      const token = user.fcmToken || null;

      const workoutReminderEnabled = user.workoutReminderEnabled !== false;
      const mealReminderEnabled = user.mealReminderEnabled !== false;

      let title = null;
      let message = null;
      let type = null;
      let allowPhonePush = false;
      let updateField = null;

      if (hour >= 6 && hour < 10) {
        if (user.lastMorning === today) continue;
        title = "Good morning ☀️";
        message = "Start your progress today!";
        type = "workout";
        allowPhonePush = workoutReminderEnabled;
        updateField = "lastMorning";
      } else if (hour >= 12 && hour < 14) {
        if (user.lastMeal === today) continue;
        title = "Lunch reminder 🍛";
        message = "Don't forget to log your meal!";
        type = "meal";
        allowPhonePush = mealReminderEnabled;
        updateField = "lastMeal";
      } else if (hour >= 18 && hour < 21) {
        if (user.lastEvening === today) continue;
        title = "Evening check 🌙";
        message = "Review your progress today!";
        type = "workout";
        allowPhonePush = workoutReminderEnabled;
        updateField = "lastEvening";
      }

      if (!title || !message || !type || !updateField) continue;

      try {
        await db.collection("notifications").add({
          userId: uid,
          title,
          message,
          type,
          target: "notifications",
          isRead: false,
          timestamp: Date.now(),
        });

        if (allowPhonePush && token) {
          await admin.messaging().send({
            token,
            notification: { title, body: message },
            data: { type, target: "notifications" },
            android: { priority: "high" },
          });
        }

        await db.collection("users").doc(uid).set(
          { [updateField]: today },
          { merge: true }
        );

        console.log(
          `Reminder processed for ${uid} | type=${type} | push=${allowPhonePush} | date=${today}`
        );
      } catch (error) {
        console.error(`Error processing reminder for ${uid}:`, error);
      }
    }
  }
);

exports.sendYesterdaySummary = onSchedule(
  {
    schedule: "0 7 * * *",
    timeZone: "Asia/Manila",
    region: "asia-southeast1",
  },
  async () => {
    const users = await db.collection("users").get();

    const { manilaDate } = getManilaDateParts(new Date());
    const yesterdayManila = new Date(manilaDate);
    yesterdayManila.setDate(yesterdayManila.getDate() - 1);
    const { dateKey: summaryDate } = getManilaDateParts(yesterdayManila);

    for (const doc of users.docs) {
      const user = doc.data();
      const uid = doc.id;
      const token = user.fcmToken || null;

      if (user.lastSummaryDate === summaryDate) continue;

      const title = "Yesterday Summary 📊";
      const message = `Hello! This is the summary of your progress for ${summaryDate}.`;

      try {
        await db.collection("notifications").add({
          userId: uid,
          title,
          message,
          type: "daily_summary",
          target: "daily_summary",
          summaryDate,
          isRead: false,
          timestamp: Date.now(),
        });

        if (token) {
          await admin.messaging().send({
            token,
            notification: { title, body: message },
            data: {
              type: "daily_summary",
              target: "daily_summary",
              summaryDate,
            },
            android: { priority: "high" },
          });
        }

        await db.collection("users").doc(uid).set(
          { lastSummaryDate: summaryDate },
          { merge: true }
        );

        console.log(`Yesterday summary sent to ${uid} for ${summaryDate}`);
      } catch (error) {
        console.error(`Error sending yesterday summary to ${uid}:`, error);
      }
    }
  }
);