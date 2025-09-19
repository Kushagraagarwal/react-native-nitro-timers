import UIKit
import Foundation

class HybridNitroTimer : HybridNitroTimerSpec {

    // UIView with UILabel for timer display
    var view: UIView = UIView()
    private let timerLabel = UILabel()

    // Timer properties
    private var endDate: Date?
    private var isRunning = false
    private var isPaused = false
    private var wasExpired = false
    private var wasCritical = false
    private let dateFormatter = ISO8601DateFormatter()

    // Props
    var endTime: String = "" {
        didSet {
            setEndTimeFromString(endTime)
        }
    }

    var autoStart: Bool = true {
        didSet {
            if autoStart && !isRunning {
                start()
            }
        }
    }

    var criticalThreshold: Double = 300.0 // 5 minutes default

    // Display format
    var format: String = "HH:MM:SS"
    var showDays: Bool = true
    var hideZeroHours: Bool = false

    // Styling
    var fontSize: Double = 16.0 {
        didSet {
            updateLabelStyle()
        }
    }

    var textColor: String = "#000000" {
        didSet {
            updateLabelStyle()
        }
    }

    var criticalColor: String = "#FF0000"

    var fontWeight: String = "normal" {
        didSet {
            updateLabelStyle()
        }
    }

    var textAlign: String = "left" {
        didSet {
            updateLabelStyle()
        }
    }

    var backgroundColor: String? = nil {
        didSet {
            updateViewStyle()
        }
    }

    var cornerRadius: Double? = nil {
        didSet {
            updateViewStyle()
        }
    }

    var padding: Double? = nil {
        didSet {
            updateLabelConstraints()
        }
    }

    // Behavior
    var pauseWhenNotVisible: Bool = false
    var continueInBackground: Bool = true

    // Events
    var onExpired: (() -> Void)?
    var onCritical: (() -> Void)?

    override init() {
        super.init()
        setupTimerLabel()
        TimerManager.shared.addTimer(self)
    }

    deinit {
        TimerManager.shared.removeTimer(self)
    }

    private func setupTimerLabel() {
        view.addSubview(timerLabel)
        timerLabel.translatesAutoresizingMaskIntoConstraints = false
        updateLabelConstraints()
        updateLabelStyle()
        updateViewStyle()

        timerLabel.text = formatTime(days: 0, hours: 0, minutes: 0, seconds: 0)
    }

    private func updateLabelConstraints() {
        timerLabel.removeFromSuperview()
        view.addSubview(timerLabel)
        timerLabel.translatesAutoresizingMaskIntoConstraints = false

        let paddingValue = padding ?? 0.0
        NSLayoutConstraint.activate([
            timerLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: paddingValue),
            timerLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: paddingValue),
            timerLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -paddingValue),
            timerLabel.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -paddingValue)
        ])
    }

    private func updateLabelStyle() {
        timerLabel.font = UIFont.systemFont(ofSize: fontSize, weight: fontWeight == "bold" ? .bold : .regular)
        timerLabel.textColor = hexStringToUIColor(hexColor: textColor)

        switch textAlign {
        case "center":
            timerLabel.textAlignment = .center
        case "right":
            timerLabel.textAlignment = .right
        default:
            timerLabel.textAlignment = .left
        }
    }

    private func updateViewStyle() {
        if let bgColor = backgroundColor {
            view.backgroundColor = hexStringToUIColor(hexColor: bgColor)
        }

        if let radius = cornerRadius {
            view.layer.cornerRadius = radius
            view.layer.masksToBounds = true
        }
    }

    private func setEndTimeFromString(_ timeString: String) {
        endDate = dateFormatter.date(from: timeString)
        updateDisplay()
    }

    func updateDisplay() {
        guard let endDate = endDate else {
            timerLabel.text = formatTime(days: 0, hours: 0, minutes: 0, seconds: 0)
            return
        }

        let now = Date()
        let timeInterval = endDate.timeIntervalSince(now)

        if timeInterval <= 0 {
            // Timer expired
            timerLabel.text = formatTime(days: 0, hours: 0, minutes: 0, seconds: 0)
            if !wasExpired {
                wasExpired = true
                onExpired?()
            }
            return
        }

        wasExpired = false

        // Check critical threshold
        let isCritical = timeInterval <= criticalThreshold
        if isCritical && !wasCritical {
            wasCritical = true
            onCritical?()
        } else if !isCritical {
            wasCritical = false
        }

        // Update text color based on critical state
        timerLabel.textColor = isCritical ? hexStringToUIColor(hexColor: criticalColor) : hexStringToUIColor(hexColor: textColor)

        // Calculate time components
        let totalSeconds = Int(timeInterval)
        let days = totalSeconds / 86400
        let hours = (totalSeconds % 86400) / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        timerLabel.text = formatTime(days: days, hours: hours, minutes: minutes, seconds: seconds)
    }

    private func formatTime(days: Int, hours: Int, minutes: Int, seconds: Int) -> String {
        switch format {
        case "HH:MM:SS":
            if showDays && days > 0 {
                return String(format: "%d:%02d:%02d:%02d", days, hours, minutes, seconds)
            } else if hideZeroHours && hours == 0 {
                return String(format: "%02d:%02d", minutes, seconds)
            } else {
                return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
            }
        case "H:M:S":
            if showDays && days > 0 {
                return String(format: "%d:%d:%d:%d", days, hours, minutes, seconds)
            } else if hideZeroHours && hours == 0 {
                return String(format: "%d:%d", minutes, seconds)
            } else {
                return String(format: "%d:%d:%d", hours, minutes, seconds)
            }
        case "Hh Mm Ss":
            if showDays && days > 0 {
                return String(format: "%dd %dh %dm %ds", days, hours, minutes, seconds)
            } else if hideZeroHours && hours == 0 {
                return String(format: "%dm %ds", minutes, seconds)
            } else {
                return String(format: "%dh %dm %ds", hours, minutes, seconds)
            }
        case "H hours M minutes":
            if showDays && days > 0 {
                return String(format: "%d days %d hours %d minutes", days, hours, minutes)
            } else if hideZeroHours && hours == 0 {
                return String(format: "%d minutes", minutes)
            } else {
                return String(format: "%d hours %d minutes", hours, minutes)
            }
        default:
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    // MARK: - Timer Methods

    func start() {
        isRunning = true
        isPaused = false
        TimerManager.shared.startTimer()
    }

    func pause() {
        isPaused = true
    }

    func resume() {
        isPaused = false
    }

    func reset() {
        wasExpired = false
        wasCritical = false
        updateDisplay()
    }

    func addSeconds(_ seconds: Double) {
        guard let currentEndDate = endDate else { return }
        endDate = currentEndDate.addingTimeInterval(seconds)
        updateDisplay()
    }

    func setEndTime(_ endTime: String) {
        setEndTimeFromString(endTime)
    }

    func getCurrentTime() -> [String: Any] {
        guard let endDate = endDate else {
            return [
                "days": 0,
                "hours": 0,
                "minutes": 0,
                "seconds": 0,
                "totalSeconds": 0,
                "isExpired": true,
                "isCritical": false
            ]
        }

        let now = Date()
        let timeInterval = endDate.timeIntervalSince(now)

        if timeInterval <= 0 {
            return [
                "days": 0,
                "hours": 0,
                "minutes": 0,
                "seconds": 0,
                "totalSeconds": 0,
                "isExpired": true,
                "isCritical": false
            ]
        }

        let totalSeconds = Int(timeInterval)
        let days = totalSeconds / 86400
        let hours = (totalSeconds % 86400) / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        return [
            "days": days,
            "hours": hours,
            "minutes": minutes,
            "seconds": seconds,
            "totalSeconds": totalSeconds,
            "isExpired": false,
            "isCritical": timeInterval <= criticalThreshold
        ]
    }

    private func hexStringToUIColor(hexColor: String) -> UIColor {
        let stringScanner = Scanner(string: hexColor)

        if(hexColor.hasPrefix("#")) {
            stringScanner.scanLocation = 1
        }
        var color: UInt32 = 0
        stringScanner.scanHexInt32(&color)

        let r = CGFloat(Int(color >> 16) & 0x000000FF)
        let g = CGFloat(Int(color >> 8) & 0x000000FF)
        let b = CGFloat(Int(color) & 0x000000FF)

        return UIColor(red: r / 255.0, green: g / 255.0, blue: b / 255.0, alpha: 1)
    }
}

// MARK: - Timer Manager Singleton

class TimerManager {
    static let shared = TimerManager()

    private var displayLink: CADisplayLink?
    private var timers: [HybridNitroTimer] = []
    private var isAppInBackground = false

    private init() {
        setupAppStateNotifications()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func setupAppStateNotifications() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }

    @objc private func appDidEnterBackground() {
        isAppInBackground = true
        // Keep running timers in background if continueInBackground is true
        for timer in timers {
            if !timer.continueInBackground {
                timer.pause()
            }
        }
    }

    @objc private func appWillEnterForeground() {
        isAppInBackground = false
        // Resume timers that were paused due to backgrounding
        for timer in timers {
            if !timer.continueInBackground && timer.autoStart {
                timer.resume()
            }
        }
    }

    @objc private func appDidBecomeActive() {
        // Force update all timers when app becomes active
        // This ensures accuracy after backgrounding
        DispatchQueue.main.async {
            for timer in self.timers {
                timer.updateDisplay()
            }
        }
    }

    func addTimer(_ timer: HybridNitroTimer) {
        timers.append(timer)
    }

    func removeTimer(_ timer: HybridNitroTimer) {
        timers.removeAll { $0 === timer }
        if timers.isEmpty {
            stopDisplayLink()
        }
    }

    func startTimer() {
        if displayLink == nil {
            setupDisplayLink()
        }
    }

    private func setupDisplayLink() {
        displayLink = CADisplayLink(target: self, selector: #selector(updateTimers))
        displayLink?.add(to: .main, forMode: .common)
    }

    private func stopDisplayLink() {
        displayLink?.invalidate()
        displayLink = nil
    }

    @objc private func updateTimers() {
        for timer in timers {
            let shouldUpdate = timer.isRunning && !timer.isPaused

            // Handle visibility-based pausing
            if timer.pauseWhenNotVisible {
                // In a real implementation, you'd check if the timer's view is visible
                // For now, we'll assume timers are visible when app is in foreground
                let isVisible = !isAppInBackground
                if !isVisible && shouldUpdate {
                    continue // Skip update for invisible timers
                }
            }

            if shouldUpdate {
                DispatchQueue.main.async {
                    timer.updateDisplay()
                }
            }
        }
    }
}
