NFC Auto Switch

NFC Auto Switch is an Android utility application designed to solve the conflict between Google Wallet (Google Pay) and other local NFC payment or access control apps (such as EasyCard, OPPO Access Card, or banking apps).

On many Android devices, the system's "Default Payment App" setting does not always switch automatically when you open a different app, leading to failed transactions or access attempts. This app uses the Android AccessibilityService to detect the foreground application and instantly changes the NFC routing configuration to match your needs.

🚀 Key Features

Automatic Switching: Instantly changes the default NFC payment service when a configured app comes to the foreground.

Screen-Off "Reset": Automatically switches to a specific card (e.g., your Transit Card or Door Access Card) when the screen turns off, ensuring you can tap-and-go without unlocking your phone.

Non-Root Solution: Works on unrooted devices by utilizing the WRITE_SECURE_SETTINGS permission via ADB.

Smart Scanning: Automatically detects installed apps with NFC capabilities (HCE & Off-Host services).

Modern UI: Clean, Material Design 3 interface with dark mode support.

Force Refresh Mode: An advanced option for devices that require a routing table reset to apply changes.

🛠 Prerequisites

Since this app modifies system secure settings, you must grant it specific permissions using a computer via ADB (Android Debug Bridge).

1. Enable Developer Options

Go to Settings > About Phone and tap Build Number 7 times.

Go to System > Developer Options.

Enable USB Debugging.

(For Xiaomi/Redmi/POCO): Enable USB Debugging (Security Settings).

(For OPPO/Realme): Enable Disable Permission Monitoring.

2. Grant Permission via ADB

Connect your phone to your PC/Mac and run the following command in your terminal/command prompt:

adb shell pm grant com.marineking.nfcautoswitch android.permission.WRITE_SECURE_SETTINGS


📱 How to Use

Grant Permission: Ensure the ADB command above is executed successfully.

Enable Service: Open the app and grant Accessibility Service permission when prompted.

Configure Rules:

The app lists all detected NFC-capable apps.

Toggle the switch next to an app (e.g., Google Wallet) to enable auto-switching for it.

Screen Off Behavior:

Tap "Screen Off Behavior" in the dashboard.

Select the card you use most frequently (e.g., Access Card) to be the default when the phone is locked.

🔧 Tech Stack

Language: Kotlin

Core API: AccessibilityService, Settings.Secure, PackageManager

UI: XML Layouts, Material Components (MDC) for Android

Architecture: Event-driven (BroadcastReceiver & Accessibility Events)

⚠️ Disclaimer

This tool allows you to modify system secure settings. While it is designed to be safe and only modifies the NFC payment default setting, the developer is not responsible for any system instability or issues that may arise on specific device ROMs.

📄 License

This project is licensed under the MIT License - see the LICENSE file for details.