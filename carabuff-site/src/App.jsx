import "./App.css";
import logo from "./assets/carabuff_logo.png";
import { motion } from "framer-motion";

function App() {
  const apkLink =
    "https://github.com/Realp03/carabuff/releases/download/v1.0/Carabuff.apk";

  const handleDownload = () => {
    const isAndroid = /Android/i.test(navigator.userAgent);

    if (isAndroid) {
      window.location.href = apkLink;
    } else {
      alert("📱 Open this on Android to install the app.");
    }
  };

  const features = [
    {
      title: "🤖 AI Fitness Coach",
      desc: "Chat with Carabuff for personalized tips about calories, protein, workouts, and progress.",
    },
    {
      title: "🔥 Calorie Tracking",
      desc: "Log your meals and monitor your daily calorie intake with ease.",
    },
    {
      title: "🍗 Macro Monitoring",
      desc: "Track protein, carbs, and fats to stay aligned with your nutrition goals.",
    },
    {
      title: "🏋️ Workout Logging",
      desc: "Record workout duration and calories burned to measure your activity level.",
    },
    {
      title: "📊 Smart Analytics",
      desc: "View averages, highest records, streaks, weekly trends, and overall progress insights.",
    },
    {
      title: "🎯 Goal-Based Dashboard",
      desc: "See your calorie, macro, and workout progress compared to your personal targets.",
    },
    {
      title: "🔔 Smart Notifications",
      desc: "Get reminders for meals, workouts, and daily progress check-ins.",
    },
    {
      title: "📅 Daily Progress Summary",
      desc: "Review your daily performance and stay aware of where you need to improve.",
    },
    {
      title: "⚡ Fast and Simple UI",
      desc: "Clean design focused on quick logging, fast access, and easy tracking.",
    },
  ];

  const updates = [
    "Added AI chatbot with personalized fitness coaching",
    "Connected AI to user goals and progress data",
    "Added calorie, macro, and workout tracking system",
    "Added dashboard with daily and weekly analytics",
    "Added highest record and average progress insights",
    "Added smart reminders and in-app notifications",
    "Added daily summary support for user progress review",
    "Improved goal tracking for protein, carbs, fats, and calories",
  ];

  return (
    <div className="main">
      {/* HERO */}
      <section className="hero">
        <motion.img
          src={logo}
          alt="Carabuff Logo"
          className="logo"
          initial={{ opacity: 0, y: -40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
        />

        <motion.h1
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
        >
          Carabuff 💪
        </motion.h1>

        <motion.p
          className="subtitle"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
        >
          Your smart fitness companion for tracking food, workouts, analytics,
          and real progress.
        </motion.p>

        <motion.button
          className="btn"
          onClick={handleDownload}
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.95 }}
        >
          📥 Download for Android
        </motion.button>

        <p className="small-note">Free • No ads • Built for fitness tracking</p>
      </section>

      {/* FEATURES */}
      <section className="features">
        {features.map((item, index) => (
          <motion.div
            className="card"
            key={index}
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.08 }}
            viewport={{ once: true }}
          >
            <h3>{item.title}</h3>
            <p>{item.desc}</p>
          </motion.div>
        ))}
      </section>

      {/* WHAT'S NEW */}
      <section className="features">
        {updates.map((text, index) => (
          <motion.div
            className="card"
            key={`update-${index}`}
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.06 }}
            viewport={{ once: true }}
          >
            <h3>✨ Update {index + 1}</h3>
            <p>{text}</p>
          </motion.div>
        ))}
      </section>

      {/* STATS */}
      <section className="stats">
        <div>
          <h2>🤖 AI</h2>
          <p>Coach Assistant</p>
        </div>
        <div>
          <h2>📊 Smart</h2>
          <p>Progress Analytics</p>
        </div>
        <div>
          <h2>💪 100%</h2>
          <p>Free App</p>
        </div>
      </section>

      {/* QR */}
      <section className="qr">
        <h2>Scan to Install</h2>

        <img
          src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${apkLink}`}
          alt="QR Code"
        />

        <p className="small-note">Scan using your Android phone</p>
      </section>

      <footer>
        <p>© 2026 Carabuff • Built for fitness 💪</p>
      </footer>
    </div>
  );
}

export default App;
