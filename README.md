# Student ID Card Scanner — সম্পূর্ণ Functional Android App (Kotlin)

অফলাইন-ফার্স্ট Student ID Scanner। ক্যামেরায় QR/Barcode স্ক্যান, QR না থাকলে ছবি তুলে OCR, Room ডাটাবেজ, Search, Scan History, Admin (PIN), CSV Import/Export, Backup/Restore।

## প্রযুক্তি
- Kotlin + Jetpack Compose + Material 3, Navigation Compose
- CameraX (Preview + ImageAnalysis + ImageCapture)
- ML Kit Barcode Scanning (QR, Code 128/39, EAN, UPC) ও ML Kit Text Recognition (OCR, অন-ডিভাইস)
- Room (SQLite) — সম্পূর্ণ অফলাইন ডাটাবেজ
- Coil (ছবি), DataStore (প্রতিষ্ঠানের নাম ও Admin PIN)

## ১. Project download/export
নিচের ZIP ফাইলটি ডাউনলোড করে unzip করুন: `student-id-scanner.zip`
ফোল্ডারের ভেতরে `settings.gradle.kts`, `app/` ফোল্ডার থাকবে।

## ২. Android Studio-তে খোলা
1. Android Studio (Hedgehog বা নতুন, JDK 17) খুলুন।
2. **File → Open** → unzip করা `student-id-scanner` ফোল্ডারটি সিলেক্ট করুন।
3. প্রথমবার Gradle sync হবে (ইন্টারনেট লাগবে) — Sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন।
4. কোনো SDK missing দেখালে **SDK Manager** থেকে Android 14 (API 34) ইনস্টল করুন।

## ৩. APK build করা
- মেনু থেকে **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- শেষ হলে নোটিফিকেশনের **locate**-এ ক্লিক করুন, অথবা ফাইলটি পাবেন:
  `app/build/outputs/apk/debug/app-debug.apk`
- Release APK চাইলে **Build → Generate Signed Bundle / APK** ব্যবহার করুন।

## ৪. মোবাইলে install করা
1. ফোনে **Settings → Security → Unknown sources / Install unknown apps** থেকে Chrome/Files-কে অনুমতি দিন (প্রথমবার)।
2. APK ফাইলটি ফোনে পাঠিয়ে (WhatsApp/Drive/USB) ট্যাপ করে Install করুন।
3. খুলে ক্যামেরার অনুমতি দিন।

## ৫. Student Database update করা
- **Admin (ডিফল্ট PIN: 1234) → CSV থেকে Import**: `sample-students.csv` ফরম্যাটে ফাইল বানান। Excel থাকলে **File → Save As → CSV UTF-8** করে import করুন।
- কলাম: `Student ID | Name | Father | Mother | Class | Roll | Session | Section | Phone`
- এককভাবে: **Admin → Add Student**, আর Edit/Delete: **Students → Profile → Edit/Delete**।
- Export: **Admin → CSV-এ Export**। Backup: **Admin → Database Backup**।

## প্রতিষ্ঠানের নাম ও Logo পরিবর্তন
- নাম: অ্যাপের **Admin → প্রতিষ্ঠানের নাম ও PIN পরিবর্তন** থেকে (অথবা Settings স্ক্রিন)।
- Logo: `app/src/main/res/drawable/ic_launcher_foreground.xml`-এর বদলে নিজের লোগোর vector/PNG বসান, আর Home Screen-এর 🎓-এর জায়গায় `Image()` দিয়ে লোগো দেখান (কোডে মন্তব্য দেওয়া আছে)।

## ডেমো ডাটা
প্রথমবার অ্যাপ খুললে ৫ জন ডেমো শিক্ষার্থী (STD-1001 … STD-1005) থাকে, যাতে সঙ্গে সঙ্গে স্ক্যান পরীক্ষা করা যায়। Admin থেকে মুছে ফেলুন বা CSV import করে নিজের ডাটা বসান।

## সীমাবদ্ধতা (সৎভাবে)
- OCR শুধু Latin (ইংরেজি/সংখ্যা) লেখা পড়ে; বাংলা হাতের/ছাপা লেখা নির্ভরযোগ্যভাবে পড়ে না — তাই ID কার্ডে QR/Barcode বা ইংরেজি ID নম্বর থাকলে সবচেয়ে নির্ভরযোগ্য ফল পাবেন।
- ছবি (photoUri) ফোনের গ্যালারি থেকে বাছা URI হিসেবে সংরক্ষিত হয়; ছবি মুছে ফেললে প্রোফাইলে ছবি দেখাবে না।
- এই প্রজেক্ট এখানে (সার্ভার ছাড়া) কম্পাইল করে যাচাই করা হয়নি — Android Studio-তে প্রথম sync-এ Gradle/AGP সংস্করণ মিলে গেলে build হবে।
