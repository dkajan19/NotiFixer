# <img src="images/bell-ring.png" alt="BlackHole GIF" width="25" /> NotiFixer

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material3](https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Lottie](https://img.shields.io/badge/Lottie-00D1B2?style=for-the-badge)
![Shizuku](https://img.shields.io/badge/Shizuku-Enabled-blue?style=for-the-badge)
![API](https://img.shields.io/badge/API-31%2B-orange?style=for-the-badge)

NotiFixer is an Android utility designed to restore persistent notifications. The project was created as a solution for recent Android versions (specifically tested on **Samsung Galaxy S25 / Android 16**) where the system natively prevents apps from keeping their notifications pinned. This tool bypasses these limitations by modifying system flags.

<a href='https://ko-fi.com/J3J11RQE5L' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi2.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

## Table of Contents
- [Features and Usage](#features-and-usage)
- [Instructions](#instructions)
- [Technical Specifications](#technical-specifications)
- [Screenshots](#screenshots)
- [Installation & Support](#installation--support)
- [License](#license)

## Features and Usage
* **Restore Persistence Notifications:** Toggle any app's notification status to make it undismissable.
* **Interface:** Built using Jetpack Compose and Material 3.
* **Search & Sort:** Quickly find apps by name or package ID and sort them by install date, update time, or name.

## Instructions
1. Ensure the **[Shizuku](https://shizuku.rikka.app/)** service is running on your device.
2. Open **NotiFixer** and grant it Shizuku permissions.
3. Select an app from the list to enable or disable persistent notifications.

## Technical Specifications

| Category | Details |
| :--- | :--- |
| **Requirements** | Android 12.0+ (Optimized for **Android 16**) |
| **Service** | Active **Shizuku** instance (Wireless Debugging, ADB, or Root) |
| **Stack** | Jetpack Compose, Lottie, AndroidX SplashScreen |

### Under the Hood
The application uses Shizuku to execute the following `appops` commands:
* **Enable:** `appops set <pkg> SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS allow`
* **Disable:** `appops set <pkg> SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS default`

## Screenshots
<div style="display: flex; gap: 10px; flex-wrap: wrap;">
  <img src="images/screenshot_1.png" alt="Screenshot 1" width="250"/>
  <img src="images/screenshot_2.png" alt="Screenshot 2" width="250"/>
  <img src="images/screenshot_3.png" alt="Screenshot 3" width="250"/>
</div>

## Installation & Support
Latest builds are available in the **[Releases](../../releases)** section.

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/dkajan19/notifixer/releases)

* **Issues:** If you encounter bugs or have feature requests, please **[open an issue](../../issues)**.
* **Disclaimer:** This project is not affiliated with the Shizuku project. The developer is not responsible for any system instability, damages, or other consequences resulting from the use of this application. Use at your own risk.

## License
This project is licensed under the **MIT License**. You are free to use, modify, and distribute the code, provided that the original copyright notice is preserved. See the **[LICENSE](./LICENSE)** file for full details.

![visitors](https://visitor-badge.laobi.icu/badge?page_id=dkajan19.notifixer)