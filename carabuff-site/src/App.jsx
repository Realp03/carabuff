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
          Track calories, workouts, and progress — all in one app.
        </motion.p>

        <motion.button
          className="btn"
          onClick={handleDownload}
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.95 }}
        >
          📥 Download for Android
        </motion.button>

        <p className="small-note">Free • No ads • Easy to use</p>
      </section>

      {/* FEATURES */}
      <section className="features">
        {[
          "🔥 Track Calories",
          "🏋️ Log Workouts",
          "📊 Monitor Progress",
          "🔔 Smart Notifications",
          "⚡ Fast Performance",
          "🎯 Goal Tracking",
          "📅 Daily Planner",
          "💡 Fitness Tips",
        ].map((text, index) => (
          <motion.div
            className="card"
            key={index}
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <h3>{text}</h3>
            <p>Powerful feature to boost your fitness journey.</p>
          </motion.div>
        ))}
      </section>

      {/* STATS SECTION 🔥 */}
      <section className="stats">
        <div>
          <h2>🔥 1K+</h2>
          <p>Downloads</p>
        </div>
        <div>
          <h2>⭐ 4.8</h2>
          <p>User Rating</p>
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