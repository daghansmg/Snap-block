# SnapBlock — Devir Dosyası

> Son güncelleme: 2026-11-26 · Çalışma dalı: `claude/evren-llm-session-39pfiz`

## 1. Proje ne yapıyor?

SnapBlock, Snapchat'in **Sohbet** listesinde sağ tarafta **çarpı (X)** simgesi görünen kişileri
otomatik olarak engelleyen bir Android uygulamasıdır. Bunu Android **Erişilebilirlik Servisi**
(AccessibilityService) ile yapar: ekran görüntüsü alır, çarpıyı piksel analiziyle bulur, ona dokunur,
"Engelle" ve onay adımlarını tıklar, sonra listeyi kaydırıp devam eder.

## 2. Dosya haritası

| Dosya | Görev |
|---|---|
| `app/src/main/java/com/example/snapblock/MainActivity.kt` | Tek ekranlı arayüz: 1) Erişilebilirlik ayarları, 2) Tara (teşhis), 3) Başlat, Durdur ve bir log alanı. Arayüz kodla kurulmuş, layout XML'i yok. |
| `app/src/main/java/com/example/snapblock/BlockService.kt` | Bütün iş mantığı: ekran görüntüsü, simge tespiti, dokunma ve kaydırma hareketleri, engelleme döngüsü. |
| `app/src/main/res/xml/accessibility_config.xml` | Servis izinleri: pencere içeriğini okuma, hareket, ekran görüntüsü. Yalnızca `com.snapchat.android` paketini dinler. |
| `app/src/main/AndroidManifest.xml` | Activity ve servis tanımları. |
| `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts` | AGP 8.5.2, Kotlin 1.9.24, compileSdk/targetSdk 34, minSdk 26. |
| `.github/workflows/build.yml` | Her push'ta `gradle assembleDebug` çalıştırır ve APK'yı `SnapBlock-apk` artifact'ı olarak yükler. |

## 3. Çalışma akışı (`BlockService.loop`)

1. Ön plandaki uygulama Snapchat değilse 2 sn bekler.
2. `takeScreenshot` ile ekran görüntüsü alır (Android 11+ gerekir; çekimler arasında en az ~1,15 sn vardır).
3. `icons()` ekranın sağındaki şeridi tarar: genişliğin %86–96'sı, yüksekliğin %12–76'sı. Arka plandan
   farklı "mürekkep" bloklarını bulur ve ortası arka plandan belirgin farklı olanları **çarpı** sayar.
4. Çarpı yoksa listeyi yukarı kaydırır. Ekran imzası (`sig`) üst üste 3 kez değişmezse liste bitmiş sayılır.
5. Çarpı varsa ona dokunur, menünün açıldığını başlık parlaklığının düşmesinden anlar.
6. "Engelle" düğümünü metinle arar, bulamazsa sabit koordinata (%35, %69.7) dokunur.
7. "emin misin" metnini ya da beyaz bir dialog pikselini arayarak onay ekranını doğrular, sonra ikinci "Engelle"ye dokunur.
8. Ekran değiştiyse başarı sayar. Ardından **15–40 sn rastgele bekler** (insan davranışı taklidi).
9. Üst üste 3 hatada ya da 4 başarısız ekran görüntüsünde durur.

## 4. Derleme ve kurulum

- **CI:** GitHub Actions → `build` iş akışı → `SnapBlock-apk` artifact'ındaki `app-debug.apk`.
- **Yerel:** JDK 17 ve Gradle 8.7 ile `gradle assembleDebug` (repoda Gradle wrapper yok).
- **Telefonda:** APK'yı kur → 1. düğme → SnapBlock servisini aç → Snapchat'te Sohbet ekranına geç →
  önce 2. düğmeyle teşhis yap → sonra 3. düğmeyle başlat.

## 5. Durum

| Konu | Durum |
|---|---|
| Temel engelleme döngüsü | Kodlandı; gerçek cihazda ne kadar güvenilir çalıştığı bu oturumda doğrulanmadı. |
| CI derlemesi | İş akışı tanımlı; bu oturumda sonucu kontrol edilmedi. |
| **Evren LLM entegrasyonu** | **Başlamadı.** Kullanıcı "Evren" adlı bir yapay zekânın API'sine bağlanılmasını istedi. Webde herkese açık bir "Evren LLM" dokümanı bulunamadı. **API host adı, endpoint ve kimlik doğrulama biçimi bilinmiyor.** |
| Test | Birim ya da cihaz testi yok. |

### Evren entegrasyonu için gerekenler
1. API'nin dokümantasyon linki ya da en azından **host adı** (ör. `api.<alanadı>`).
2. İstek ve yanıt biçimi (OpenAI uyumlu mu: `/v1/chat/completions`?).
3. Anahtarın nasıl gönderildiği (`Authorization: Bearer` ya da özel bir başlık).
4. Anahtar **sohbete ya da koda yazılmamalı**. Ortamın "network secrets" ayarıyla ya da güvenli formla
   eklenmeli. Daha önce sohbete yapıştırılan anahtar **iptal edilip yenilenmeli**.

Önerilen kullanım: Kişi engellenmeden önce sohbet satırındaki metin ya da kullanıcı adı Evren'e
gönderilir ve "engellensin mi?" kararı alınır. Böylece kör "çarpı = engelle" kuralı akıllı bir filtreye dönüşür.

## 6. Riskler ve çözümler

| # | Risk | Neden önemli | Çözüm |
|---|---|---|---|
| 1 | **Snapchat kullanım şartları** otomasyonu yasaklıyor | Hesap kilitlenebilir ya da kapanabilir. | Rastgele bekleme zaten var. Ek olarak günlük üst limit koy (ör. 50/gün) ve oturumları kısa tut. Kullanıcıyı riske karşı uyar. |
| 2 | **Google Play erişilebilirlik politikası** | Erişilebilirlik API'sini engelli kullanıcılara yardım dışında kullanan uygulamalar reddedilir. | Play yerine APK ya da alternatif mağazalarla dağıt. Play hedefleniyorsa açıklama ve onay akışını politikaya göre baştan yaz. |
| 3 | **Sabit koordinatlar ve piksel eşikleri** (%35/%69.7, %50/%51.8, lum>235, eşik 90/60) | Başka ekran oranında, karanlık modda ya da Snapchat güncellemesinden sonra yanlış yere dokunur. | Önce düğüm (metin, `viewId`) ile aramayı esas al. Koordinatları cihaz bazlı kalibrasyonla ayarla. Karanlık mod için arka planı dinamik ölç (kısmen var). |
| 4 | **Yalnızca Türkçe metinler** ("Engelle", "emin misin") | Telefon başka dildeyse düğüm bulunamaz ve koordinata düşer. | Dil bazlı metin listesi kullan (`Block`, `Are you sure` …) ya da `viewIdResourceName` ile eşleştir. |
| 5 | **Yanlış pozitif:** her çarpı "engellenecek kişi" değil | Yanlış kişi engellenir; geri almak elle yapılır. | Engellemeden önce kullanıcı adını logla, "kuru çalıştırma" modu ekle, beyaz liste tut. Evren ile içerik kontrolü yap. |
| 6 | **Başarı tespiti zayıf** (`sig` eşitliği) | Ekran başka bir nedenle değişirse yanlışlıkla başarı sayılır. | Engelleme sonrasında aynı satırın listeden kalktığını düğüm ya da metinle doğrula. |
| 7 | **İş parçacığı ve yaşam döngüsü** | `logger` Activity'yi tutar (bellek sızıntısı); `running` bayrağı servis kapanınca sıfırlanıyor ama döngü uykudaysa geç durur. | `logger`'ı `onDestroy`'da null yap. Döngüyü `Handler`/coroutine ile iptal edilebilir yap. |
| 8 | **Gradle wrapper yok** | Yerelde farklı Gradle sürümüyle derleme kırılabilir. | `gradle wrapper --gradle-version 8.7` ile wrapper ekle. |
| 9 | **LLM'e veri göndermek (Evren)** | Ekrandaki kişi adları üçüncü tarafa gider; bu KVKK ve gizlilik açısından risklidir. Anahtar APK'dan çıkarılabilir. | Gönderilecek veriyi en aza indir ve maskele. Kullanıcıdan açık onay al. Anahtarı uygulamaya gömme; arada küçük bir backend proxy kullan. |

## 7. Sıradaki adımlar (öncelik sırasıyla)

1. **Evren API bilgilerini al** (dokümantasyon linki ya da host) → test isteğiyle bağlantıyı doğrula.
2. Gerçek cihazda **Tara** çıktısını topla, koordinat ve eşikleri doğrula.
3. Kuru çalıştırma modu, günlük limit ve beyaz liste ekle (Risk 1 ve 5).
4. Çok dilli metin eşleştirmesi ekle (Risk 4).
5. Gradle wrapper ekle ve CI'ın yeşil olduğunu doğrula.
6. Evren entegrasyonunu backend proxy üzerinden ekle (Risk 9).

## 8. Güvenlik notu

Bu repoda **hiçbir API anahtarı ya da gizli bilgi yoktur** ve olmamalıdır. Anahtarlar yalnızca ortam
secret'larında ya da backend'de tutulmalıdır.
