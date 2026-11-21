# OneNotify (Android prototype)

แสดงการแจ้งเตือนที่ยังค้างอยู่จาก LINE, Messenger, WhatsApp, WhatsApp Business, Discord, Lemon8, Threads และ X ในรายการเดียว แตะการ์ดเพื่อส่ง `contentIntent` ของการแจ้งเตือนกลับไปเปิดแชทในแอปต้นทาง หน้าหลักปรับตามต้นแบบ Stitch ของผู้ใช้: ส่วนหัว OneNotify, ภาพรวม, ค้นหา, ตัวกรองแอป, การ์ดข้อความ และเมนูล่าง

## ฟีเจอร์

- รวมการแจ้งเตือนที่ค้างอยู่จาก 8 แอป เรียงจากใหม่ไปเก่า
- ตัวกรองแยกตามแอป: ทั้งหมด / LINE / Messenger / WhatsApp / WA Business / Discord / Lemon8 / Threads / X
- ค้นหาจากชื่อผู้ส่งหรือข้อความ
- รวมการ์ดซ้ำ: ข้ามการแจ้งเตือนสรุปกลุ่ม (`FLAG_GROUP_SUMMARY`) และแสดงเพียงใบล่าสุดเมื่อแอป ผู้ส่ง และข้อความตรงกัน
- แตะการ์ดเพื่อเปิดแชทในแอปต้นทาง รองรับ Android 14 ขึ้นไป ซึ่งต้องขออนุญาตเปิดแอปอื่นผ่าน `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED`

## Build

ต้องใช้ AGP 8.13.2, JDK 17 และ Android SDK 35 เปิดด้วย Android Studio หรือ build จาก command line:

```bash
./gradlew.bat :app:assembleDebug
```

APK จะอยู่ที่ `app/build/outputs/apk/debug/app-debug.apk`

ถ้า Gradle บนเครื่องนี้ขึ้น error `Unable to establish loopback connection` ให้ตั้งค่าโฟลเดอร์ชั่วคราวก่อน build:

```bash
JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=D:\AllIn1\build\udtmp' ./gradlew.bat :app:assembleDebug
```

## ทดสอบบนมือถือจำลอง

ใช้ AVD **`OneNotify_Play_Pixel_7`** (Android 15 / API 35, มี Play Store) ตัวนี้ติดตั้ง LINE, Messenger, WhatsApp และ WhatsApp Business ไว้แล้ว ส่วน `OneNotify_Pixel_7` เป็นตัวเก่าที่ไม่มีแอปแชท

```bash
emulator -avd OneNotify_Play_Pixel_7
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb shell cmd notification allow_listener com.allin1.oneinbox/com.allin1.oneinbox.MessageListener
```

```bash
adb shell am start -n com.allin1.oneinbox/.MainActivity
```

ปิด emulator ด้วยปุ่มปิดหน้าต่างเท่านั้น หากบังคับปิด snapshot อาจบันทึกไม่สำเร็จ และเครื่องจะย้อนกลับไปเป็นสภาพเดิม

เวลาทดสอบให้ปิดแอปแชทบน emulator ไว้ก่อน แล้วส่งข้อความจากบัญชีอื่น ถ้าแอปแชทเปิดอยู่ แอปจะไม่สร้างการแจ้งเตือน

### ผลทดสอบ (18 พ.ย. 2026)

| แอป | แสดงในรายการ | แตะเปิดแชท |
|---|---|---|
| LINE | ✅ | ✅ |
| Messenger | ✅ | ✅ |
| WhatsApp | ✅ (การแจ้งเตือนจากระบบ) | ✅ เปิดแอปได้ แต่ยังไม่ได้ทดสอบกับข้อความแชทจริง |
| WhatsApp Business | ยังไม่ได้ทดสอบ (สิทธิ์การแจ้งเตือนยังปิดอยู่) | ยังไม่ได้ทดสอบ |
| Discord, Lemon8, Threads, X | ยังไม่ได้ทดสอบ (ยังไม่ได้ติดตั้งใน emulator) | ยังไม่ได้ทดสอบ |

## ขอบเขตและความเป็นส่วนตัว

- แสดงเฉพาะการแจ้งเตือนที่ยังค้างอยู่ ไม่ดึงประวัติแชท ไม่เก็บข้อความลงฐานข้อมูล และไม่ส่งข้อมูลขึ้นเซิร์ฟเวอร์
- เนื้อหาที่แอปต้นทางซ่อนไว้ในการแจ้งเตือนจะไม่ปรากฏในแอปนี้
- การแตะเปิดใช้ intent ที่แอปต้นทางแนบมา ผลลัพธ์จึงขึ้นอยู่กับพฤติกรรมของแอปนั้น
- หน้าแชทด่วนเป็นทางลัดกลับไปยังกล่องข้อความ OneNotify ไม่มีฟังก์ชันตอบข้อความ รุ่นนี้ยังไม่มี AI วิเคราะห์ความเร่งด่วน และไม่มีปุ่มอ่านทั้งหมด เพราะ Notification Listener ไม่สามารถสั่งให้สถานะเหล่านั้นตรงกับแชทต้นทางได้
- เมื่อเปิดสิทธิ์ Notification access แล้ว Android จะให้สิทธิ์อ่านการแจ้งเตือนจากทุกแอป แม้ OneNotify จะแสดงเพียงแปดแอปข้างต้น ผู้ใช้ควรถอนสิทธิ์เมื่อเลิกใช้งาน

## ภาพสำหรับ Play Store

อยู่ในโฟลเดอร์ `store/`: ไอคอน 512×512, feature graphic 1024×500 และภาพหน้าจอ 1080×1920 จำนวน 4 ภาพใน `store/screenshots/` ภาพหน้าจอดิบอยู่ใน `store/screenshots/raw/`

ภาพหน้าจอถ่ายจากโหมดเดโม ซึ่งใช้ข้อความตัวอย่างที่แต่งขึ้น ไม่ใช่ข้อมูลจริง โหมดนี้ใช้ได้เฉพาะ debug build:

```bash
adb shell am start -n com.allin1.oneinbox/.MainActivity --ez demo true
```
