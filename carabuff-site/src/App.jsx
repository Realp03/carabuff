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
      desc: "Chat with Carabuff for personalized advice about calories, protein, workouts, and daily progress.",
    },
    {
      title: "🔥 Calorie Tracking",
      desc: "Log meals quickly and stay aware of your daily calorie intake.",
    },
    {
      title: "🍗 Macro Monitoring",
      desc: "Track protein, carbs, and fats to stay aligned with your nutrition goals.",
    },
    {
      title: "🏋️ Workout Logging",
      desc: "Save workouts, monitor duration, and estimate calories burned.",
    },
    {
      title: "📊 Smart Analytics",
      desc: "View trends, averages, records, and progress insights in one dashboard.",
    },
    {
      title: "🎯 Goal Dashboard",
      desc: "Compare your current calories, macros, and activity against your personal targets.",
    },
    {
      title: "🔔 Smart Notifications",
      desc: "Receive reminders for meals, workouts, and daily check-ins.",
    },
    {
      title: "📅 Daily Summary",
      desc: "Review your performance each day and spot where you need improvement.",
    },
    {
      title: "⚡ Fast and Clean UI",
      desc: "Built for quick logging, easy navigation, and a smooth user experience.",
    },
  ];

  const highlights = [
    { value: "AI", label: "Coach Assistant" },
    { value: "Smart", label: "Progress Analytics" },
    { value: "100%", label: "Free to Use" },
  ];

  return (
    <div className="main">
      <div className="bg-orb orb-1" />
      <div className="bg-orb orb-2" />
      <div className="bg-grid" />

      <section className="hero">
        <motion.div
          className="hero-badge"
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          Carabuff Mobile Fitness App
        </motion.div>

        <motion.div
          className="logo-shell"
          initial={{ opacity: 0, y: -35, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.8 }}
        >
          <img src={logo} alt="Carabuff Logo" className="logo" />
        </motion.div>

        <motion.h1
          className="hero-title"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15, duration: 0.6 }}
        >
          Carabuff 💪
        </motion.h1>

        <motion.p
          className="subtitle"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6 }}
        >
          Your smart fitness companion for tracking food, workouts, analytics,
          and real progress.
        </motion.p>

        <motion.div
          className="hero-actions"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45, duration: 0.6 }}
        >
          <motion.button
            className="btn primary-btn"
            onClick={handleDownload}
            whileHover={{ scale: 1.04 }}
            whileTap={{ scale: 0.97 }}
          >
            📥 Download for Android
          </motion.button>

          <a
            className="btn ghost-btn"
            href={apkLink}
            target="_blank"
            rel="noreferrer"
          >
            🔗 View APK Link
          </a>
        </motion.div>

        <motion.div
          className="hero-note-box"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.6 }}
        >
          <span>Free</span>
          <span>No ads</span>
          <span>Built for fitness tracking</span>
        </motion.div>
      </section>

      <section className="preview-strip">
        <div className="preview-card">
          <p className="preview-label">Why Carabuff?</p>
          <h2>Inspired by Filipino strength and discipline</h2>
          <p>
            The name <strong>Carabuff</strong> comes from two powerful ideas:
            <strong> Cara</strong>, inspired by the <strong>carabao</strong>,
            the national animal of the Philippines, and <strong>Buff</strong>,
            which represents strength, powerfull, and becoming a better version
            of yourself. Ang carabao ay simbolo ng pagiging
            <strong> matatag, masipag, malakas, at may disiplina</strong> —
            mga katangiang mahalaga rin sa bawat fitness journey. Kaya ang
            Carabuff ay hindi lang tungkol sa muscles o workouts, kundi tungkol
            din sa consistency, discipline, at self-improvement para mas maging
            better version ka ng sarili mo.
          </p>
        </div>
      </section>

      <section className="section-head">
        <p className="section-tag">Core Features</p>
        <h2>Everything you need in one fitness app</h2>
      </section>

      <section className="features">
        {features.map((item, index) => (
          <motion.div
            className="card"
            key={index}
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.06, duration: 0.45 }}
            viewport={{ once: true }}
          >
            <h3>{item.title}</h3>
            <p>{item.desc}</p>
          </motion.div>
        ))}
      </section>

      <section className="section-head stats-head">
        <p className="section-tag">Highlights</p>
        <h2>Simple, smart, and made for progress</h2>
      </section>

      <section className="stats">
        {highlights.map((item, index) => (
          <motion.div
            className="stat-card"
            key={index}
            initial={{ opacity: 0, y: 25 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1, duration: 0.45 }}
            viewport={{ once: true }}
          >
            <h2>{item.value}</h2>
            <p>{item.label}</p>
          </motion.div>
        ))}
      </section>

      <section className="qr">
        <div className="qr-card">
          <p className="section-tag">Quick Install</p>
          <h2>Scan to install on Android</h2>

          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${apkLink}`}
            alt="QR Code"
          />

          <p className="small-note">Scan using your Android phone</p>
        </div>
      </section>

      <footer>
        <p>© 2026 Carabuff • Built for fitness 💪</p>
      </footer>
    </div>
  );
}

export default App;
