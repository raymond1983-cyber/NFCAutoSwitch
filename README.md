NFC Auto Switch

An Android utility that automatically switches your device's default NFC payment service based on the app you are currently using. This tool solves common conflicts between Google Wallet, local transit cards, banking apps, and access control cards.

On many Android devices, the system's "Default Payment App" setting does not always switch automatically when a different app is opened, leading to failed transactions or access attempts. NFC Auto Switch uses AccessibilityService to detect the foreground application and instantly updates the NFC routing configuration to match your needs.

🚀 Key Features

Automatic Switching: Instantly changes the default NFC service when a configured app comes to the foreground.

Screen-Off "Reset": Automatically switches to a specific card (e.g., a Transit Card or Door Access Card) when the screen turns off, ensuring "tap-and-go" functionality without unlocking.

Non-Root Solution: Works on unrooted devices by utilizing the WRITE_SECURE_SETTINGS permission via ADB.

Smart Scanning: Automatically detects installed apps with NFC capabilities (supports both HCE and Off-Host services).

Modern UI: Clean, Material Design 3 interface with dark mode support.

Force Refresh Mode: An advanced option for devices that require a routing table reset to apply changes.

🛠 Setup Instructions

Since this app modifies system secure settings to change the NFC default, you must grant it specific permissions. This is a one-time setup.

Step 1: Enable Developer Options

Go to Settings > About Phone.

Tap Build Number 7 times until you see a message saying "You are now a developer!".

Go to System > Developer Options.

Enable USB Debugging.

Device-Specific Settings (Important):

Xiaomi / Redmi / POCO: You must enable USB Debugging (Security Settings). This often requires a SIM card to be inserted and an Mi Account login.

OPPO / Realme: Look for and enable Disable Permission Monitoring.

Step 2: Grant Permission via ADB

You need a computer to grant the necessary permission.

Connect your phone to your PC or Mac via USB.

Open your terminal (Mac/Linux) or command prompt (Windows).

Run the following command:

adb shell pm grant com.marineking.nfcautoswitch android.permission.WRITE_SECURE_SETTINGS


If you don't have ADB installed, you can use a web-based tool like WebADB or download the Android Platform Tools.

Step 3: Enable Accessibility Service

Open the NFC Auto Switch app.

Tap the Accessibility Service card on the dashboard (or go to Android Settings > Accessibility).

Find NFC Auto Switch Service in the list (it might be under "Downloaded Apps").

Turn the switch ON and allow the permission.

(Note: It is recommended to set the battery optimization for this app to "Unrestricted" to prevent the system from killing the service in the background.)

📱 How to Use

Check Status: Ensure the dashboard shows "Authorized" for ADB and "Running" for Accessibility.

Configure Rules:

Go to the Applications tab.

The app will list all detected NFC-capable apps on your device.

Simply toggle the switch next to any app (e.g., Google Wallet) to enable auto-switching for it.

Screen Off Behavior:

On the Dashboard tab, tap "Screen Off Behavior".

Select the card you use most frequently when your phone is locked (e.g., your door access card or transit card).

⚠️ Disclaimer

This tool modifies system secure settings (nfc_payment_default_component). While it is designed to be safe, the developer is not responsible for any system instability or issues that may arise on specific device ROMs.

📄 License

This project is licensed under the MIT License.