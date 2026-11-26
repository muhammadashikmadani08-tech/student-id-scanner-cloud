# Student ID Scanner — Cloud ভার্সন (Firebase)

এটা অফলাইন অ্যাপের আলাদা **ক্লাউড ভার্সন**। প্যাকেজ: `com.institution.idscanner.cloud`,
অ্যাপের নাম: **Student ID Scanner Cloud** — পুরনো অফলাইন অ্যাপের পাশাপাশি
একই ফোনে ইনস্টল করা যায়, দুটি অ্যাপের ডাটা সম্পূর্ণ আলাদা।

## কীভাবে কাজ করে

- **আসল Google লগইন:** অ্যাপ খুললেই "Google দিয়ে লগইন করুন" বাটন আসে।
  লগইন করা Gmail-এর Firebase **User ID (UID)**-ই হলো অ্যাকাউন্টের পরিচয়।
- **প্রতি UID-তে আলাদা ডাটা:** ছাত্র, হাজিরা, স্ক্যান হিস্ট্রি, প্রতিষ্ঠানের
  নাম ও Admin PIN — সব ডাটা থাকে `users/{UID}/`-এর নিচে। এক Gmail-এর ডাটা
  অন্য Gmail দেখতে পারে না।
- **যেকোনো ফোনে একই ডাটা:** একই Gmail দিয়ে লগইন করলেই ছাত্র, হাজিরা,
  হিস্ট্রি — সব লেখা ডাটা সব ফোনে চলে আসে।
- **ছবি শুধু এই ফোনে:** এই বিল্ডে Firebase Storage চালু নেই, তাই ছবি
  ফোনের ভেতরেই সংরক্ষিত হয় — অন্য ফোনে ছবি দেখা যাবে না, শুধু লেখা
  তথ্য দেখা যাবে।
- **অফলাইন ক্যাশ:** Firestore-এর offline persistence চালু আছে — লগইন করা
  থাকলে শেষবার সিঙ্ক হওয়া ডাটা ইন্টারনেট ছাড়াও দেখা যায়, আর নেট এলে
  পরিবর্তন নিজে থেকে সিঙ্ক হয়। তবে **প্রথম লগইন ও সিঙ্কের জন্য ইন্টারনেট
  আবশ্যক**।

## Firestore ডাটা কাঠামো

```
users/{uid}/students/{studentId}        Student (id, name, fatherName,
                                        motherName, className, roll,
                                        session, section, phone, photoUrl)
users/{uid}/attendance/{studentId_yyyy-MM-dd}
                                        AttendanceRecord (studentId, date,
                                        scannedAt) — এক দিনে একটাই ডকুমেন্ট
users/{uid}/history/{autoId}            ScanHistory (studentId, scannedAt)
users/{uid}/settings/profile            UserProfile (institutionName,
                                        adminPin, seeded, email)
```

ছবি (Storage চালু না থাকায়) ফোনের ভেতরের `photos/` ফোল্ডারে সংরক্ষিত
হয়, `photoUrl`-এ থাকে তার absolute file path — অন্য ফোনে সিঙ্ক হয় না।

## Firebase সেটআপ (APK বানানোর আগে আবশ্যক)

1. https://console.firebase.google.com -এ প্রজেক্ট বানান (ফ্রি)।
2. Android অ্যাপ যোগ করুন, প্যাকেজ নাম **ঠিক এভাবে** দিন:
   `com.institution.idscanner.cloud`
3. `google-services.json` ডাউনলোড করে **`app/google-services.json`** হিসেবে
   রাখুন (এই ফোল্ডারে `app/google-services.json.placeholder` ফাইলটি দেখুন)।
   **এই ফাইল ছাড়া বিল্ড হবে না** — GitHub Actions-এও প্রথম ধাপেই স্পষ্ট
   ভুল দেখাবে। আসল ফাইলটি পাবলিক রিপোজিটরিতে কমিট করবেন না।
4. Firebase console-এ চালু করুন:
   - **Authentication > Sign-in method > Google** — Enable করুন।
   - **Firestore Database** — তৈরি করুন, তারপর এই প্রজেক্টের
     `firestore.rules` ফাইলের নিয়মগুলো Rules-এ বসিয়ে Publish করুন।
   - **Storage** — তৈরি করুন, তারপর `storage.rules` ফাইলের নিয়মগুলো
     বসিয়ে Publish করুন।
5. Google সাইন-ইন-এ সমস্যা হলে: Firebase-এ Android অ্যাপের SHA-1
   ফিঙ্গারপ্রিন্ট (debug keystore-এরটা, রিলিজের জন্য রিলিজ key-এরটা)
   যোগ করা আছে কিনা দেখুন।

## Firestore Security Rules (সংক্ষেপে)

শুধুমাত্র লগইন করা ব্যবহারকারী নিজের `users/{UID}/**` ফোল্ডারে পড়তে ও
লিখতে পারে; অন্য কারো ডাটায় নয়, লগইন ছাড়া কিছুই নয়। সম্পূর্ণ নিয়ম
`firestore.rules` ফাইলে আছে — সেটাই কপি করে বসান।

## Storage Security Rules (সংক্ষেপে)

শুধুমাত্র লগইন করা ব্যবহারকারী নিজের `users/{UID}/photos/` ফোল্ডারের
ছবি দেখতে/আপলোড/মুছতে পারে; আপলোড সর্বোচ্চ 5 MB। সম্পূর্ণ নিয়ম
`storage.rules` ফাইলে আছে।

## বিল্ড (GitHub Actions)

`.github/workflows/android.yml` আগের ঠিক করা ভার্সনের মতোই (Gradle 8.7,
JDK 17, heap 4 GB), তবে প্রথমে `app/google-services.json` আছে কিনা দেখে
নেয়। রিয়েল ফাইলটি রিপোতে বসিয়ে push করলেই বিল্ড শুরু হবে; APK
artifact-এর নাম `student-id-scanner-cloud-apk`।

## অফলাইন ভার্সন থেকে পার্থক্য

- Room/DataStore নেই — ডাটা Firestore-এ, সেটিংস প্রোফাইল ডকুমেন্টে।
- Gmail টাইপ করে অ্যাকাউন্ট বদলানোর অপশন নেই — Google লগইনই অ্যাকাউন্ট।
- Database Backup/Restore বোতাম নেই — ডাটা ক্লাউডেই থাকে।
- স্ক্যান, QR/Barcode, OCR, শ্রেণি অনুযায়ী তালিকা, সাপ্তাহিক হাজিরা
  (শনি–শুক্র, রাত ৮টার নিয়ম, সকাল/বিকেল), CSV import/export — সব একই
  আছে। প্রথম লগইনে অফলাইন ভার্সনের মতোই ৫ জন ডেমো ছাত্র (STD-1001…
  STD-1005) তৈরি হয়।

## সীমাবদ্ধতা (জানা বিষয়)

- এই কোড কম্পাইল করে পরীক্ষা করা হয়নি (লোকাল Gradle daemon ব্লকড);
  GitHub Actions-এ প্রথম বিল্ডেই যাচাই করতে হবে।
- ছাত্র মুছলে তার Storage ছবিও মোছার চেষ্টা করা হয়; ছবি না থাকলে বা
  নেট না থাকলে মুছতে না পারলেও ছাত্র মোছা আটকায় না।
- CSV import-এ ছবি আসে না — ছবি আলাদাভাবে প্রোফাইলে যোগ করতে হয়।
